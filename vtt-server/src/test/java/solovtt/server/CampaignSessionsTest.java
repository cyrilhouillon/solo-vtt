package solovtt.server;

import dnd.engine.state.Combat;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;
import solovtt.scene.DistanceUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La partie de chaque campagne. Campagne A : Gobelin (12 ; 1,5), Magicienne au contact (12 ; 3) ;
 * initiative Gobelin 15, Magicienne 2 ; un d20 de 10 au Cimeterre touche la Magicienne, et Bouclier est proposé.
 */
class CampaignSessionsTest {

    private final CampaignStore campaigns = CampaignStore.inMemory();

    private static dnd.engine.catalog.CatalogEntry entree(String id) {
        return SceneSession.catalogEntry(id).orElseThrow();
    }

    @Test
    void uneCampagne_garde_saPartie_dUneDemandeALAutre() {
        Campaign a = campaigns.create("A");
        Campaign b = campaigns.create("B");
        var sessions = new CampaignSessions(Dice.scripted(), campaigns);

        assertSame(sessions.of(a.id()), sessions.of(a.id()));
        assertNotSame(sessions.of(a.id()), sessions.of(b.id()));
    }

    @Test
    void uneCampagneInconnue_estIntrouvable() {
        var sessions = new CampaignSessions(Dice.scripted(), campaigns);

        assertThrows(NotFoundException.class, () -> sessions.of("inconnue"));
    }

    @Test
    void apresRedemarrage_laCampagneRevientALIdentique() {
        Campaign a = campaigns.create("A");
        Campaign b = campaigns.create("B");
        var session = new CampaignSessions(Dice.scripted(15, 2, 10), campaigns).of(a.id());
        session.create(30, 20, DistanceUnit.METRE, null);
        session.place(entree("gobelin"), 12, 1.5);
        session.place(entree("magicienne"), 12, 3);
        session.startCombat();
        session.attack("gobelin-1", "melee:Cimeterre", "magicienne-1");

        var redemarre = new CampaignSessions(Dice.scripted(), campaigns);
        var reprise = redemarre.of(a.id());

        assertEquals(session.journal(), reprise.journal());
        assertEquals(session.combat().map(Combat::signature), reprise.combat().map(Combat::signature));
        assertEquals(session.pendingDecision().orElseThrow().id(), reprise.pendingDecision().orElseThrow().id());
        assertTrue(redemarre.of(b.id()).scene().isEmpty());
    }
}
