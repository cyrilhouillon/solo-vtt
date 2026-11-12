package dnd.server;

import dnd.engine.sheet.Ability;
import dnd.engine.sheet.AbilityMethod;
import dnd.engine.sheet.AbilityRoll;
import dnd.engine.sheet.Background;
import dnd.engine.sheet.BackgroundChoice;
import dnd.engine.sheet.BackgroundEquipment;
import dnd.engine.sheet.Build;
import dnd.engine.sheet.CharacterClass;
import dnd.engine.sheet.CharacterSheet;
import dnd.engine.sheet.CreatureSize;
import dnd.engine.sheet.FightingStyle;
import dnd.engine.sheet.GamingSet;
import dnd.engine.sheet.OriginFeat;
import dnd.engine.sheet.OriginFeatChoice;
import dnd.engine.sheet.Proficiency;
import dnd.engine.sheet.SheetAttack;
import dnd.engine.sheet.SheetState;
import dnd.engine.sheet.Skill;
import dnd.engine.sheet.Species;
import dnd.engine.sheet.SpeciesChoice;
import dnd.engine.sheet.SpeciesOption;
import dnd.engine.sheet.SpellList;
import dnd.engine.sheet.SpellSlots;
import dnd.engine.sheet.SrdWeapon;
import dnd.engine.sheet.StartingEquipment;
import dnd.engine.state.DamageType;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Ce que les routes du module D&D reçoivent et rendent. */
public final class DndDtos {

    private DndDtos() {
    }

    /** La page « Personnages joueurs », racine de la section du module. */
    public record SectionsDto(String playersPageId) {}

    /** Le nom d'un nouveau personnage : le titre de sa page. */
    public record NewSheetRequest(String title) {}

    /**
     * Une attaque saisie : bonus d'attaque, dés et bonus de dégâts, type de dégâts, allonge et portée en mètres ({@code null}
     * : sans).
     */
    public record AttackDto(String name, int bonus, int diceCount, int dieSides, int damageBonus, DamageType damageType,
                            Double reach, Double normalRange, Double longRange) {

        static AttackDto of(SheetAttack a) {
            return new AttackDto(a.name(), a.bonus(), a.diceCount(), a.dieSides(), a.damageBonus(), a.damageType(),
                    a.reach(), a.normalRange(), a.longRange());
        }

        SheetAttack toAttack() {
            return new SheetAttack(name, bonus, diceCount, dieSides, damageBonus, damageType, reach, normalRange, longRange);
        }
    }

    /** Les emplacements d'un niveau de sort. */
    public record SpellSlotsDto(int level, int max, int expended) {}

    /**
     * Les valeurs saisies d'une fiche ; {@code skills} ne liste que les compétences maîtrisées, {@code speed} est en
     * mètres. Une fiche d'avant, sans taille, traits d'espèce ni dons, se lit avec ces textes vides.
     */
    public record SheetValuesDto(String className, int level, String species, String size, String background,
                                 Map<Ability, Integer> abilities, int proficiencyBonus,
                                 Set<Ability> savingThrows, Map<Skill, Proficiency> skills,
                                 int armorClass, int hitPointMax, int hitPoints, String hitDice, double speed, int initiative,
                                 List<AttackDto> attacks,
                                 String features, String speciesTraits, String feats, String equipment,
                                 String proficiencies, String languages, List<SpellSlotsDto> spellSlots) {

        static SheetValuesDto of(CharacterSheet sheet) {
            return new SheetValuesDto(sheet.className(), sheet.level(), sheet.species(), sheet.size(), sheet.background(),
                    new EnumMap<>(sheet.abilities()), sheet.proficiencyBonus(), sheet.savingThrows(), sheet.skills(),
                    sheet.armorClass(), sheet.hitPointMax(), sheet.hitPoints(), sheet.hitDice(), sheet.speed(), sheet.initiative(),
                    sheet.attacks().stream().map(AttackDto::of).toList(),
                    sheet.features(), sheet.speciesTraits(), sheet.feats(), sheet.equipment(), sheet.proficiencies(),
                    sheet.languages(),
                    sheet.spellSlots().stream().map(s -> new SpellSlotsDto(s.level(), s.max(), s.expended())).toList());
        }

        /**
         * La fiche de ces valeurs.
         *
         * @throws IllegalArgumentException si une valeur est hors bornes, avec la raison
         */
        CharacterSheet toSheet() {
            return new CharacterSheet(className, level, species, size, background, abilities, proficiencyBonus,
                    savingThrows, skills == null ? Map.of() : withoutNone(skills),
                    armorClass, hitPointMax, hitPoints, hitDice, speed, initiative,
                    attacks == null ? List.of() : attacks.stream().map(AttackDto::toAttack).toList(),
                    features, speciesTraits, feats, equipment, proficiencies, languages,
                    spellSlots == null ? List.of() : spellSlots.stream()
                            .map(s -> new SpellSlots(s.level(), s.max(), s.expended())).toList());
        }

        private static Map<Skill, Proficiency> withoutNone(Map<Skill, Proficiency> skills) {
            Map<Skill, Proficiency> kept = new EnumMap<>(Skill.class);
            skills.forEach((skill, proficiency) -> {
                if (skill != null && proficiency != null && proficiency != Proficiency.NONE) kept.put(skill, proficiency);
            });
            return kept;
        }
    }

