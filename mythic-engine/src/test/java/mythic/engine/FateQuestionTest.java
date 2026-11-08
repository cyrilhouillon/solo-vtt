package mythic.engine;

import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FateQuestionTest {

    private static final ChaosFactor CHAOS_5 = ChaosFactor.of(5);
    private static final Optional<MeaningTables> SANS_TABLES = Optional.empty();
    /** Sans listes, un focus de PNJ trouve Personnages vide. */
    private static final Optional<ListDraw> PERSONNAGES_VIDE = Optional.of(new ListDraw.Empty(AdventureList.Kind.CHARACTERS));

    @Test
    void probable_auChaos5_sousLeSeuil_oui_sansEvenement() {
        var reponse = FateQuestion.ask(Odds.LIKELY, CHAOS_5, SANS_TABLES, Dice.scripted(42));

        assertEquals(new FateAnswer(42, Odds.LIKELY, CHAOS_5, Answer.YES, Optional.empty()), reponse);
    }

    @Test
    void chaqueSeuil_donneSaReponse() {
        assertEquals(Answer.EXCEPTIONAL_YES, FateQuestion.ask(Odds.LIKELY, CHAOS_5, SANS_TABLES, Dice.scripted(13)).answer());
        assertEquals(Answer.YES, FateQuestion.ask(Odds.LIKELY, CHAOS_5, SANS_TABLES, Dice.scripted(65)).answer());
        assertEquals(Answer.NO, FateQuestion.ask(Odds.LIKELY, CHAOS_5, SANS_TABLES, Dice.scripted(70)).answer());
        assertEquals(Answer.EXCEPTIONAL_NO, FateQuestion.ask(Odds.LIKELY, CHAOS_5, SANS_TABLES, Dice.scripted(94)).answer());
    }

    @Test
    void unDoubleSousLeChaos_declencheUnEvenement_dontUnSecondD100TireLeFocus() {
        var reponse = FateQuestion.ask(Odds.LIKELY, CHAOS_5, SANS_TABLES, Dice.scripted(33, 25));

        assertEquals(Answer.YES, reponse.answer());
        assertEquals(Optional.of(new RandomEvent(EventFocus.NPC_ACTION, 25, Optional.empty(), PERSONNAGES_VIDE)), reponse.randomEvent());
    }

    @Test
    void unDoubleAuChaos_declencheUnEvenement() {
        var reponse = FateQuestion.ask(Odds.LIKELY, CHAOS_5, SANS_TABLES, Dice.scripted(55, 1));

        assertEquals(Optional.of(new RandomEvent(EventFocus.REMOTE_EVENT, 1, Optional.empty())), reponse.randomEvent());
    }

    @Test
    void unDoubleAuDessusDuChaos_neDeclencheRien() {
        var reponse = FateQuestion.ask(Odds.LIKELY, CHAOS_5, SANS_TABLES, Dice.scripted(66));

        assertEquals(Optional.empty(), reponse.randomEvent());
    }

    @Test
    void cent_estLeDouble00_etDeclencheToujours_memeAuChaos1() {
        var reponse = FateQuestion.ask(Odds.FIFTY_FIFTY, ChaosFactor.of(1), SANS_TABLES, Dice.scripted(100, 90));

        assertEquals(Answer.EXCEPTIONAL_NO, reponse.answer());
        assertEquals(Optional.of(new RandomEvent(EventFocus.CURRENT_CONTEXT, 90, Optional.empty())), reponse.randomEvent());
    }

    @Test
    void unJetQuiNEstPasUnDouble_neDeclencheRien() {
        assertEquals(false, FateQuestion.triggersRandomEvent(10, ChaosFactor.of(9)));
        assertEquals(false, FateQuestion.triggersRandomEvent(1, ChaosFactor.of(9)));
        assertEquals(false, FateQuestion.triggersRandomEvent(99, ChaosFactor.of(8)));
        assertEquals(true, FateQuestion.triggersRandomEvent(99, ChaosFactor.of(9)));
        assertEquals(true, FateQuestion.triggersRandomEvent(11, ChaosFactor.of(1)));
    }

    @Test
    void avecLesTables_unEvenementTireSonSensDansLesActions() {
        var reponse = FateQuestion.ask(Odds.LIKELY, CHAOS_5, Optional.of(TablesDeTest.tables()), Dice.scripted(33, 25, 4, 90));

        assertEquals(Optional.of(new RandomEvent(EventFocus.NPC_ACTION, 25,
                Optional.of(new Meaning("Actions", "action1-004", 4, "action2-090", 90)), PERSONNAGES_VIDE)), reponse.randomEvent());
    }

    @Test
    void sansTables_unEvenementNaPasDeSens_etNeLancePasDeDeDePlus() {
        var reponse = FateQuestion.ask(Odds.LIKELY, CHAOS_5, SANS_TABLES, Dice.scripted(33, 25));

        assertEquals(Optional.of(new RandomEvent(EventFocus.NPC_ACTION, 25, Optional.empty(), PERSONNAGES_VIDE)), reponse.randomEvent());
    }

    @Test
    void avecLesTables_sansEvenement_aucunSensNEstTire() {
        var reponse = FateQuestion.ask(Odds.LIKELY, CHAOS_5, Optional.of(TablesDeTest.tables()), Dice.scripted(42));

        assertEquals(Optional.empty(), reponse.randomEvent());
    }

    private static final Map<AdventureList.Kind, AdventureList> LISTES = Map.of(
            AdventureList.Kind.CHARACTERS, new AdventureList(AdventureList.Kind.CHARACTERS, List.of(true, true)),
            AdventureList.Kind.THREADS, new AdventureList(AdventureList.Kind.THREADS, List.of(true, true, true, true, true, true)));

    @Test
    void unFocusDePnj_tireDansPersonnages_apresLesDesDuSens() {
        var reponse = FateQuestion.ask(Odds.LIKELY, CHAOS_5, Optional.of(TablesDeTest.tables()), LISTES,
                Dice.scripted(33, 25, 4, 90, 3));

        var evenement = reponse.randomEvent().orElseThrow();
        assertEquals(Optional.of(new ListDraw.Line(AdventureList.Kind.CHARACTERS, 2, null, null, 3, false)), evenement.draw());
        assertEquals(EventFocus.NPC_ACTION, evenement.effectiveFocus());
    }

    @Test
    void unFocusDeFil_tireDansFils() {
        var reponse = FateQuestion.ask(Odds.LIKELY, CHAOS_5, SANS_TABLES, LISTES, Dice.scripted(33, 60, 3, 1));

        assertEquals(Optional.of(new ListDraw.Line(AdventureList.Kind.THREADS, 6, 4, 3, 1, false)),
                reponse.randomEvent().orElseThrow().draw());
    }

    @Test
    void uneListeViseeAbsente_estVide_etLeFocusDevientContexteActuel_sansDeDePlus() {
        var reponse = FateQuestion.ask(Odds.LIKELY, CHAOS_5, SANS_TABLES, Map.of(), Dice.scripted(33, 43));

        var evenement = reponse.randomEvent().orElseThrow();
        assertEquals(EventFocus.NPC_NEGATIVE, evenement.focus());
        assertEquals(Optional.of(new ListDraw.Empty(AdventureList.Kind.CHARACTERS)), evenement.draw());
        assertEquals(EventFocus.CURRENT_CONTEXT, evenement.effectiveFocus());
    }

    @Test
    void unFocusSansListe_neTireRien_etNeLancePasDeDeDePlus() {
        for (int focus : new int[]{1, 6, 11, 71, 81, 86}) {
            var evenement = FateQuestion.ask(Odds.LIKELY, CHAOS_5, SANS_TABLES, LISTES, Dice.scripted(33, focus))
                    .randomEvent().orElseThrow();
            assertEquals(Optional.empty(), evenement.draw(), evenement.focus().name());
            assertEquals(evenement.focus(), evenement.effectiveFocus());
        }
    }
}
