package mythic.server;

import mythic.engine.FateAnswer;
import mythic.engine.Odds;

/** Ce que l'oracle échange avec le client. Chaque code d'énumération voyage avec son libellé. */
public final class MythicDtos {

    private MythicDtos() {
    }

    /** Le facteur de chaos de la campagne, de 1 à 9. */
    public record ChaosDto(int chaos) {}

    /** Une probabilité, d'« Impossible » à « Certain ». */
    public record OddsDto(String code, String label) {
        static OddsDto of(Odds odds) {
            return new OddsDto(odds.name(), odds.label());
        }
    }

    /** {@code question} est facultative : elle revient telle quelle, pour être écrite avec la réponse. */
    public record QuestionRequest(String question, String odds) {}

    public record RandomEventDto(String focus, String focusLabel, int focusRoll) {}

    public record AnswerDto(String question, int roll, OddsDto odds, int chaos, String answer, String answerLabel,
                            RandomEventDto randomEvent) {
        static AnswerDto of(String question, FateAnswer fate) {
            RandomEventDto event = fate.randomEvent()
                    .map(e -> new RandomEventDto(e.focus().name(), e.focus().label(), e.focusRoll()))
                    .orElse(null);
            return new AnswerDto(question, fate.roll(), OddsDto.of(fate.odds()), fate.chaos().value(),
                    fate.answer().name(), fate.answer().label(), event);
        }
    }
}