    /** Ce que les formules du SRD tirent des valeurs saisies. */
    public record DerivedDto(Map<Ability, Integer> modifiers, Map<Ability, Integer> savingThrows,
                             Map<Skill, Integer> skills, int passivePerception) {

        static DerivedDto of(CharacterSheet sheet) {
            Map<Ability, Integer> modifiers = new EnumMap<>(Ability.class);
            Map<Ability, Integer> saves = new EnumMap<>(Ability.class);
            for (Ability ability : Ability.values()) {
                modifiers.put(ability, sheet.modifier(ability));
                saves.put(ability, sheet.savingThrow(ability));
            }
            Map<Skill, Integer> skills = new EnumMap<>(Skill.class);
            for (Skill skill : Skill.values()) skills.put(skill, sheet.skill(skill));
            return new DerivedDto(modifiers, saves, skills, sheet.passivePerception());
        }
    }

    /** Un don d'origines et ses sous-choix, par leurs ids ; {@code skills} (Doué) dans l'ordre des compétences. */
    public record OriginFeatChoiceDto(OriginFeat feat, SpellList spellList, Ability spellcastingAbility, List<Skill> skills) {

        static OriginFeatChoiceDto of(OriginFeatChoice choice) {
            return choice == null ? null : new OriginFeatChoiceDto(choice.feat(), choice.spellList(),
                    choice.spellcastingAbility(), choice.skills().stream().sorted().toList());
        }

        OriginFeatChoice toChoice() {
            if (skills != null && skills.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Compétence inconnue");
            return new OriginFeatChoice(feat, spellList, spellcastingAbility, skills == null ? Set.of() : Set.copyOf(skills));
        }
    }

    /**
     * L'espèce d'une construction et ses sous-choix, par leurs ids ; seule l'espèce est requise. {@code originFeat} : le don
     * de Polyvalent (Humain), {@code null} tant qu'il n'est pas choisi.
     */
    public record SpeciesChoiceDto(Species species, CreatureSize size, SpeciesOption option, Skill skill,
                                   Ability spellcastingAbility, OriginFeatChoiceDto originFeat) {

        static SpeciesChoiceDto of(SpeciesChoice choice) {
            return choice == null ? null : new SpeciesChoiceDto(choice.species(), choice.size(), choice.option(),
                    choice.skill(), choice.spellcastingAbility(), OriginFeatChoiceDto.of(choice.originFeat()));
        }

        SpeciesChoice toChoice() {
            return new SpeciesChoice(species, size, option, skill, spellcastingAbility,
                    originFeat == null ? null : originFeat.toChoice());
        }
    }

