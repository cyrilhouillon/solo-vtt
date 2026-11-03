package solovtt.server;

import dnd.engine.state.Combat;
import org.junit.jupiter.api.Test;
import solovtt.engine.Decision;
import solovtt.engine.Dice;
import solovtt.scene.DistanceUnit;
import solovtt.scene.Scene;
import solovtt.scene.Token;
import solovtt.server.SavedGame.SavedCommand;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fermer puis rouvrir : une session neuve sur la partie gardée la retrouve à l'identique.
 * Carte de 30 × 20 m : Guerrière (1,5 ; 1,5), Gobelin (12 ; 1,5), Magicienne au contact du Gobelin (12 ; 3).
 * Initiative : Guerrière 20, Gobelin 15, Magicienne 2, puis un Loup posé en combat tire 1.
 * Le d20 de 10 du Gobelin donne 14 au Cimeterre : touche la CA 12 de la Magicienne, que Bouclier ferait rater.
 */
class ResumeGameTest {

    private final InMemoryGameStore store = new InMemoryGameStore();

    /** Joue jusqu'à la décision Bouclier, en passant par chaque sorte de commande sauf la fin du combat et la réponse. */
    private SceneSession partieJusquABouclier() {
        var session = new SceneSession(Dice.scripted(20, 15, 2, 1, 10), store);
        session.create(30, 20, DistanceUnit.METRE, null);
        session.place(entree("guerriere"), 1.5, 1.5);
        session.place(entree("gobelin"), 12, 1.5);
        session.place(entree("magicienne"), 12, 3);
        session.place(entree("loup"), 25, 15);
        session.move("guerriere-1", 3, 1.5);
        session.remove("loup-1");
        session.startCombat();
        session.place(entree("loup"), 25, 15);
        session.move("guerriere-1", 6, 1.5);
        session.endTurn();
        var outcome = session.attack("gobelin-1", "melee:Cimeterre", "magicienne-1");
        assertTrue(outcome.decision().isPresent());
        return session;
    }

    private static dnd.engine.catalog.CatalogEntry entree(String id) {
        return SceneSession.catalogEntry(id).orElseThrow();
    }

    @Test
    void rouvrirPendantUneDecision_redonneLaPartieALIdentique() {
        SceneSession avant = partieJusquABouclier();

        SceneSession apres = new SceneSession(Dice.scripted(), store);

        assertMemeScene(avant.scene().orElseThrow(), apres.scene().orElseThrow());
        assertEquals(avant.journal(), apres.journal());
        assertEquals(avant.combat().map(Combat::signature), apres.combat().map(Combat::signature));
        assertEquals(avant.movementLeft(), apres.movementLeft());
        for (Token token : avant.scene().orElseThrow().tokens()) {
            assertEquals(avant.creature(token.entityId()).orElseThrow().signature(),
                    apres.creature(token.entityId()).orElseThrow().signature());
        }
        Decision attendue = avant.pendingDecision().orElseThrow();
        Decision reprise = apres.pendingDecision().orElseThrow();
        assertEquals(List.of(attendue.id(), attendue.chooserId(), attendue.prompt(), attendue.options()),
                List.of(reprise.id(), reprise.chooserId(), reprise.prompt(), reprise.options()));
        assertEquals(0, store.setAsides);
    }

    @Test
    void apresReprise_repondreReprendLAttaque_etLaPartieContinueDEtreGardee() {
        SceneSession avant = partieJusquABouclier();
        String id = avant.pendingDecision().orElseThrow().id();

        SceneSession apres = new SceneSession(Dice.scripted(3), store);
        var outcome = apres.answer(id, "Ne rien faire").orElseThrow();

        assertTrue(outcome.entries().stream().anyMatch(e -> e.text().equals("Magicienne subit 5 dégâts tranchants (PV 2/7)")));
        assertEquals(2, apres.creature("magicienne-1").orElseThrow().hitPoints());
        assertInstanceOf(SavedCommand.Answer.class, store.game().commands().getLast());
        assertEquals(List.of(3), store.game().commands().getLast().rolls());

        SceneSession encoreApres = new SceneSession(Dice.scripted(), store);
        assertEquals(apres.journal(), encoreApres.journal());
        assertTrue(encoreApres.pendingDecision().isEmpty());
    }

    @Test
    void lesDesDeLInitiativeDUneCreaturePoseeEnCombat_vontAvecSaPose() {
        partieJusquABouclier();

        var poses = store.game().commands().stream().filter(SavedCommand.Place.class::isInstance).toList();
        assertEquals(List.of(), poses.get(3).rolls());
        assertEquals(List.of(1), poses.get(4).rolls());
        var lancement = store.game().commands().stream().filter(SavedCommand.StartCombat.class::isInstance).findFirst().orElseThrow();
        assertEquals(List.of(20, 15, 2), lancement.rolls());
    }

