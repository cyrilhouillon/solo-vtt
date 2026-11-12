package dnd.server;

import dnd.engine.sheet.Ability;
import dnd.engine.sheet.AbilityMethod;
import dnd.engine.sheet.AbilityRoll;
import dnd.engine.sheet.Build;
import dnd.engine.sheet.CharacterClass;
import dnd.engine.sheet.CharacterSheet;
import dnd.engine.sheet.FightingStyle;
import dnd.engine.sheet.Proficiency;
import dnd.engine.sheet.SheetAttack;
import dnd.engine.sheet.SheetState;
import dnd.engine.sheet.Skill;
import dnd.engine.sheet.SpellSlots;
import dnd.engine.sheet.SrdWeapon;
import dnd.engine.sheet.StartingEquipment;
import dnd.engine.state.DamageType;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
     * mètres.
     */
    public record SheetValuesDto(String className, int level, String species, String background,
                                 Map<Ability, Integer> abilities, int proficiencyBonus,
                                 Set<Ability> savingThrows, Map<Skill, Proficiency> skills,
                                 int armorClass, int hitPointMax, int hitPoints, String hitDice, double speed, int initiative,
                                 List<AttackDto> attacks,
                                 String features, String equipment, String proficiencies, String languages,
                                 List<SpellSlotsDto> spellSlots) {

        static SheetValuesDto of(CharacterSheet sheet) {
            return new SheetValuesDto(sheet.className(), sheet.level(), sheet.species(), sheet.background(),
                    new EnumMap<>(sheet.abilities()), sheet.proficiencyBonus(), sheet.savingThrows(), sheet.skills(),
                    sheet.armorClass(), sheet.hitPointMax(), sheet.hitPoints(), sheet.hitDice(), sheet.speed(), sheet.initiative(),
                    sheet.attacks().stream().map(AttackDto::of).toList(),
                    sheet.features(), sheet.equipment(), sheet.proficiencies(), sheet.languages(),
                    sheet.spellSlots().stream().map(s -> new SpellSlotsDto(s.level(), s.max(), s.expended())).toList());
        }

        /**
         * La fiche de ces valeurs.
         *
         * @throws IllegalArgumentException si une valeur est hors bornes, avec la raison
         */
        CharacterSheet toSheet() {
            return new CharacterSheet(className, level, species, background, abilities, proficiencyBonus,
                    savingThrows, skills == null ? Map.of() : withoutNone(skills),
                    armorClass, hitPointMax, hitPoints, hitDice, speed, initiative,
                    attacks == null ? List.of() : attacks.stream().map(AttackDto::toAttack).toList(),
                    features, equipment, proficiencies, languages,
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

    /**
     * Les choix de la création guidée, par leurs ids ; {@code skills} et {@code masteries} dans l'ordre de leurs listes,
     * {@code masteries} par id d'arme.
     */
    public record BuildDto(CharacterClass characterClass, AbilityMethod method, Map<Ability, Integer> scores,
                           List<Skill> skills, FightingStyle fightingStyle, StartingEquipment equipment,
                           List<SrdWeapon> masteries) {

        static BuildDto of(Build build) {
            return new BuildDto(build.characterClass(), build.method(),
                    build.scores().isEmpty() ? Map.of() : new EnumMap<>(build.scores()),
                    build.skills().stream().sorted().toList(), build.fightingStyle(), build.equipment(), build.masteries());
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
            return new Build(characterClass, method, scores, Set.copyOf(skills), fightingStyle, equipment, masteries);
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