    /**
     * L'historique d'une construction et ses choix, par leurs ids : augmentations par caractéristique, équipement A ou B,
     * boîte de jeux (Soldat) et caractéristique d'incantation d'Initié à la magie.
     */
    public record BackgroundChoiceDto(Background background, Map<Ability, Integer> increases, BackgroundEquipment equipment,
                                      GamingSet gamingSet, Ability spellcastingAbility) {

        static BackgroundChoiceDto of(BackgroundChoice choice) {
            return choice == null ? null : new BackgroundChoiceDto(choice.background(),
                    choice.increases().isEmpty() ? Map.of() : new EnumMap<>(choice.increases()),
                    choice.equipment(), choice.gamingSet(), choice.spellcastingAbility());
        }

        BackgroundChoice toChoice() {
            if (increases != null && increases.keySet().stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Caractéristique inconnue");
            return new BackgroundChoice(background, increases, equipment, gamingSet, spellcastingAbility);
        }
    }

    /**
     * Les choix de la création guidée, par leurs ids ; {@code skills} et {@code masteries} dans l'ordre de leurs listes,
     * {@code masteries} par id d'arme ; {@code species} {@code null} tant qu'aucune espèce n'est choisie (et dans une
     * construction d'avant) ; {@code background} de même pour l'historique.
     */
    public record BuildDto(CharacterClass characterClass, AbilityMethod method, Map<Ability, Integer> scores,
                           List<Skill> skills, FightingStyle fightingStyle, StartingEquipment equipment,
                           List<SrdWeapon> masteries, SpeciesChoiceDto species, BackgroundChoiceDto background) {

        static BuildDto of(Build build) {
            return new BuildDto(build.characterClass(), build.method(),
                    build.scores().isEmpty() ? Map.of() : new EnumMap<>(build.scores()),
                    build.skills().stream().sorted().toList(), build.fightingStyle(), build.equipment(), build.masteries(),
                    SpeciesChoiceDto.of(build.species()), BackgroundChoiceDto.of(build.background()));
        }

        /**
         * La construction de ces choix, à valider ({@link Build#validate}).
         *
         * @throws IllegalArgumentException s'il manque les valeurs, les compétences ou les Bottes d'arme
         */
        Build toBuild() {
            if (scores == null) throw new IllegalArgumentException("Une construction porte ses valeurs de caractéristique");
            if (skills == null) throw new IllegalArgumentException("Une construction porte ses compétences");
            if (masteries == null) throw new IllegalArgumentException("Une construction porte ses Bottes d'arme");
            if (scores.containsKey(null)) throw new IllegalArgumentException("Caractéristique inconnue");
            if (skills.contains(null)) throw new IllegalArgumentException("Compétence inconnue");
            return new Build(characterClass, method, scores, Set.copyOf(skills), fightingStyle, equipment, masteries,
                    species == null ? null : species.toChoice(), background == null ? null : background.toChoice());
        }
    }

    /** Un jet de caractéristique : ses quatre d6 dans l'ordre du tirage, et la somme des trois plus hauts. */
    public record AbilityRollDto(List<Integer> dice, int total) {

        static AbilityRollDto of(AbilityRoll roll) {
            return new AbilityRollDto(roll.dice(), roll.total());
        }
    }

    /**
     * Une fiche : la page qui la montre, ses valeurs saisies et leurs dérivés, sa construction guidée et ses jets de
     * caractéristique ({@code null} s'il n'y en a pas).
     */
    public record SheetDto(String pageId, SheetValuesDto values, DerivedDto derived, BuildDto build,
                           List<AbilityRollDto> rolls) {

        static SheetDto of(String pageId, SheetState state) {
            return new SheetDto(pageId, SheetValuesDto.of(state.values()), DerivedDto.of(state.values()),
                    state.build() == null ? null : BuildDto.of(state.build()),
                    state.rolls() == null ? null : state.rolls().stream().map(AbilityRollDto::of).toList());
        }
    }

    /**
     * Une espèce pour la création guidée : ses tailles (prise d'office s'il n'y en a qu'une), sa vitesse en mètres, le
     * trait dont on choisit une option et ses options, les compétences au choix et le trait qui les donne, les
     * caractéristiques d'incantation au choix (vide si elle n'en choisit pas), et ses traits qui ne dépendent d'aucun choix.
     */
    public record SpeciesOptionsDto(String id, String label, List<ChoiceDto> sizes, double speed, String optionLabel,
                                    List<DescribedChoiceDto> options, String skillTrait, List<ChoiceDto> skillChoices,
                                    List<ChoiceDto> spellcastingAbilities, List<String> traits) {

        static SpeciesOptionsDto of(Species species) {
            return new SpeciesOptionsDto(species.name(), species.label(),
                    species.sizes().stream().map(size -> new ChoiceDto(size.name(), size.label())).toList(),
                    species.speed(), species.optionLabel(),
                    species.options().stream()
                            .map(option -> new DescribedChoiceDto(option.name(), option.label(), option.text())).toList(),
                    species.skillTrait(),
                    Arrays.stream(Skill.values()).filter(species.skillChoices()::contains)
                            .map(skill -> new ChoiceDto(skill.name(), skill.label())).toList(),
                    species.spellcasting()
                            ? SpeciesChoice.SPELLCASTING_ABILITIES.stream()
                                    .map(ability -> new ChoiceDto(ability.name(), ability.label())).toList()
                            : List.of(),
                    species.traits());
        }
    }

    /**
     * Un historique pour la création guidée : ses trois caractéristiques, son don (et la liste de sorts qu'il fixe), ses
     * compétences, son outil ({@code null} : une boîte de jeux au choix, parmi {@code gamingSets}) et ses équipements A et B.
     */
    public record BackgroundOptionsDto(String id, String label, List<ChoiceDto> abilities, DescribedChoiceDto feat,
                                       ChoiceDto spellList, List<ChoiceDto> skills, String tool, List<ChoiceDto> gamingSets,
                                       List<DescribedChoiceDto> equipments) {

        static BackgroundOptionsDto of(Background background) {
            OriginFeat feat = background.feat();
            return new BackgroundOptionsDto(background.name(), background.label(),
                    Arrays.stream(Ability.values()).filter(background.abilities()::contains)
                            .map(ability -> new ChoiceDto(ability.name(), ability.label())).toList(),
                    new DescribedChoiceDto(feat.name(), feat.label(), feat.text()),
                    background.spellList() == null ? null
                            : new ChoiceDto(background.spellList().name(), background.spellList().label()),
                    Arrays.stream(Skill.values()).filter(background.skills()::contains)
                            .map(skill -> new ChoiceDto(skill.name(), skill.label())).toList(),
                    background.tool(),
                    background.choosesGamingSet()
                            ? Arrays.stream(GamingSet.values()).map(set -> new ChoiceDto(set.name(), set.label())).toList()
                            : List.of(),
                    Arrays.stream(BackgroundEquipment.values())
                            .map(e -> new DescribedChoiceDto(e.name(), e.label(), background.equipmentText(e))).toList());
        }
    }

    /**
     * Un don d'origines pour la création guidée : ce qu'il fait, les listes de sorts et caractéristiques d'incantation au
     * choix (vides sauf Initié à la magie) et le nombre de compétences à choisir (3 pour Doué, 0 sinon).
     */
    public record OriginFeatOptionsDto(String id, String label, String text, List<ChoiceDto> spellLists,
                                       List<ChoiceDto> spellcastingAbilities, int skillCount) {

        static OriginFeatOptionsDto of(OriginFeat feat) {
            boolean magic = feat == OriginFeat.MAGIC_INITIATE;
            return new OriginFeatOptionsDto(feat.name(), feat.label(), feat.text(),
                    magic ? Arrays.stream(SpellList.values()).map(l -> new ChoiceDto(l.name(), l.label())).toList() : List.of(),
                    magic ? SpeciesChoice.SPELLCASTING_ABILITIES.stream()
                            .map(ability -> new ChoiceDto(ability.name(), ability.label())).toList() : List.of(),
                    feat == OriginFeat.SKILLED ? OriginFeat.SKILLED_COUNT : 0);
        }
    }

    /** Un choix proposé : son id et son libellé. */
    public record ChoiceDto(String id, String label) {}

    /** Un choix proposé, avec ce qu'il fait ou contient. */
    public record DescribedChoiceDto(String id, String label, String text) {}

    /** Une arme qu'une Botte d'arme peut viser, et sa botte. */
    public record WeaponChoiceDto(String id, String label, String mastery) {}

    /**
     * Les options d'une classe pour la création guidée : compétences au choix et combien, Styles de combat, équipements
     * de départ (leurs objets et pièces d'or), armes pour les Bottes et combien, répartition suggérée, valeurs standard et
     * coûts de l'achat de points (par valeur).
     */
    public record ClassOptionsDto(String id, String label, List<ChoiceDto> skillChoices, int skillCount,
                                  List<DescribedChoiceDto> fightingStyles, List<DescribedChoiceDto> equipments,
                                  List<WeaponChoiceDto> weapons, int masteryCount, Map<Ability, Integer> suggestedScores,
                                  List<Integer> standardScores, Map<String, Integer> pointBuyCosts, int pointBuyBudget,
                                  BuildDto defaultBuild) {

        static ClassOptionsDto of(CharacterClass characterClass) {
            Map<String, Integer> costs = new LinkedHashMap<>();
            for (int score = AbilityMethod.POINT_BUY_MIN; score <= AbilityMethod.POINT_BUY_MAX; score++) {
                costs.put(String.valueOf(score), AbilityMethod.pointBuyCost(score));
            }
            return new ClassOptionsDto(characterClass.name(), characterClass.label(),
                    Arrays.stream(Skill.values()).filter(characterClass.skillChoices()::contains)
                            .map(skill -> new ChoiceDto(skill.name(), skill.label())).toList(),
                    characterClass.skillCount(),
                    Arrays.stream(FightingStyle.values())
                            .map(style -> new DescribedChoiceDto(style.name(), style.label(), style.text())).toList(),
                    characterClass.equipments().stream()
                            .map(equipment -> new DescribedChoiceDto(equipment.name(), equipment.label(), equipment.text())).toList(),
                    Arrays.stream(SrdWeapon.values())
                            .map(weapon -> new WeaponChoiceDto(weapon.name(), weapon.label(), weapon.mastery())).toList(),
                    characterClass.masteryCount(), new EnumMap<>(characterClass.suggestedScores()),
                    AbilityMethod.STANDARD_SCORES, costs, AbilityMethod.POINT_BUY_BUDGET,
                    BuildDto.of(Build.suggested(characterClass)));
        }
    }
}
