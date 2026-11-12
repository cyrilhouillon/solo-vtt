package dnd.engine.sheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Les choix de la création guidée d'un personnage : classe, méthode et valeurs de caractéristique, compétences, Style de
 * combat, équipement de départ et Bottes d'arme. Le record n'impose rien : {@link #validate} dit ce qui est impossible.
 */
public record Build(CharacterClass characterClass, AbilityMethod method, Map<Ability, Integer> scores,
                    Set<Skill> skills, FightingStyle fightingStyle, StartingEquipment equipment,
                    List<SrdWeapon> masteries) {

    public Build {
        scores = scores == null || scores.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(scores));
        skills = skills == null ? Set.of() : Collections.unmodifiableSet(new HashSet<>(skills));
        masteries = masteries == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(masteries));
    }

    /** Un Guerrier jouable d'emblée : répartition standard suggérée, Défense, équipement A et ses trois armes en Bottes. */
    public static Build fighter() {
        CharacterClass fighter = CharacterClass.FIGHTER;
        StartingEquipment equipment = StartingEquipment.FIGHTER_A;
        return new Build(fighter, AbilityMethod.STANDARD_ARRAY, fighter.suggestedScores(), Set.of(),
                FightingStyle.DEFENSE, equipment, equipment.weapons());
    }

    /**
     * Refuse une construction impossible, avec la raison.
     *
     * @param rolls les jets de caractéristique de la fiche, s'il y en a
     */
    public void validate(List<AbilityRoll> rolls) {
        if (characterClass == null) throw new IllegalArgumentException("Choisir une classe");
        if (method == null) throw new IllegalArgumentException("Choisir une méthode de caractéristiques");
        method.validate(scores, rolls);
        if (skills.contains(null)) throw new IllegalArgumentException("Compétence inconnue");
        if (skills.size() > characterClass.skillCount()) {
            throw new IllegalArgumentException("%s : %d compétences au plus".formatted(characterClass.label(), characterClass.skillCount()));
        }
        for (Skill skill : skills) {
            if (!characterClass.skillChoices().contains(skill)) {
                throw new IllegalArgumentException("%s hors des compétences du %s".formatted(skill.label(), characterClass.label()));
            }
        }
        if (fightingStyle == null) throw new IllegalArgumentException("Choisir un Style de combat");
        if (equipment == null || equipment.characterClass() != characterClass) {
            throw new IllegalArgumentException("Choisir un équipement de départ du " + characterClass.label());
        }
        if (masteries.contains(null)) throw new IllegalArgumentException("Botte d'arme inconnue");
        if (masteries.size() != characterClass.masteryCount() || Set.copyOf(masteries).size() != masteries.size()) {
            throw new IllegalArgumentException("Bottes d'arme : %d types d'arme différents".formatted(characterClass.masteryCount()));
        }
    }
}