    @Test
    void uneCommandeRefusee_nEstPasGardee() {
        SceneSession session = partieJusquABouclier();
        SavedGame gardee = store.game();
        int saves = store.saves;
        String id = session.pendingDecision().orElseThrow().id();

        assertThrows(DecisionPendingException.class, () -> session.attack("gobelin-1", "melee:Cimeterre", "magicienne-1"));
        assertThrows(IllegalArgumentException.class, () -> session.answer(id, "Fuir"));
        assertTrue(session.answer("d999", "Ne rien faire").isEmpty());

        assertEquals(saves, store.saves);
        assertEquals(gardee, store.game());
    }

    @Test
    void uneNouvelleCarte_remplaceLaPartieGardee() {
        SceneSession session = partieJusquABouclier();

        session.create(10, 10, DistanceUnit.FOOT, null);

        assertEquals(List.of(new SavedCommand.Create(10, 10, DistanceUnit.FOOT, null, List.of())), store.game().commands());
    }

    @Test
    void sansPartieGardee_laSessionDemarreSansCarte() {
        SceneSession session = new SceneSession(Dice.scripted(), store);

        assertTrue(session.scene().isEmpty());
        assertEquals(0, store.saves);
    }

    @Test
    void unePartieQuiNeSeRejouePlus_estMiseDeCote_etLaSessionDemarreSansCarte() {
        var divergente = new InMemoryGameStore(new SavedGame(List.of(
                new SavedCommand.Create(30, 20, DistanceUnit.METRE, null, List.of()),
                new SavedCommand.Place("gobelin", 12, 1.5, List.of()),
                new SavedCommand.StartCombat(List.of(15, 7)))));

        SceneSession session = new SceneSession(Dice.scripted(), divergente);

        assertTrue(session.scene().isEmpty());
        assertTrue(session.journal().isEmpty());
        assertEquals(1, divergente.setAsides);
        assertEquals(0, divergente.saves);
    }

    @Test
    void uneReponseSansDecisionAuRejeu_estUnEchec() {
        var divergente = new InMemoryGameStore(new SavedGame(List.of(
                new SavedCommand.Create(30, 20, DistanceUnit.METRE, null, List.of()),
                new SavedCommand.Answer("d1", "Ne rien faire", List.of()))));

        SceneSession session = new SceneSession(Dice.scripted(), divergente);

        assertTrue(session.scene().isEmpty());
        assertEquals(1, divergente.setAsides);
    }

    @Test
    void unePartieIllisible_estMiseDeCote() {
        var illisible = InMemoryGameStore.unreadable();

        SceneSession session = new SceneSession(Dice.scripted(), illisible);

        assertTrue(session.scene().isEmpty());
        assertEquals(1, illisible.setAsides);
    }

    @Test
    void uneSessionSansStockage_neGardeRien() {
        var session = new SceneSession(Dice.scripted());
        session.create(30, 20, DistanceUnit.METRE, null);
        assertFalse(session.scene().isEmpty());
    }

    @Test
    void rouvrir_redonneLImageEtLeChoixDeGrille() {
        var session = new SceneSession(Dice.scripted(20, 15), store);
        session.create(45, 45, DistanceUnit.METRE, "a".repeat(64));
        session.place(entree("guerriere"), 1.5, 1.5);
        session.place(entree("gobelin"), 12, 1.5);
        session.showGrid(false);
        session.startCombat();

        SceneSession apres = new SceneSession(Dice.scripted(), store);

        assertEquals("a".repeat(64), apres.imageId());
        assertFalse(apres.gridVisible());
        assertMemeScene(session.scene().orElseThrow(), apres.scene().orElseThrow());
        assertEquals(new SavedCommand.ShowGrid(false, List.of()), store.game().commands().get(3));
        assertEquals(0, store.setAsides);
    }

    @Test
    void uneImageDisparueDuStockage_neMetPasLaPartieDeCote() {
        // La session ne lit jamais l'image : seule la création en direct (SceneResource) refuse une image inconnue.
        var gardee = new InMemoryGameStore(new SavedGame(List.of(
                new SavedCommand.Create(45, 45, DistanceUnit.METRE, "b".repeat(64), List.of()))));

        SceneSession session = new SceneSession(Dice.scripted(), gardee);

        assertEquals("b".repeat(64), session.imageId());
        assertTrue(session.gridVisible());
        assertEquals(0, gardee.setAsides);
    }

    private static void assertMemeScene(Scene attendue, Scene reprise) {
        assertEquals(attendue.width(), reprise.width());
        assertEquals(attendue.height(), reprise.height());
        assertEquals(attendue.unit(), reprise.unit());
        assertEquals(attendue.tokens().stream().map(t -> t.id() + "@" + t.position()).toList(),
                reprise.tokens().stream().map(t -> t.id() + "@" + t.position()).toList());
    }
}
