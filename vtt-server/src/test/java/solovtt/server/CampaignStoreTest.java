package solovtt.server;

import org.junit.jupiter.api.Test;
import solovtt.scene.DistanceUnit;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CampaignStoreTest {

    private static final SavedGame PARTIE = new SavedGame(List.of(
            new SavedGame.SavedCommand.Create(30, 20, DistanceUnit.METRE, null, null, List.of())));
    private static final SavedMap CRYPTE = new SavedMap("Crypte", 30, 20, DistanceUnit.METRE, null, true, 0, 0, List.of());

    @Test
    void enMemoire_creeEtListeParNom_memeNomPermis() {
        var store = CampaignStore.inMemory();
        var strahd = store.create("Strahd");
        var phandelver = store.create("phandelver");
        var autreStrahd = store.create("Strahd");

        assertEquals(List.of("phandelver", "Strahd", "Strahd"), store.list().stream().map(Campaign::name).toList());
        assertNotEquals(strahd.id(), autreStrahd.id());
        assertEquals(Optional.of(phandelver), store.find(phandelver.id()));
        assertEquals(Optional.empty(), store.find("inconnue"));
    }

    @Test
    void enMemoire_partieEtCartesSeparees_parCampagne() {
        var store = CampaignStore.inMemory();
        var a = store.create("A");
        var b = store.create("B");
        store.games(a.id()).save(PARTIE);
        store.maps(a.id()).save(CRYPTE);

        assertEquals(Optional.of(PARTIE), store.games(a.id()).load());
        assertEquals(List.of(CRYPTE), store.maps(a.id()).list());
        assertEquals(Optional.empty(), store.games(b.id()).load());
        assertEquals(List.of(), store.maps(b.id()).list());
        assertThrows(IllegalArgumentException.class, () -> store.games("inconnue"));
    }
}
