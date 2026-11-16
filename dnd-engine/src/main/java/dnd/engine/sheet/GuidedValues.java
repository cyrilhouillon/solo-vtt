package dnd.engine.sheet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ce qu'une construction guidée écrit sur la fiche, champ par champ (SRD 5.2.1, niveau 1). Les langues n'en dérivent
 * pas : elles restent au joueur. Caractéristique d'incantation, sorts et emplacements suivent la classe : vides pour une
 * classe qui ne lance pas de sorts ; les emplacements dérivés n'ont rien de dépensé, et tant qu'ils ne changent pas, ceux
 * de la fiche restent, dépensés compris. CA et attaques non plus : la fiche les calcule ({@link CombatStats}) de
 * la classe, du Style et de l'inventaire que la construction écrit. Espèce, taille, vitesse et traits d'espèce valent
 * {@code null} tant que la construction n'a pas d'espèce, historique tant qu'elle n'a pas d'historique, dons tant qu'elle
 * n'a ni historique ni Polyvalent : la fiche garde alors ce que le joueur y a saisi.
 *
 * <p>{@link #rebuild} ne réécrit que les champs dont la valeur dérivée a changé depuis la construction précédente : une
 * retouche du joueur survit tant que le choix dont elle dépend ne change pas. L'inventaire est celui de départ : un choix
 * qui le change le remplace en entier.</p>
 */
public record GuidedValues(CharacterClass characterClass, FightingStyle fightingStyle, int level, int proficiencyBonus,
                           Map<Ability, Integer> abilities, Set<Ability> savingThrows, Map<Skill, Proficiency> skills,
                           int hitPointMax, int hitPoints, String hitDice, int initiative,
                           String features, Inventory inventory, String proficiencies,
                           String species, String size, Double speed, String speciesTraits,
                           String background, String feats,
                           Ability spellcastingAbility, List<Spell> cantrips, List<Spell> spellbook,
                           List<Spell> preparedSpells, List<SpellSlots> spellSlots) {

    /** Aucune construction précédente : {@link #rebuild} réécrit tous les champs dérivés. */
    public static final GuidedValues NONE = new GuidedValues(null, null, 0, 0, Map.of(), Set.of(), Map.of(),
            0, 0, "", 0, "", Inventory.EMPTY, "", null, null, null, null, null, null,
            null, List.of(), List.of(), List.of(), List.of());

    private static final int LEVEL = 1;
    private static final int PROFICIENCY_BONUS = 2;

    public GuidedValues {
        abilities = Map.copyOf(abilities);
        savingThrows = Set.copyOf(savingThrows);
        skills = Map.copyOf(skills);
        cantrips = List.copyOf(cantrips);
        spellbook = List.copyOf(spellbook);
        preparedSpells = List.copyOf(preparedSpells);
        spellSlots = List.copyOf(spellSlots);
    }

    /** Les valeurs qu'une construction donne à une fiche qui porte ses valeurs de caractéristique, augmentations comprises. */
    public static GuidedValues of(Build build) {
        return of(build, build.abilities());
    }

    /**
     * Les valeurs qu'une construction donne à la fiche ; la construction est supposée valide. Initiative et PV suivent les
     * caractéristiques de la fiche ({@code abilities}), retouches comprises (hausses de l'historique…).
     *
     * @param abilities les valeurs de caractéristique de la fiche, une fois la construction appliquée
     */
    public static GuidedValues of(Build build, Map<Ability, Integer> abilities) {
        CharacterClass characterClass = build.characterClass();
        int dex = modifier(abilities.get(Ability.DEXTERITY));
        int con = modifier(abilities.get(Ability.CONSTITUTION));

        SpeciesChoice species = build.species();
        BackgroundChoice background = build.background();
        Set<Skill> granted = build.proficientSkills();
        Map<Skill, Proficiency> skills = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) {
            Proficiency proficiency = build.expertise().contains(skill) ? Proficiency.EXPERTISE
                    : granted.contains(skill) ? Proficiency.PROFICIENT : Proficiency.NONE;
            skills.put(skill, proficiency);
        }
        int hitPoints = characterClass.hitDie() + con + (species == null ? 0 : species.species().hitPointsPerLevel() * LEVEL);

        List<OriginFeatChoice> feats = new ArrayList<>();
        if (background != null) feats.add(background.feat());
        if (species != null && species.originFeat() != null) feats.add(species.originFeat());
        boolean alert = feats.stream().anyMatch(feat -> feat.feat() == OriginFeat.ALERT);

        // Chaque outil une fois : celui de la classe, puis celui de l'historique.
        List<String> tools = new ArrayList<>();
        if (characterClass.tool() != null) tools.add(characterClass.tool().label());
        if (!characterClass.toolChoices().isEmpty()) tools.add(build.tool() == null ? StartingEquipment.TOOL_TO_CHOOSE : build.tool().label());
        if (background != null && !tools.contains(background.toolText())) tools.add(background.toolText());

        CharacterClass.Spellcasting casting = characterClass.spellcasting();
        List<SpellSlots> slots = new ArrayList<>();
        if (casting != null) {
            for (int level = 1; level <= casting.slots().size(); level++) slots.add(new SpellSlots(level, casting.slots().get(level - 1), 0));
        }

        return new GuidedValues(characterClass, build.fightingStyle(), LEVEL, PROFICIENCY_BONUS, build.abilities(),
                EnumSet.copyOf(characterClass.savingThrows()), skills,
                hitPoints, hitPoints,
                "1d" + characterClass.hitDie(), dex + (alert ? PROFICIENCY_BONUS : 0),
                features(build), build.startingInventory(),
                tools.isEmpty() ? characterClass.proficiencies()
                        : characterClass.proficiencies() + "\nOutils : " + String.join(", ", tools),
                species == null ? null : species.species().label(),
                species == null ? null : species.sizeText(),
                species == null ? null : species.speed(),
                species == null ? null : species.traitsText(),
                background == null ? null : background.background().label(),
                feats.isEmpty() ? null : feats.stream().map(OriginFeatChoice::text).collect(Collectors.joining("\n")),
                casting == null ? null : casting.ability(), build.cantrips(), build.spellbook(), build.prepared(), slots);
    }

    /**
     * La fiche, où chaque champ dont la valeur dérivée diffère de {@code before} prend celle de cette construction ; les
     * autres gardent ce que la fiche porte.
     *
     * @param before les valeurs de la construction précédente, {@link #NONE} ou {@code null} pour tout écrire
     */
    public CharacterSheet rebuild(CharacterSheet sheet, GuidedValues before) {
        GuidedValues previous = before == null || before == NONE ? null : before;

        Map<Ability, Integer> abilities = new EnumMap<>(sheet.abilities());
        Set<Ability> saves = sheet.savingThrows().isEmpty() ? EnumSet.noneOf(Ability.class) : EnumSet.copyOf(sheet.savingThrows());
        for (Ability ability : Ability.values()) {
            if (changed(previous, v -> v.abilities.get(ability))) abilities.put(ability, this.abilities.get(ability));
            if (changed(previous, v -> v.savingThrows.contains(ability))) {
                if (savingThrows.contains(ability)) saves.add(ability);
                else saves.remove(ability);
            }
        }
        Map<Skill, Proficiency> skills = new EnumMap<>(Skill.class);
        skills.putAll(sheet.skills());
        for (Skill skill : Skill.values()) {
            if (changed(previous, v -> v.skills.get(skill))) {
                Proficiency proficiency = this.skills.get(skill);
                if (proficiency == Proficiency.NONE) skills.remove(skill);
                else skills.put(skill, proficiency);
            }
        }

        // Le Style suit la classe : une classe sans Style le retire de la fiche.
        boolean styleChanged = changed(previous, GuidedValues::fightingStyle) || changed(previous, GuidedValues::characterClass);
        return new CharacterSheet(
                pick(previous, GuidedValues::characterClass, sheet.characterClass()),
                styleChanged ? fightingStyle : sheet.fightingStyle(),
                pick(previous, GuidedValues::level, sheet.level()),
                pick(previous, GuidedValues::species, sheet.species()),
                pick(previous, GuidedValues::size, sheet.size()),
                pick(previous, GuidedValues::background, sheet.background()), abilities,
                pick(previous, GuidedValues::proficiencyBonus, sheet.proficiencyBonus()),
                saves, skills,
                sheet.armorBonus(),
                pick(previous, GuidedValues::hitPointMax, sheet.hitPointMax()),
                pick(previous, GuidedValues::hitPoints, sheet.hitPoints()),
                pick(previous, GuidedValues::hitDice, sheet.hitDice()),
                pick(previous, GuidedValues::speed, sheet.speed()),
                pick(previous, GuidedValues::initiative, sheet.initiative()),
                sheet.otherAttacks(),
                pick(previous, GuidedValues::features, sheet.features()),
                pick(previous, GuidedValues::speciesTraits, sheet.speciesTraits()),
                pickWithdrawn(previous, GuidedValues::feats, sheet.feats()),
                pick(previous, GuidedValues::inventory, sheet.inventory()),
                pick(previous, GuidedValues::proficiencies, sheet.proficiencies()),
                sheet.languages(),
                changed(previous, GuidedValues::spellSlots) ? spellSlots : sheet.spellSlots(),
                changed(previous, GuidedValues::spellcastingAbility) ? spellcastingAbility : sheet.spellcastingAbility(),
                changed(previous, GuidedValues::cantrips) ? cantrips : sheet.cantrips(),
                changed(previous, GuidedValues::spellbook) ? spellbook : sheet.spellbook(),
                changed(previous, GuidedValues::preparedSpells) ? preparedSpells : sheet.preparedSpells());
    }

    /**
     * La valeur dérivée d'un champ s'il a changé depuis {@code previous}, sinon celle de la fiche ; un champ que cette
     * construction ne dérive pas ({@code null}) garde la fiche.
     */
    private <T> T pick(GuidedValues previous, Function<GuidedValues, T> field, T onSheet) {
        T derived = field.apply(this);
        return derived != null && changed(previous, field) ? derived : onSheet;
    }

    /**
     * Comme {@link #pick}, mais un texte que la construction précédente dérivait et que celle-ci ne dérive plus (don de
     * Polyvalent retiré) est vidé, plutôt que gardé sur la fiche.
     */
    private String pickWithdrawn(GuidedValues previous, Function<GuidedValues, String> field, String onSheet) {
        boolean withdrawn = field.apply(this) == null && previous != null && field.apply(previous) != null;
        return withdrawn ? "" : pick(previous, field, onSheet);
    }

    private boolean changed(GuidedValues previous, Function<GuidedValues, ?> field) {
        return previous == null || !Objects.equals(field.apply(previous), field.apply(this));
    }

    private static int modifier(int score) {
        return Math.floorDiv(score - 10, 2);
    }

    private static String features(Build build) {
        CharacterClass characterClass = build.characterClass();
        List<String> lines = new ArrayList<>();
        if (characterClass.masteryCount() > 0) {
            String masteries = build.masteries().stream()
                    .map(weapon -> "%s (%s)".formatted(weapon.label(), weapon.mastery()))
                    .collect(Collectors.joining(", "));
            lines.add("Bottes d’arme : " + masteries);
        }
        switch (characterClass) {
            case FIGHTER -> {
                lines.add("Second souffle (2 utilisations) : action Bonus, 1d10 + %d PV".formatted(LEVEL));
                lines.add("Style de combat : %s — %s".formatted(build.fightingStyle().label(), build.fightingStyle().text()));
            }
            case BARBARIAN -> {
                lines.add("Rage (2 utilisations, 1 au repos court, toutes au repos long ; dégâts +2) : action Bonus, sans armure lourde ; Résistance aux dégâts "
                        + "contondants, perforants et tranchants, +2 aux dégâts des attaques de Force, Avantage aux tests "
                        + "et jets de sauvegarde de Force ; ni Concentration ni sorts");
                lines.add("Défense sans armure : sans armure, CA 10 + Dex + Con ; bouclier permis");
            }
            case MONK -> {
                lines.add("Arts martiaux (1d6) : à mains nues ou avec une arme de Moine, sans armure ni bouclier, "
                        + "Dex au lieu de For, 1d6 à la place des dégâts, attaque à mains nues par une action Bonus");
                lines.add("Défense sans armure : sans armure ni bouclier, CA 10 + Dex + Sag");
            }
            case WIZARD -> {
                CharacterClass.Spellcasting casting = characterClass.spellcasting();
                lines.add("Sorts (Intelligence) : sorts mineurs " + spellsText(build.cantrips(), casting.cantrips())
                        + " ; grimoire " + spellsText(build.spellbook(), casting.spellbook())
                        + " ; préparés " + spellsText(build.prepared(), casting.prepared())
                        + " ; changer de sorts préparés au repos long");
                lines.add("Savoir rituel : lancer comme rituel un sort rituel du grimoire, sans le préparer");
                lines.add("Restauration magique : au repos court, récupérer des emplacements dépensés (niveaux au total : "
                        + "la moitié du niveau de Magicien, arrondie au supérieur), une fois par repos long");
            }
            case ROGUE -> {
                lines.add("Expertise : " + expertiseText(build));
                lines.add("Attaque sournoise (1d6) : une fois par tour, avec une arme Finesse ou à distance, si l’attaque "
                        + "a l’Avantage ou qu’un allié est à 1,50 m de la cible");
                lines.add("Argot des voleurs : l’argot des voleurs et une autre langue au choix");
            }
        }
        return String.join("\n", lines);
    }

    /** Des sorts choisis, dans l'ordre, puis combien restent à choisir. */
    private static String spellsText(List<Spell> spells, int count) {
        String list = spells.stream().map(Spell::label).collect(Collectors.joining(", "));
        int missing = count - spells.size();
        if (missing > 0) list += (list.isEmpty() ? "" : ", ") + missing + " à choisir";
        return list;
    }

    /** Les Expertises choisies, dans l'ordre des compétences, puis combien restent à choisir. */
    private static String expertiseText(Build build) {
        List<String> chosen = Arrays.stream(Skill.values()).filter(build.expertise()::contains).map(Skill::label).toList();
        int missing = build.characterClass().expertiseCount() - chosen.size();
        String list = String.join(", ", chosen);
        if (missing > 0) list += (list.isEmpty() ? "" : ", ") + missing + " à choisir";
        return list;
    }
}
