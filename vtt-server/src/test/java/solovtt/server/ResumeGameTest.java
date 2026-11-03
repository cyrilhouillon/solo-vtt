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
import static org.junit.jupiter.api.Assertions.assertNull;
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

        assertEquals(List.of(new SavedCommand.Create(10, 10, DistanceUnit.FOOT, null, null, List.of())), store.game().commands());
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
                new SavedCommand.Create(30, 20, DistanceUnit.METRE, null, null, List.of()),
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
                new SavedCommand.Create(30, 20, DistanceUnit.METRE, null, null, List.of()),
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
    void caler_metLaCarteALEchelle_etSeReprendALIdentique() {
        var session = new SceneSession(Dice.scripted(), store);
        session.create(30, 20, DistanceUnit.METRE, "a".repeat(64));
        session.place(entree("loup"), 3, 4.5);

        session.alignGrid(33, 22, 0.25, 0.5);

        Scene calee = session.scene().orElseThrow();
        assertEquals(33, calee.width(), 1e-9);
        assertEquals(22, calee.height(), 1e-9);
        assertEquals(3.3, calee.token("loup-1").position().x(), 1e-9);
        assertEquals(4.95, calee.token("loup-1").position().y(), 1e-9);
        assertEquals(0.25, session.gridOffsetX());
        assertEquals(0.5, session.gridOffsetY());

        SceneSession apres = new SceneSession(Dice.scripted(), store);
        assertMemeScene(calee, apres.scene().orElseThrow());
        assertEquals(0.25, apres.gridOffsetX());
        assertEquals(0.5, apres.gridOffsetY());
        assertEquals(0, store.setAsides);
    }

    @Test
    void caler_estRefuseEnCombat_pendantUneDecision_etSansImage_sansEtreGarde() {
        var session = new SceneSession(Dice.scripted(20, 15), store);
        session.create(30, 20, DistanceUnit.METRE, "a".repeat(64));
        session.place(entree("guerriere"), 1.5, 1.5);
        session.place(entree("gobelin"), 12, 1.5);
        session.startCombat();
        SavedGame gardee = store.game();

        assertThrows(ActionRefusedException.class, () -> session.alignGrid(33, 22, 0, 0));
        assertEquals(30, session.scene().orElseThrow().width());
        assertEquals(gardee, store.game());

        session.endCombat();
        session.alignGrid(33, 22, 0, 0);
        assertEquals(33, session.scene().orElseThrow().width(), 1e-9);

        assertThrows(DecisionPendingException.class, () -> partieJusquABouclier().alignGrid(33, 22, 0, 0));

        var sansImage = new SceneSession(Dice.scripted());
        sansImage.create(30, 20, DistanceUnit.METRE, null);
        assertThrows(ActionRefusedException.class, () -> sansImage.alignGrid(33, 22, 0, 0));
    }

    @Test
    void caler_refuseDesCasesNonCarreesOuUnDecalageHorsDeLaCase() {
        var session = new SceneSession(Dice.scripted());
        session.create(30, 20, DistanceUnit.METRE, "a".repeat(64));

        assertThrows(IllegalArgumentException.class, () -> session.alignGrid(33, 20, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> session.alignGrid(33, 22, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> session.alignGrid(33, 22, 0, -0.1));
        assertThrows(IllegalArgumentException.class, () -> session.alignGrid(0, 0, 0, 0));
        assertEquals(30, session.scene().orElseThrow().width());
    }

    @Test
    void uneImageDisparueDuStockage_neMetPasLaPartieDeCote() {
        // La session ne lit jamais l'image : seule la création en direct (SceneResource) refuse une image inconnue.
        var gardee = new InMemoryGameStore(new SavedGame(List.of(
                new SavedCommand.Create(45, 45, DistanceUnit.METRE, "b".repeat(64), null, List.of()))));

        SceneSession session = new SceneSession(Dice.scripted(), gardee);

        assertEquals("b".repeat(64), session.imageId());
        assertTrue(session.gridVisible());
        assertEquals(0, gardee.setAsides);
    }

    /** Crypte : 30 × 20 m sur image, grille masquée, un Gobelin et deux Loups. */
    private static final SavedMap CRYPTE = new SavedMap("Crypte", 30, 20, DistanceUnit.METRE, "a".repeat(64), false, List.of(
            new SavedMap.SavedToken("gobelin", 12, 1.5),
            new SavedMap.SavedToken("loup", 25, 15),
            new SavedMap.SavedToken("loup", 20.4, 3.7)));

    @Test
    void laCarteEnCours_devientUnModele_sansToucherLaPartie() {
        var session = new SceneSession(Dice.scripted(20, 15, 2, 10), store);
        session.create(30, 20, DistanceUnit.METRE, "a".repeat(64));
        session.place(entree("gobelin"), 12, 1.5);
        session.place(entree("loup"), 25, 15);
        session.place(entree("loup"), 3, 3);
        session.remove("loup-2");
        session.place(entree("loup"), 20.4, 3.7);
        session.showGrid(false);
        session.startCombat();
        int saves = store.saves;

        assertEquals(CRYPTE, session.snapshot("Crypte"));
        assertEquals(saves, store.saves);
    }

    @Test
    void charger_donneDesCreaturesNeuves_sansCombatNiJournal_etSeReprendAvecSonNom() {
        var session = new SceneSession(Dice.scripted(20, 15, 10, 4), store);
        session.create(10, 10, DistanceUnit.FOOT, null);
        session.place(entree("guerriere"), 1.5, 1.5);
        session.place(entree("gobelin"), 3, 1.5);
        session.startCombat();

        session.load(CRYPTE);

        assertEquals("Crypte", session.mapName());
        assertEquals("a".repeat(64), session.imageId());
        assertFalse(session.gridVisible());
        assertTrue(session.combat().isEmpty());
        assertTrue(session.journal().isEmpty());
        assertEquals(List.of("gobelin-1@Position[x=12.0, y=1.5]", "loup-1@Position[x=25.0, y=15.0]", "loup-2@Position[x=20.4, y=3.7]"),
                session.scene().orElseThrow().tokens().stream().map(t -> t.id() + "@" + t.position()).toList());
        for (Token token : session.scene().orElseThrow().tokens()) {
            var creature = session.creature(token.entityId()).orElseThrow();
            assertEquals(creature.maxHitPoints(), creature.hitPoints());
        }
        assertEquals(new SavedCommand.Create(30, 20, DistanceUnit.METRE, "a".repeat(64), "Crypte", List.of()), store.game().commands().getFirst());

        SceneSession apres = new SceneSession(Dice.scripted(), store);
        assertEquals("Crypte", apres.mapName());
        assertFalse(apres.gridVisible());
        assertMemeScene(session.scene().orElseThrow(), apres.scene().orElseThrow());
    }

    @Test
    void chargerPendantUneDecision_laJette() {
        SceneSession session = partieJusquABouclier();

        session.load(CRYPTE);

        assertTrue(session.pendingDecision().isEmpty());
        assertEquals(3, session.scene().orElseThrow().tokens().size());
    }

    @Test
    void uneNouvelleCarteApresUnChargement_n_aPlusDeNom() {
        var session = new SceneSession(Dice.scripted(), store);
        session.load(CRYPTE);

        session.create(10, 10, DistanceUnit.FOOT, null);

        assertNull(session.mapName());
    }

    @Test
    void uneCreatureSortieDuCatalogue_refuseLeChargement_etLaCarteEnCoursReste() {
        var session = new SceneSession(Dice.scripted(), store);
        session.create(10, 10, DistanceUnit.FOOT, null);
        session.place(entree("gobelin"), 3, 1.5);
        SavedGame gardee = store.game();
        var perimee = new SavedMap("Marais", 30, 20, DistanceUnit.METRE, null, true, List.of(
                new SavedMap.SavedToken("gobelin", 12, 1.5), new SavedMap.SavedToken("hydre", 5, 5)));

        var refus = assertThrows(ActionRefusedException.class, () -> session.load(perimee));

        assertTrue(refus.getMessage().contains("hydre"), refus.getMessage());
        assertEquals(10, session.scene().orElseThrow().width());
        assertEquals(1, session.scene().orElseThrow().tokens().size());
        assertEquals(gardee, store.game());
    }

    private static void assertMemeScene(Scene attendue, Scene reprise) {
        assertEquals(attendue.width(), reprise.width());
        assertEquals(attendue.height(), reprise.height());
        assertEquals(attendue.unit(), reprise.unit());
        assertEquals(attendue.tokens().stream().map(t -> t.id() + "@" + t.position()).toList(),
                reprise.tokens().stream().map(t -> t.id() + "@" + t.position()).toList());
    }
}
