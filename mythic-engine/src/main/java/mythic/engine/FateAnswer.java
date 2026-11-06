package mythic.engine;

import java.util.Optional;

/** Ce que l'oracle répond : le d100, la probabilité et le chaos de la question, la réponse et l'éventuel événement. */
public record FateAnswer(int roll, Odds odds, ChaosFactor chaos, Answer answer, Optional<RandomEvent> randomEvent) {
}
