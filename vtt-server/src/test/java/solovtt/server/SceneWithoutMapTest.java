package solovtt.server;

import jakarta.ws.rs.ClientErrorException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sans carte créée : la session de l'application en a toujours une dans {@link SceneApiTest}. */
class SceneWithoutMapTest {

    private final SceneResource resource = new SceneResource(new SceneSession());

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
}
