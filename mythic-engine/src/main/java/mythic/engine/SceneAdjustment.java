package mythic.engine;

import solovtt.engine.Dice;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * La table d'ajustement de scène de Mythic GME 2e, qui dit ce qui change dans une scène modifiée. Un d10 de 1 à 6 donne
 * un ajustement ; de 7 à 10, deux : on relance jusqu'à deux résultats de 1 à 6, un nouveau 7 à 10 étant ignoré. Un
 * second résultat contraire du premier (ajouter et retirer la même chose) est ignoré : seul le premier compte. Deux fois
 * le même ajustement comptent deux fois.
 */
public enum SceneAdjustment {
    REMOVE_A_CHARACTER("Retirer un personnage"),
    ADD_A_CHARACTER("Ajouter un personnage"),
    REDUCE_AN_ACTIVITY("Réduire ou retirer une activité"),
    INCREASE_AN_ACTIVITY("Intensifier une activité"),
    REMOVE_AN_OBJECT("Retirer un objet"),
    ADD_AN_OBJECT("Ajouter un objet");

    /** Le plus haut d10 qui donne un seul ajustement ; au-delà, deux. */
    static final int SINGLE = 6;

    /** Les ajustements tirés, et tous les d10 lancés pour les tirer, dans l'ordre. */
    public record Rolled(List<SceneAdjustment> adjustments, List<Integer> rolls) {
        public Rolled {
            adjustments = List.copyOf(adjustments);
            rolls = List.copyOf(rolls);
        }
    }

    private final String label;

    SceneAdjustment(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** L'ajustement d'un d10 ; aucun de 7 à 10 (deux ajustements). */
    public static Optional<SceneAdjustment> of(int roll) {
        if (roll < 1 || roll > 10) throw new IllegalArgumentException("Un d10 va de 1 à 10 : " + roll);
        return roll <= SINGLE ? Optional.of(values()[roll - 1]) : Optional.empty();
    }

    /** L'ajustement contraire : ajouter et retirer la même chose, réduire et intensifier une activité. */
    public SceneAdjustment opposite() {
        return values()[ordinal() ^ 1];
    }

    /** Tire un ou deux ajustements selon la table. */
    public static Rolled roll(Dice dice) {
        List<Integer> rolls = new ArrayList<>();
        Optional<SceneAdjustment> single = next(dice, rolls);
        if (single.isPresent()) return new Rolled(List.of(single.get()), rolls);
        SceneAdjustment first = untilOne(dice, rolls);
        SceneAdjustment second = untilOne(dice, rolls);
        return new Rolled(second == first.opposite() ? List.of(first) : List.of(first, second), rolls);
    }

    private static Optional<SceneAdjustment> next(Dice dice, List<Integer> rolls) {
        int roll = dice.roll(10);
        rolls.add(roll);
        return of(roll);
    }

    /** Relance jusqu'à un ajustement ; un 7 à 10 est ignoré. */
    private static SceneAdjustment untilOne(Dice dice, List<Integer> rolls) {
        Optional<SceneAdjustment> adjustment;
        do adjustment = next(dice, rolls); while (adjustment.isEmpty());
        return adjustment.get();
    }
}
