package mythic.engine;

import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FateQuestionTest {

    private static final ChaosFactor CHAOS_5 = ChaosFactor.of(5);

    @Test
    void probable_auChaos5_sousLeSeuil_oui_sansEvenement() {
        var reponse = FateQuestion.ask(Odds.LIKELY, CHAOS_5, Dice.scripted(42));

        assertEquals(new FateAnswer(42, Odds.LIKELY, CHAOS_5, Answer.YES, Optional.empty()), reponse);
    }

    @Test
    void chaqueSeuil_donneSaReponse() {
        assertEquals(Answer.EXCEPTIONAL_YES, FateQuestion.ask(Odds.LIKELY, CHAOS_5, Dice.scripted(13)).answer());
        assertEquals(Answer.YES, FateQuestion.ask(Odds.LIKELY, CHAOS_5, Dice.scripted(65)).answer());
        assertEquals(Answer.NO, FateQuestion.ask(Odds.LIKELY, CHAOS_5, Dice.scripted(70)).answer());
        assertEquals(Answer.EXCEPTIONAL_NO, FateQuestion.ask(Odds.LIKELY, CHAOS_5, Dice.scripted(94)).answer());
    }

    @Test
    void unDoubleSousLeChaos_declencheUnEvenement_dontUnSecondD100TireLeFocus() {
        var reponse = FateQuestion.ask(Odds.LIKELY, CHAOS_5, Dice.scripted(33, 25));

        assertEquals(Answer.YES, reponse.answer());
        assertEquals(Optional.of(new RandomEvent(EventFocus.NPC_ACTION, 25)), reponse.randomEvent());
    }

    @Test
    void unDoubleAuChaos_declencheUnEvenement() {
        var reponse = FateQuestion.ask(Odds.LIKELY, CHAOS_5, Dice.scripted(55, 1));

        assertEquals(Optional.of(new RandomEvent(EventFocus.REMOTE_EVENT, 1)), reponse.randomEvent());
    }

    @Test
    void unDoubleAuDessusDuChaos_neDeclencheRien() {
        var reponse = FateQuestion.ask(Odds.LIKELY, CHAOS_5, Dice.scripted(66));

        assertEquals(Optional.empty(), reponse.randomEvent());
    }

    @Test
    void cent_estLeDouble00_etDeclencheToujours_memeAuChaos1() {
        var reponse = FateQuestion.ask(Odds.FIFTY_FIFTY, ChaosFactor.of(1), Dice.scripted(100, 90));

        assertEquals(Answer.EXCEPTIONAL_NO, reponse.answer());
        assertEquals(Optional.of(new RandomEvent(EventFocus.CURRENT_CONTEXT, 90)), reponse.randomEvent());
    }

    @Test
    void unJetQuiNEstPasUnDouble_neDeclencheRien() {
        assertEquals(false, FateQuestion.triggersRandomEvent(10, ChaosFactor.of(9)));
        assertEquals(false, FateQuestion.triggersRandomEvent(1, ChaosFactor.of(9)));
        assertEquals(false, FateQuestion.triggersRandomEvent(99, ChaosFactor.of(8)));
        assertEquals(true, FateQuestion.triggersRandomEvent(99, ChaosFactor.of(9)));
        assertEquals(true, FateQuestion.triggersRandomEvent(11, ChaosFactor.of(1)));
    }
}
