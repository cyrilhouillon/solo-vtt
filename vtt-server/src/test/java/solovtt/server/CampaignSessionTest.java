package solovtt.server;

import dnd.engine.state.Combat;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;
import solovtt.scene.DistanceUnit;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La session de l'application et ses campagnes. Campagne A : Gobelin (12 ; 1,5), Magicienne au contact (12 ; 3) ;
 * initiative Gobelin 15, Magicienne 2 ; un d20 de 10 au Cimeterre touche la Magicienne, et Bouclier est proposé.
 */
class CampaignSessionTest {

    private final CampaignStore campaigns = CampaignStore.inMemory();

    private static dnd.engine.catalog.CatalogEntry entree(String id) {
        return SceneSession.catalogEntry(id).orElseThrow();
    }

    @Test
    void sansCampagneOuverte_creerOuChargerUneCarte_estRefuse() {
        var session = new SceneSession(Dice.scripted(), campaigns);

        assertEquals(Optional.empty(), session.campaign());
        assertThrows(NoCampaignException.class, () -> session.create(30, 20, DistanceUnit.METRE, null));
        assertThrows(NoCampaignException.class, session::maps);
        var carte = new SavedMap("Crypte", 30, 20, DistanceUnit.METRE, null, true, 0, 0, List.of());
        assertThrows(NoCampaignException.class, () -> session.load(carte));
        assertTrue(session.scene().isEmpty());
    }

    @Test
    void allerRetour_puisSessionNeuve_retrouventLaCampagneALIdentique() {
        Campaign a = campaigns.create("A");
        Campaign b = campaigns.create("B");
        var session = new SceneSession(Dice.scripted(15, 2, 10), campaigns);
        session.open(a);
        session.create(30, 20, DistanceUnit.METRE, null);
        session.place(entree("gobelin"), 12, 1.5);
        session.place(entree("magicienne"), 12, 3);
        session.startCombat();
        session.attack("gobelin-1", "melee:Cimeterre", "magicienne-1");
        var journal = session.journal();
        var combat = session.combat().map(Combat::signature);
        String decision = session.pendingDecision().orElseThrow().id();

        session.open(b);
        assertTrue(session.scene().isEmpty());
        assertTrue(session.pendingDecision().isEmpty());
        assertEquals(Optional.of(b), session.campaign());

        session.open(a);
        assertEquals(journal, session.journal());
        assertEquals(combat, session.combat().map(Combat::signature));
        assertEquals(decision, session.pendingDecision().orElseThrow().id());

        var redemarree = new SceneSession(Dice.scripted(), campaigns);
        redemarree.open(a);
        assertEquals(journal, redemarree.journal());
        assertEquals(decision, redemarree.pendingDecision().orElseThrow().id());
    }
}
