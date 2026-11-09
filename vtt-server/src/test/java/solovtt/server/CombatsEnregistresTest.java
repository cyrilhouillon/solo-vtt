package solovtt.server;

import dnd.engine.catalog.CatalogEntry;
import dnd.engine.state.Combat;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;
import solovtt.scene.DistanceUnit;
import solovtt.server.SavedGame.SavedCommand;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chaque combat est gardé à part, de la création de sa carte à sa fin, et se relit ou se reprend.
 * Carte de 30 × 20 m : Guerrière (1,5 ; 1,5) au contact du Gobelin (3 ; 1,5). Initiative : Guerrière 20, Gobelin 15.
 */
class CombatsEnregistresTest {

    private final InMemoryGameStore partie = new InMemoryGameStore();
    private final CombatStore combats = CombatStore.inMemory();
    private final List<String> lances = new ArrayList<>();

    private SceneSession session(int... des) {
        return new SceneSession(Dice.scripted(des), partie, MapStore.inMemory(), combats, lances::add);
    }

    private static CatalogEntry entree(String id) {
        return SceneSession.catalogEntry(id).orElseThrow();
    }

    private static void carte(SceneSession session) {
        session.create(30, 20, DistanceUnit.METRE, null);
        session.place(entree("guerriere"), 1.5, 1.5);
        session.place(entree("gobelin"), 3, 1.5);
    }

    private SavedGame combat(String id) {
        return combats.game(id).load().orElseThrow();
    }

    @Test
    void lancerUnCombat_leGardeAPartDepuisLaCreationDeLaCarte_etLAnnonce() {
        var session = session(20, 15);
        carte(session);

        session.startCombat();

        assertEquals(1, lances.size());
        String id = lances.getFirst();
        assertEquals(id, session.combatId().orElseThrow());
        var commandes = combat(id).commands();
        assertInstanceOf(SavedCommand.Create.class, commandes.getFirst());
        assertEquals(new SavedCommand.StartCombat(id, List.of(20, 15)), commandes.getLast());
        assertEquals(partie.game(), combat(id));
    }

    @Test
    void unCombatTermine_seRelitASonEtatFinal_etNeChangePlusEnsuite() {
        var session = session(20, 15, 18, 5);
        carte(session);
        session.startCombat();
        String id = lances.getFirst();
        session.attack("guerriere-1", "melee:Épée longue", "gobelin-1");
        session.endCombat();

        assertTrue(session.combatId().isEmpty());
        assertInstanceOf(SavedCommand.EndCombat.class, combat(id).commands().getLast());
        SavedGame fige = combat(id);
        session.move("gobelin-1", 10, 10);
        assertEquals(fige, combat(id));
        assertEquals(10, session.scene().orElseThrow().token("gobelin-1").position().x());

        SceneSession relu = SceneSession.replayed(fige);
        assertTrue(relu.combatId().isEmpty());
        assertTrue(relu.combat().isEmpty());
        assertEquals(session.creature("gobelin-1").orElseThrow().signature(), relu.creature("gobelin-1").orElseThrow().signature());
        assertEquals(3, relu.scene().orElseThrow().token("gobelin-1").position().x());
    }

    @Test
    void uneNouvelleCarte_ouUneCarteChargee_nEcrasentPasLeCombatEnCours() {
        var session = session(20, 15);
        carte(session);
        session.startCombat();
        String id = lances.getFirst();
        SavedGame enCours = combat(id);

        session.create(10, 10, DistanceUnit.FOOT, null);
        session.place(entree("loup"), 2, 2);
        session.load(new SavedMap("Crypte", 30, 20, DistanceUnit.METRE, null, true, 0, 0, List.of()));

        assertEquals(enCours, combat(id));
        assertTrue(session.combatId().isEmpty());
        SceneSession relu = SceneSession.replayed(combat(id));
        assertEquals(id, relu.combatId().orElseThrow());
        assertTrue(relu.combat().isPresent());
    }

