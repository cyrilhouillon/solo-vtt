package dnd.engine.sheet;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Les classes que la création guidée connaît, avec leurs traits de base au niveau 1 (SRD 5.2.1). */
public enum CharacterClass {
    FIGHTER("Guerrier", 10, EnumSet.of(Ability.STRENGTH, Ability.CONSTITUTION),
            EnumSet.of(Skill.ACROBATICS, Skill.ATHLETICS, Skill.ANIMAL_HANDLING, Skill.HISTORY, Skill.INSIGHT,
                    Skill.INTIMIDATION, Skill.PERCEPTION, Skill.PERSUASION, Skill.SURVIVAL), 2,
            scores(15, 14, 13, 8, 10, 12), 3,
            "Armes courantes et armes de guerre ; armures légères, intermédiaires et lourdes, boucliers");

    private final String label;
    private final int hitDie;
    private final Set<Ability> savingThrows;
    private final Set<Skill> skillChoices;
    private final int skillCount;
    private final Map<Ability, Integer> suggestedScores;
    private final int masteryCount;
    private final String proficiencies;

    CharacterClass(String label, int hitDie, Set<Ability> savingThrows, Set<Skill> skillChoices, int skillCount,
                   Map<Ability, Integer> suggestedScores, int masteryCount, String proficiencies) {
        this.label = label;
        this.hitDie = hitDie;
        this.savingThrows = Set.copyOf(savingThrows);
        this.skillChoices = Set.copyOf(skillChoices);
        this.skillCount = skillCount;
        this.suggestedScores = Map.copyOf(suggestedScores);
        this.masteryCount = masteryCount;
        this.proficiencies = proficiencies;
    }

    public String label() {
        return label;
    }

    /** Le nombre de faces du dé de vie. */
    public int hitDie() {
        return hitDie;
    }

    public Set<Ability> savingThrows() {
        return savingThrows;
    }

    /** Les compétences parmi lesquelles la classe choisit. */
    public Set<Skill> skillChoices() {
        return skillChoices;
    }

    /** Combien de compétences la classe choisit. */
    public int skillCount() {
        return skillCount;
    }

    /** La répartition des valeurs standard que le SRD suggère pour la classe. */
    public Map<Ability, Integer> suggestedScores() {
        return suggestedScores;
    }

    /** Le nombre de Bottes d'arme au niveau 1. */
    public int masteryCount() {
        return masteryCount;
    }

    /** Les maîtrises d'arme et la formation aux armures, en texte. */
    public String proficiencies() {
        return proficiencies;
    }

    /** Les équipements de départ que la classe propose. */
    public List<StartingEquipment> equipments() {
        return Arrays.stream(StartingEquipment.values()).filter(equipment -> equipment.characterClass() == this).toList();
    }

    private static Map<Ability, Integer> scores(int str, int dex, int con, int intel, int wis, int cha) {
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        scores.put(Ability.STRENGTH, str);
        scores.put(Ability.DEXTERITY, dex);
        scores.put(Ability.CONSTITUTION, con);
        scores.put(Ability.INTELLIGENCE, intel);
        scores.put(Ability.WISDOM, wis);
        scores.put(Ability.CHARISMA, cha);
        return scores;
    }
}
