package mythic.server;

import com.fasterxml.jackson.databind.JsonNode;
import mythic.engine.AdventureList;
import mythic.engine.FateAnswer;
import mythic.engine.ListDraw;
import mythic.engine.Meaning;
import mythic.engine.Odds;
import mythic.engine.RandomEvent;
import mythic.engine.SceneAdjustment;
import mythic.engine.SceneCheck;

import java.util.List;
import java.util.function.Function;

/** Ce que l'oracle échange avec le client. Chaque code d'énumération voyage avec son libellé. */
public final class MythicDtos {

    private MythicDtos() {
    }

    /** Le facteur de chaos de la campagne, de 1 à 9. */
    public record ChaosDto(int chaos) {}

    /** Les pages des listes de la campagne, dans la section Mythic du Carnet. */
    public record ListsDto(String threadsPageId, String charactersPageId) {}

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

    /**
     * Le tirage dans une liste. {@code empty} : aucune entrée, le focus devient Contexte actuel ; {@code full} : une entrée
     * au-delà de la 25e ligne, rien n'est tiré. Sinon la ligne (1 à 25), ses dés ({@code sectionDie} et
     * {@code sectionRoll} nuls avec une seule section active) et, si elle est écrite, son entrée : ses nœuds en ligne
     * (texte, liens de page) et le texte qu'on y lit ; {@code blank} : ligne vide, « Choisir ».
     */
    public record ListDrawDto(String list, String listLabel, boolean empty, boolean full, Integer line, Integer sectionDie,
                              Integer sectionRoll, Integer lineRoll, boolean blank, List<JsonNode> entry, String entryText) {
        static ListDrawDto of(ListDraw draw, Function<AdventureList.Kind, List<ListLines.Line>> lines) {
            String list = draw.kind().name();
            String label = draw.kind().label();
            return switch (draw) {
                case ListDraw.Empty e -> new ListDrawDto(list, label, true, false, null, null, null, null, false, null, null);
                case ListDraw.Full f -> new ListDrawDto(list, label, false, true, null, null, null, null, false, null, null);
                case ListDraw.Line l -> {
                    ListLines.Line entry = l.blank() ? null : lines.apply(draw.kind()).get(l.line() - 1);
                    yield new ListDrawDto(list, label, false, false, l.line(), l.sectionDie(), l.sectionRoll(), l.lineRoll(),
                            l.blank(), entry == null ? null : entry.inline(), entry == null ? null : entry.text());
                }
            };
        }
    }

    /**
     * {@code meaning} : deux mots d'action, nul sans tables de sens importées. {@code effectiveFocus} : Contexte actuel
     * quand la liste visée est vide, sinon le focus tiré. {@code list} : nul si le focus ne vise aucune liste.
     */
    public record RandomEventDto(String focus, String focusLabel, int focusRoll, MeaningDto meaning, String effectiveFocus,
                                 String effectiveFocusLabel, ListDrawDto list) {
        static RandomEventDto of(RandomEvent event, Function<AdventureList.Kind, List<ListLines.Line>> lines) {
            return new RandomEventDto(event.focus().name(), event.focus().label(), event.focusRoll(),
                    event.meaning().map(MeaningDto::of).orElse(null), event.effectiveFocus().name(),
                    event.effectiveFocus().label(), event.draw().map(d -> ListDrawDto.of(d, lines)).orElse(null));
        }
    }

    public record AnswerDto(String question, int roll, OddsDto odds, int chaos, String answer, String answerLabel,
                            RandomEventDto randomEvent) {
        static AnswerDto of(String question, FateAnswer fate, Function<AdventureList.Kind, List<ListLines.Line>> lines) {
            RandomEventDto event = fate.randomEvent().map(e -> RandomEventDto.of(e, lines)).orElse(null);
            return new AnswerDto(question, fate.roll(), OddsDto.of(fate.odds()), fate.chaos().value(),
                    fate.answer().name(), fate.answer().label(), event);
        }
    }

    /** {@code expected} est facultative : la scène attendue revient telle quelle, pour être écrite avec son issue. */
    public record SceneRequest(String expected) {}

    /** Un ajustement de scène. */
    public record AdjustmentDto(String code, String label) {
        static AdjustmentDto of(SceneAdjustment adjustment) {
            return new AdjustmentDto(adjustment.name(), adjustment.label());
        }
    }

    /**
     * Le test d'une scène attendue : le d10 et le chaos, l'issue ; pour une scène modifiée, ses ajustements et tous les d10
     * de la table ({@code adjustmentRolls}, le premier de 7 à 10 quand il y en a deux) ; pour une scène interrompue, son
     * événement aléatoire, nul sinon.
     */
    public record SceneDto(String expected, int chaos, int roll, String outcome, String outcomeLabel,
                           List<AdjustmentDto> adjustments, List<Integer> adjustmentRolls, RandomEventDto randomEvent) {
        static SceneDto of(String expected, SceneCheck check, Function<AdventureList.Kind, List<ListLines.Line>> lines) {
            return new SceneDto(expected, check.chaos().value(), check.roll(), check.outcome().name(), check.outcome().label(),
                    check.adjustments().stream().map(AdjustmentDto::of).toList(), check.adjustmentRolls(),
                    check.event().map(e -> RandomEventDto.of(e, lines)).orElse(null));
        }
    }

    /** {@code inControl} : le joueur a-t-il gardé le contrôle de la scène ? */
    public record SceneEndRequest(Boolean inControl) {}

    /** Le chaos avant et après la fin de scène ; égaux à une borne. */
    public record SceneEndDto(boolean inControl, int before, int after) {}
}
