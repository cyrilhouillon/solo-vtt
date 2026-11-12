package dnd.engine.sheet;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * La fiche d'un personnage D&D 2024 : ce que le joueur y écrit, et ce que les formules du SRD 5.2.1 en tirent
 * (modificateurs, sauvegardes, compétences, Perception passive). Le nom du personnage n'y est pas : c'est le titre de sa
 * page dans le Carnet.
 *
 * <p>Une compétence absente de {@code skills} n'est pas maîtrisée. Vitesse en mètres. Les textes absents valent
 * {@code ""}.</p>
 */
public record CharacterSheet(String className, int level, String species, String background,
                             Map<Ability, Integer> abilities, int proficiencyBonus,
                             Set<Ability> savingThrows, Map<Skill, Proficiency> skills,
                             int armorClass, int hitPointMax, int hitPoints, String hitDice, double speed, int initiative,
                             List<SheetAttack> attacks,
                             String features, String equipment, String proficiencies, String languages,
                             List<SpellSlots> spellSlots) {

    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 30;
    public static final int MAX_LEVEL = 20;
    public static final int MAX_PROFICIENCY_BONUS = 10;

    public CharacterSheet {
        className = text(className);
        species = text(species);
        background = text(background);
        hitDice = text(hitDice);
        features = text(features);
        equipment = text(equipment);
        proficiencies = text(proficiencies);
        languages = text(languages);
        if (level < 1 || level > MAX_LEVEL) throw new IllegalArgumentException("Niveau entre 1 et %d : %d".formatted(MAX_LEVEL, level));
        abilities = abilities == null ? Map.of() : abilities;
        for (Ability ability : Ability.values()) {
            Integer score = abilities.get(ability);
            if (score == null) throw new IllegalArgumentException("Valeur manquante : " + ability.label());
            if (score < MIN_SCORE || score > MAX_SCORE) {
                throw new IllegalArgumentException("%s entre %d et %d : %d".formatted(ability.label(), MIN_SCORE, MAX_SCORE, score));
            }
        }
        abilities = Map.copyOf(new EnumMap<>(abilities));
        if (proficiencyBonus < 0 || proficiencyBonus > MAX_PROFICIENCY_BONUS) {
            throw new IllegalArgumentException("Bonus de maîtrise entre 0 et %d : %d".formatted(MAX_PROFICIENCY_BONUS, proficiencyBonus));
        }
        savingThrows = savingThrows == null || savingThrows.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(savingThrows));
        skills = skills == null ? Map.of() : Map.copyOf(skills);
        if (armorClass < 0) throw new IllegalArgumentException("CA négative : " + armorClass);
        if (hitPointMax < 0) throw new IllegalArgumentException("PV maximum négatifs : " + hitPointMax);
        if (hitPoints < 0) throw new IllegalArgumentException("PV actuels négatifs : " + hitPoints);
        if (!(speed >= 0)) throw new IllegalArgumentException("Vitesse négative : " + speed);
        attacks = attacks == null ? List.of() : List.copyOf(attacks);
        spellSlots = spellSlots == null ? List.of() : List.copyOf(spellSlots);
        if (spellSlots.stream().map(SpellSlots::level).distinct().count() != spellSlots.size()) {
            throw new IllegalArgumentException("Un seul jeu d'emplacements par niveau de sort");
        }
    }

    /** La fiche d'un personnage qui vient d'être créé : niveau 1, toutes les valeurs à 10, bonus de maîtrise +2. */
    public static CharacterSheet blank() {
        Map<Ability, Integer> abilities = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) abilities.put(ability, 10);
        return new CharacterSheet("", 1, "", "", abilities, 2, Set.of(), Map.of(),
                10, 0, 0, "", 9, 0, List.of(), "", "", "", "", List.of());
    }

    /** Le modificateur d'une caractéristique : (valeur − 10) / 2, arrondi à l'inférieur. */
    public int modifier(Ability ability) {
        return Math.floorDiv(abilities.get(ability) - 10, 2);
    }

    /** Le bonus de sauvegarde : le modificateur, plus le bonus de maîtrise si la sauvegarde est maîtrisée. */
    public int savingThrow(Ability ability) {
        return modifier(ability) + (savingThrows.contains(ability) ? proficiencyBonus : 0);
    }

    /** La maîtrise d'une compétence, {@link Proficiency#NONE} si elle n'est pas notée. */
    public Proficiency proficiency(Skill skill) {
        return skills.getOrDefault(skill, Proficiency.NONE);
    }

    /** Le bonus d'une compétence : le modificateur de sa caractéristique, plus le bonus de maîtrise, deux fois en expertise. */
    public int skill(Skill skill) {
        return modifier(skill.ability()) + proficiency(skill).multiplier() * proficiencyBonus;
    }

    /** Perception passive : 10 + le bonus de Perception. */
    public int passivePerception() {
        return 10 + skill(Skill.PERCEPTION);
    }

    /** Un texte saisi, sans espaces aux bords ; {@code ""} s'il est absent. */
    static String text(String value) {
        return value == null ? "" : value.strip();
    }
}
