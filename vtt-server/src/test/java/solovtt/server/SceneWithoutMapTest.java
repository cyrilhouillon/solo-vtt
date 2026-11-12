package solovtt.server;

import jakarta.ws.rs.ClientErrorException;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;
import solovtt.scene.DistanceUnit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sans carte créée : la session de l'application en a toujours une dans {@link SceneApiTest}. */
class SceneWithoutMapTest {

    private final SceneSession session = new SceneSession(Dice.scripted());
    private final SceneResource resource = new SceneResource(session, ImageStore.inMemory(), TestCharacters.catalog());

    @Test
    void sansCarte_enregistrerLaCarteRend409() {
        var e = assertThrows(ClientErrorException.class,
                () -> new MapResource(session, TestCharacters.catalog()).save(new Dtos.MapNameRequest("Crypte")));
        assertEquals(409, e.getResponse().getStatus());
    }

    @Test
    void uneCarteDontUneCreatureAQuitteLeCatalogue_refuseLeChargement() {
        session.maps().save(new SavedMap("Marais", 30, 20, DistanceUnit.METRE, null, true, 0, 0, List.of(new SavedMap.SavedToken("hydre", 5, 5))));

        var e = assertThrows(ActionRefusedException.class, () -> new MapResource(session, TestCharacters.catalog()).load(new Dtos.MapNameRequest("Marais")));
        assertTrue(e.getMessage().contains("hydre"));
        assertTrue(session.scene().isEmpty());
    }

    @Test
    void sansCarte_calerLaGrilleRend409() {
        var e = assertThrows(ClientErrorException.class,
                () -> resource.alignGrid(new Dtos.AlignmentRequest(33.0, 22.0, 0.0, 0.0)));
        assertEquals(409, e.getResponse().getStatus());
    }

    @Test
    void sansCarte_attaquerRend409() {
        var e = assertThrows(ClientErrorException.class,
                () -> new AttackResource(session).attack(new Dtos.AttackRequest("loup-1", "melee:Morsure", "loup-2")));
        assertEquals(409, e.getResponse().getStatus());
    }

    @Test
    void sansCarte_terminerLeCombatRend409() {
        var e = assertThrows(ClientErrorException.class, () -> new CombatResource(session).end());
        assertEquals(409, e.getResponse().getStatus());
    }

    @Test
    void sansCarte_leJournalEstVide() {
        assertTrue(new JournalResource(session).journal().isEmpty());
    }

    @Test
    void sansCarte_poserUnJetonRend409() {
        var e = assertThrows(ClientErrorException.class,
                () -> resource.place(new Dtos.PlaceTokenRequest("loup", 1, 1)));
        assertEquals(409, e.getResponse().getStatus());
        assertTrue(e.getMessage().contains("Aucune carte"));
    }

    @Test
    void sansCarte_lireLaSceneRend409() {
        var e = assertThrows(ClientErrorException.class, resource::get);
        assertEquals(409, e.getResponse().getStatus());
    }

    @Test
    void sansCarte_basculerLaGrilleRend409() {
        var e = assertThrows(ClientErrorException.class, () -> resource.showGrid(new Dtos.GridRequest(false)));
        assertEquals(409, e.getResponse().getStatus());
    }
}
