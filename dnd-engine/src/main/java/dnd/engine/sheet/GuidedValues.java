package dnd.engine.sheet;

import dnd.engine.state.DamageType;

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
    private static final String UNARMED_STRIKE = "Attaque à mains nues";
    private static final String TOOL_TO_CHOOSE = "outil d’artisan ou instrument de musique (à choisir)";

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
        Set<Skill> granted = build.proficientSkills();
        Map<Skill, Proficiency> skills = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) {
            Proficiency proficiency = build.expertise().contains(skill) ? Proficiency.EXPERTISE
                    : granted.contains(skill) ? Proficiency.PROFICIENT : Proficiency.NONE;
            skills.put(skill, proficiency);
        }
        int hitPoints = characterClass.hitDie() + con + (species == null ? 0 : species.species().hitPointsPerLevel() * LEVEL);

        // Les armes de l'historique suivent celles de la classe, sans doublon ; l'attaque à mains nues vient en dernier.
        StartingEquipment equipment = build.equipment();
        List<SrdWeapon> weapons = new ArrayList<>(equipment.weapons());
        if (background != null) background.weapons().stream().filter(w -> !weapons.contains(w)).forEach(weapons::add);
        List<SheetAttack> attacks = new ArrayList<>(weapons.stream()
                .map(weapon -> attack(weapon, str, dex, build.fightingStyle(), characterClass)).toList());
        attacks.add(unarmedStrike(str, dex, characterClass));

        List<OriginFeatChoice> feats = new ArrayList<>();
        if (background != null) feats.add(background.feat());
        if (species != null && species.originFeat() != null) feats.add(species.originFeat());
        boolean alert = feats.stream().anyMatch(feat -> feat.feat() == OriginFeat.ALERT);

        // Chaque outil une fois : celui de la classe, puis celui de l'historique.
        List<String> tools = new ArrayList<>();
        if (characterClass.tool() != null) tools.add(characterClass.tool().label());
        if (!characterClass.toolChoices().isEmpty()) tools.add(build.tool() == null ? TOOL_TO_CHOOSE : build.tool().label());
        if (background != null && !tools.contains(background.toolText())) tools.add(background.toolText());

        int unarmoredBonus = characterClass.unarmoredDefense() == null ? 0
                : modifier(abilities.get(characterClass.unarmoredDefense()));
        String equipmentText = equipment.text(build.tool());
        return new GuidedValues(characterClass.label(), LEVEL, PROFICIENCY_BONUS, build.abilities(),
                EnumSet.copyOf(characterClass.savingThrows()), skills,
                armorClass(characterClass, equipment.armor(), equipment.shield(), dex, unarmoredBonus, build.fightingStyle()),
                hitPoints, hitPoints,
                "1d" + characterClass.hitDie(), dex + (alert ? PROFICIENCY_BONUS : 0), attacks,
                features(build),
                background == null ? equipmentText : equipmentText + "\n" + background.equipmentText(),
                tools.isEmpty() ? characterClass.proficiencies()
                        : characterClass.proficiencies() + "\nOutils : " + String.join(", ", tools),
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

    /**
     * CA de ce qui est porté : l'armure (Dextérité selon sa catégorie) et Défense ; sans armure, la meilleure de 10 + Dex et
     * de la Défense sans armure de la classe (si le bouclier la permet) ; puis le bouclier.
     *
     * @param unarmoredBonus le modificateur de la caractéristique de Défense sans armure, 0 sans
     */
    static int armorClass(CharacterClass characterClass, SrdArmor armor, boolean shield, int dex, int unarmoredBonus,
                          FightingStyle style) {
        int armorClass;
        if (armor != null) {
            armorClass = armor.armorClass(dex) + (style == FightingStyle.DEFENSE ? DEFENSE_BONUS : 0);
        } else {
            armorClass = 10 + dex;
            boolean unarmored = characterClass.unarmoredDefense() != null
                    && (!shield || characterClass.unarmoredDefenseWithShield());
            if (unarmored) armorClass = Math.max(armorClass, 10 + dex + unarmoredBonus);
        }
        return armorClass + (shield ? SrdArmor.SHIELD.armorClass() : 0);
    }

    /**
     * Maîtrise + Force, Dextérité à distance, la meilleure en Finesse ; + 2 en Archerie avec une arme à distance. Une arme de
     * Moine, avec Arts martiaux, prend la meilleure de Force et Dextérité, et le dé d'Arts martiaux s'il est plus fort que le
     * sien. Allonge 1,5 m (3 m pour Allonge) au corps à corps, aucune à distance ; portée pour Lancer et Munitions.
     */
    static SheetAttack attack(SrdWeapon weapon, int str, int dex, FightingStyle style, CharacterClass characterClass) {
        boolean ranged = weapon.category().ranged();
        boolean martialArts = characterClass.martialArts() && CharacterClass.monkWeapon(weapon);
        int ability = martialArts || weapon.has(SrdWeapon.Property.FINESSE) ? Math.max(str, dex) : ranged ? dex : str;
        int bonus = PROFICIENCY_BONUS + ability + (ranged && style == FightingStyle.ARCHERY ? ARCHERY_BONUS : 0);
        Double reach = ranged ? null : weapon.has(SrdWeapon.Property.REACH) ? REACH_WEAPON : MELEE_REACH;
        boolean hasRange = weapon.normalRange() > 0;
        int diceCount = weapon.diceCount();
        int dieSides = weapon.dieSides();
        // Le dé le plus fort en moyenne : n × (faces + 1) / 2 contre (d + 1) / 2.
        if (martialArts && diceCount * (dieSides + 1) < CharacterClass.MARTIAL_ARTS_DIE + 1) {
            diceCount = 1;
            dieSides = CharacterClass.MARTIAL_ARTS_DIE;
        }
        // Des dégâts fixes, sans dé (sarbacane), ne prennent pas le modificateur (SRD 5.2.1 p. 16, « Damage Rolls »).
        int damageBonus = diceCount == 0 ? weapon.flatDamage() : ability;
        return new SheetAttack(weapon.label(), bonus, diceCount, dieSides, damageBonus,
                weapon.damageType(), reach, hasRange ? weapon.normalRange() : null, hasRange ? weapon.longRange() : null);
    }

    /**
     * L'attaque à mains nues de toute créature (SRD 5.2.1, « Unarmed Strike ») : Force + maîtrise, 1 + Force contondants ;
     * avec Arts martiaux, la meilleure de Force et Dextérité au toucher et aux dégâts, et le dé d'Arts martiaux.
     */
    static SheetAttack unarmedStrike(int str, int dex, CharacterClass characterClass) {
        if (characterClass.martialArts()) {
            int ability = Math.max(str, dex);
            return new SheetAttack(UNARMED_STRIKE, PROFICIENCY_BONUS + ability, 1, CharacterClass.MARTIAL_ARTS_DIE, ability,
                    DamageType.BLUDGEONING, MELEE_REACH, null, null);
        }
        return new SheetAttack(UNARMED_STRIKE, PROFICIENCY_BONUS + str, 0, 4, 1 + str, DamageType.BLUDGEONING,
                MELEE_REACH, null, null);
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
                lines.add("Rage (2 par jour, dégâts +2) : action Bonus, sans armure lourde ; Résistance aux dégâts "
                        + "contondants, perforants et tranchants, +2 aux dégâts des attaques de Force, Avantage aux tests "
                        + "et jets de sauvegarde de Force ; ni Concentration ni sorts");
                lines.add("Défense sans armure : sans armure, CA 10 + Dex + Con ; bouclier permis");
            }
            case MONK -> {
                lines.add("Arts martiaux (1d6) : à mains nues ou avec une arme de Moine, sans armure ni bouclier, "
                        + "Dex au lieu de For, 1d6 à la place des dégâts, attaque à mains nues par une action Bonus");
                lines.add("Défense sans armure : sans armure ni bouclier, CA 10 + Dex + Sag");
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

    /** Les Expertises choisies, dans l'ordre des compétences, puis combien restent à choisir. */
    private static String expertiseText(Build build) {
        List<String> chosen = Arrays.stream(Skill.values()).filter(build.expertise()::contains).map(Skill::label).toList();
        int missing = build.characterClass().expertiseCount() - chosen.size();
        String list = String.join(", ", chosen);
        if (missing > 0) list += (list.isEmpty() ? "" : ", ") + missing + " à choisir";
        return list;
    }
}