    @Test
    void unLancementRefuse_neChangePasLeCombatEnCours() {
        var session = session(20, 15);
        carte(session);
        session.startCombat();
        String id = lances.getFirst();

        assertThrows(ActionRefusedException.class, session::startCombat);

        assertEquals(id, session.combatId().orElseThrow());
        assertEquals(1, combats.ids().size());
        session.move("gobelin-1", 3, 3);
        assertInstanceOf(SavedCommand.Move.class, combat(id).commands().getLast());
    }

    @Test
    void unSecondCombatSurLaMemeCarte_estUnAutreCombat() {
        var session = session(20, 15, 20, 15);
        carte(session);
        session.startCombat();
        session.endCombat();
        SavedGame premier = combat(lances.getFirst());

        session.startCombat();

        assertEquals(2, lances.size());
        assertEquals(premier, combat(lances.get(0)));
        var second = combat(lances.get(1)).commands();
        assertInstanceOf(SavedCommand.Create.class, second.getFirst());
        assertEquals(premier.commands(), second.subList(0, premier.commands().size()));
    }

    @Test
    void rouvrirLaSession_retrouveLeCombatEnCours_sansLAnnoncerDeNouveau() {
        var session = session(20, 15);
        carte(session);
        session.startCombat();
        String id = lances.getFirst();

        SceneSession apres = session();

        assertEquals(id, apres.combatId().orElseThrow());
        assertEquals(1, lances.size());
        apres.endCombat();
        assertInstanceOf(SavedCommand.EndCombat.class, combat(id).commands().getLast());
    }

    @Test
    void reprendreUnCombatEnCours_remplaceLaPartie_laOuIlEnEtait() {
        var session = session(20, 15);
        carte(session);
        session.startCombat();
        String id = lances.getFirst();
        Combat avant = session.combat().orElseThrow();
        session.create(10, 10, DistanceUnit.FOOT, null);

        session.resume(combat(id));

        assertEquals(id, session.combatId().orElseThrow());
        assertEquals(avant.signature(), session.combat().orElseThrow().signature());
        assertEquals(combat(id), partie.game());
        session.endCombat();
        assertInstanceOf(SavedCommand.EndCombat.class, combat(id).commands().getLast());
        assertEquals(1, lances.size());
    }

    @Test
    void reprendreUnePartieIllisible_laissePartieEnCoursIntacte() {
        var session = session(20, 15);
        carte(session);
        SavedGame avant = partie.game();
        var illisible = new SavedGame(List.of(
                new SavedCommand.Create(30, 20, DistanceUnit.METRE, null, null, List.of()),
                new SavedCommand.StartCombat(List.of(15, 7))));

        assertThrows(RuntimeException.class, () -> session.resume(illisible));

        assertEquals(avant, partie.game());
        assertEquals(2, session.scene().orElseThrow().tokens().size());
    }

    @Test
    void unePartieDAvant_sansIdDeCombat_seReprendSansCombatGarde() {
        var avant = new InMemoryGameStore(new SavedGame(List.of(
                new SavedCommand.Create(30, 20, DistanceUnit.METRE, null, null, List.of()),
                new SavedCommand.Place("guerriere", 1.5, 1.5, List.of()),
                new SavedCommand.Place("gobelin", 3, 1.5, List.of()),
                new SavedCommand.StartCombat(List.of(20, 15)))));

        var session = new SceneSession(Dice.scripted(), avant, MapStore.inMemory(), combats, lances::add);

        assertTrue(session.combat().isPresent());
        assertTrue(session.combatId().isEmpty());
        assertEquals(0, avant.setAsides);
        session.endTurn();
        assertTrue(combats.ids().isEmpty());
        assertTrue(lances.isEmpty());
        assertNull(((SavedCommand.StartCombat) avant.game().commands().get(3)).combatId());
        assertFalse(avant.game().commands().isEmpty());
    }
}
