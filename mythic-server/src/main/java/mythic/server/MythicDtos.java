package mythic.server;

import mythic.engine.FateAnswer;
import mythic.engine.Meaning;
import mythic.engine.Odds;

import java.util.List;

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

    /** Les sortes de tables de sens : les deux colonnes d'Actions ou de Descriptions, ou une table d'éléments nommée. */
    public static final String ACTIONS = "ACTIONS";
    public static final String DESCRIPTIONS = "DESCRIPTIONS";
    public static final String ELEMENT = "ELEMENT";

    /** Une table de sens importée ; {@code element} nomme une table d'éléments, nul sinon. */
    public record MeaningTableDto(String kind, String element, String label) {}

    /** Les tables importées, vides tant que le joueur n'a rien importé. */
    public record MeaningTablesDto(boolean imported, List<MeaningTableDto> tables) {}

    /** {@code table} : {@code ACTIONS}, {@code DESCRIPTIONS} ou {@code ELEMENT}, avec alors le nom de l'élément. */
    public record MeaningRequest(String table, String element) {}

    /** Deux mots tirés dans une table, avec leurs d100 ; {@code table} est son libellé. */
    public record MeaningDto(String table, String first, int firstRoll, String second, int secondRoll) {
        static MeaningDto of(Meaning meaning) {
            return new MeaningDto(meaning.table(), meaning.first(), meaning.firstRoll(), meaning.second(), meaning.secondRoll());
        }
    }

    /** {@code meaning} : deux mots d'action, nul sans tables de sens importées. */
    public record RandomEventDto(String focus, String focusLabel, int focusRoll, MeaningDto meaning) {}

    public record AnswerDto(String question, int roll, OddsDto odds, int chaos, String answer, String answerLabel,
                            RandomEventDto randomEvent) {
        static AnswerDto of(String question, FateAnswer fate) {
            RandomEventDto event = fate.randomEvent()
                    .map(e -> new RandomEventDto(e.focus().name(), e.focus().label(), e.focusRoll(),
                            e.meaning().map(MeaningDto::of).orElse(null)))
                    .orElse(null);
            return new AnswerDto(question, fate.roll(), OddsDto.of(fate.odds()), fate.chaos().value(),
                    fate.answer().name(), fate.answer().label(), event);
        }
    }
}
