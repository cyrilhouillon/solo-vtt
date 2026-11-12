package dnd.engine.sheet;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ce qu'une construction guidée écrit sur la fiche, champ par champ (SRD 5.2.1, niveau 1). Langues et emplacements de
 * sort n'en dérivent pas : ils restent au joueur. Espèce, taille, vitesse et traits d'espèce valent {@code null} tant que
 * la construction n'a pas d'espèce, historique tant qu'elle n'a pas d'historique, dons tant qu'elle n'a ni historique ni
 * Polyvalent : la fiche garde alors ce que le joueur y a saisi.
 *
 * <p>{@link #rebuild} ne réécrit que les champs dont la valeur dérivée a changé depuis la construction précédente : une
 * retouche du joueur survit tant que le choix dont elle dépend ne change pas.</p>
 */
public record GuidedValues(String className, int level, int proficiencyBonus, Map<Ability, Integer> abilities,
                           Set<Ability> savingThrows, Map<Skill, Proficiency> skills,
                           int armorClass, int hitPointMax, int hitPoints, String hitDice, int initiative,
                           List<SheetAttack> attacks, String features, String equipment, String proficiencies,
                           String species, String size, Double speed, String speciesTraits,
                           String background, String feats) {

    /** Aucune construction précédente : {@link #rebuild} réécrit tous les champs dérivés. */
    public static final GuidedValues NONE = new GuidedValues("", 0, 0, Map.of(), Set.of(), Map.of(),
            0, 0, 0, "", 0, List.of(), "", "", "", null, null, null, null, null, null);

    private static final int LEVEL = 1;
    private static final int PROFICIENCY_BONUS = 2;
    private static final int DEFENSE_BONUS = 1;
    private static final int ARCHERY_BONUS = 2;
    private static final double MELEE_REACH = 1.5;
    private static final double REACH_WEAPON = 3;

    public GuidedValues {
        abilities = Map.copyOf(abilities);
        savingThrows = Set.copyOf(savingThrows);
        skills = Map.copyOf(skills);
        attacks = List.copyOf(attacks);
    }

    /** Les valeurs qu'une construction donne à une fiche qui porte ses valeurs de caractéristique, augmentations comprises. */
    public static GuidedValues of(Build build) {
        return of(build, build.abilities());
    }

    /**
     * Les valeurs qu'une construction donne à la fiche ; la construction est supposée valide. CA, attaques, initiative et
     * PV suivent les caractéristiques de la fiche ({@code abilities}), retouches comprises (hausses de l'historique…).
     *
     * @param abilities les valeurs de caractéristique de la fiche, une fois la construction appliquée
     */
    public static GuidedValues of(Build build, Map<Ability, Integer> abilities) {
        CharacterClass characterClass = build.characterClass();
        int str = modifier(abilities.get(Ability.STRENGTH));
        int dex = modifier(abilities.get(Ability.DEXTERITY));
        int con = modifier(abilities.get(Ability.CONSTITUTION));

        SpeciesChoice species = build.species();
        BackgroundChoice background = build.background();
        Set<Skill> granted = EnumSet.noneOf(Skill.class);
        granted.addAll(build.skills());
        granted.addAll(build.backgroundSkills());
        if (species != null) granted.addAll(species.grantedSkills());
        Map<Skill, Proficiency> skills = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) skills.put(skill, granted.contains(skill) ? Proficiency.PROFICIENT : Proficiency.NONE);
        int hitPoints = characterClass.hitDie() + con + (species == null ? 0 : species.species().hitPointsPerLevel() * LEVEL);

        // Les armes de l'historique suivent celles de la classe, sans doublon.
        List<SrdWeapon> weapons = new ArrayList<>(build.equipment().weapons());
        if (background != null) background.weapons().stream().filter(w -> !weapons.contains(w)).forEach(weapons::add);
        List<SheetAttack> attacks = weapons.stream().map(weapon -> attack(weapon, str, dex, build.fightingStyle())).toList();

        List<OriginFeatChoice> feats = new ArrayList<>();
        if (background != null) feats.add(background.feat());
        if (species != null && species.originFeat() != null) feats.add(species.originFeat());
        boolean alert = feats.stream().anyMatch(feat -> feat.feat() == OriginFeat.ALERT);

        return new GuidedValues(characterClass.label(), LEVEL, PROFICIENCY_BONUS, build.abilities(),
                EnumSet.copyOf(characterClass.savingThrows()), skills,
                armorClass(build.equipment(), dex, build.fightingStyle()), hitPoints, hitPoints,
                "1d" + characterClass.hitDie(), dex + (alert ? PROFICIENCY_BONUS : 0), attacks,
                features(build),
                background == null ? build.equipment().text() : build.equipment().text() + "\n" + background.equipmentText(),
                background == null ? characterClass.proficiencies()
                        : characterClass.proficiencies() + "\nOutils : " + background.toolText(),
                species == null ? null : species.species().label(),
                species == null ? null : species.sizeText(),
                species == null ? null : species.speed(),
                species == null ? null : species.traitsText(),
                background == null ? null : background.background().label(),
                feats.isEmpty() ? null : feats.stream().map(OriginFeatChoice::text).collect(Collectors.joining("\n")));
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

        return new CharacterSheet(
                pick(previous, GuidedValues::className, sheet.className()),
                pick(previous, GuidedValues::level, sheet.level()),
                pick(previous, GuidedValues::species, sheet.species()),
                pick(previous, GuidedValues::size, sheet.size()),
                pick(previous, GuidedValues::background, sheet.background()), abilities,
                pick(previous, GuidedValues::proficiencyBonus, sheet.proficiencyBonus()),
                saves, skills,
                pick(previous, GuidedValues::armorClass, sheet.armorClass()),
                pick(previous, GuidedValues::hitPointMax, sheet.hitPointMax()),
                pick(previous, GuidedValues::hitPoints, sheet.hitPoints()),
                pick(previous, GuidedValues::hitDice, sheet.hitDice()),
                pick(previous, GuidedValues::speed, sheet.speed()),
                pick(previous, GuidedValues::initiative, sheet.initiative()),
                pick(previous, GuidedValues::attacks, sheet.attacks()),
                pick(previous, GuidedValues::features, sheet.features()),
                pick(previous, GuidedValues::speciesTraits, sheet.speciesTraits()),
                pickWithdrawn(previous, GuidedValues::feats, sheet.feats()),
                pick(previous, GuidedValues::equipment, sheet.equipment()),
                pick(previous, GuidedValues::proficiencies, sheet.proficiencies()),
                sheet.languages(), sheet.spellSlots());
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

    /** Armure (Dextérité selon sa catégorie), bouclier, et Défense si une armure est portée. */
    private static int armorClass(StartingEquipment equipment, int dex, FightingStyle style) {
        SrdArmor armor = equipment.armor();
        int armorClass = armor == null ? 10 + dex : armor.armorClass(dex);
        if (equipment.shield()) armorClass += SrdArmor.SHIELD.armorClass();
        if (armor != null && style == FightingStyle.DEFENSE) armorClass += DEFENSE_BONUS;
        return armorClass;
    }

    /**
     * Maîtrise + Force, Dextérité à distance, la meilleure en Finesse ; + 2 en Archerie avec une arme à distance. Allonge
     * 1,5 m (3 m pour Allonge) au corps à corps, aucune à distance ; portée pour Lancer et Munitions.
     */
    static SheetAttack attack(SrdWeapon weapon, int str, int dex, FightingStyle style) {
        boolean ranged = weapon.category().ranged();
        int ability = weapon.has(SrdWeapon.Property.FINESSE) ? Math.max(str, dex) : ranged ? dex : str;
        int bonus = PROFICIENCY_BONUS + ability + (ranged && style == FightingStyle.ARCHERY ? ARCHERY_BONUS : 0);
        Double reach = ranged ? null : weapon.has(SrdWeapon.Property.REACH) ? REACH_WEAPON : MELEE_REACH;
        boolean hasRange = weapon.normalRange() > 0;
        // Des dégâts fixes, sans dé (sarbacane), ne prennent pas le modificateur (SRD 5.2.1 p. 16, « Damage Rolls »).
        int damageBonus = weapon.diceCount() == 0 ? weapon.flatDamage() : ability;
        return new SheetAttack(weapon.label(), bonus, weapon.diceCount(), weapon.dieSides(), damageBonus,
                weapon.damageType(), reach, hasRange ? weapon.normalRange() : null, hasRange ? weapon.longRange() : null);
    }

    private static String features(Build build) {
        String masteries = build.masteries().stream()
                .map(weapon -> "%s (%s)".formatted(weapon.label(), weapon.mastery()))
                .collect(Collectors.joining(", "));
        List<String> lines = new ArrayList<>();
        lines.add("Bottes d’arme : " + masteries);
        switch (build.characterClass()) {
            case FIGHTER -> lines.add("Second souffle (2 utilisations) : action Bonus, 1d10 + %d PV".formatted(LEVEL));
        }
        lines.add("Style de combat : %s — %s".formatted(build.fightingStyle().label(), build.fightingStyle().text()));
        return String.join("\n", lines);
    }
}
