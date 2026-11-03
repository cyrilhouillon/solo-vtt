package solovtt.server;

import jakarta.ws.rs.ClientErrorException;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sans carte créée : la session de l'application en a toujours une dans {@link SceneApiTest}. */
class SceneWithoutMapTest {

    private final SceneSession session = new SceneSession(Dice.scripted());
    private final SceneResource resource = new SceneResource(session, ImageStore.inMemory());

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
