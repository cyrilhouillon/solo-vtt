package mythic.engine;

import solovtt.engine.Dice;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Le test d'une scène attendue au chaos, selon Mythic GME 2e : un d10 au-dessus du chaos, la scène se passe comme prévu ;
 * égal ou dessous, impair, elle est modifiée (la table d'ajustement dit comment) ; pair, elle est interrompue par un
 * événement aléatoire.
 *
 * @param adjustments les ajustements d'une scène modifiée, vides sinon
 * @param adjustmentRolls les d10 de la table d'ajustement, vides sinon
 * @param event l'événement d'une scène interrompue
 */
public record SceneCheck(int roll, ChaosFactor chaos, SceneOutcome outcome, List<SceneAdjustment> adjustments,
                         List<Integer> adjustmentRolls, Optional<RandomEvent> event) {

    public SceneCheck {
        adjustments = List.copyOf(adjustments);
        adjustmentRolls = List.copyOf(adjustmentRolls);
    }

    /**
     * @param lists la liste d'une sorte, lue seulement si le focus d'un événement la vise
     */
    public static SceneCheck test(ChaosFactor chaos, Optional<MeaningTables> tables,
                                  Function<AdventureList.Kind, AdventureList> lists, Dice dice) {
        int roll = dice.roll(10);
        if (roll > chaos.value()) return new SceneCheck(roll, chaos, SceneOutcome.EXPECTED, List.of(), List.of(), Optional.empty());
        if (roll % 2 == 1) {
            SceneAdjustment.Rolled rolled = SceneAdjustment.roll(dice);
            return new SceneCheck(roll, chaos, SceneOutcome.ALTERED, rolled.adjustments(), rolled.rolls(), Optional.empty());
        }
        return new SceneCheck(roll, chaos, SceneOutcome.INTERRUPTED, List.of(), List.of(),
                Optional.of(RandomEvent.roll(tables, lists, dice)));
    }
}
