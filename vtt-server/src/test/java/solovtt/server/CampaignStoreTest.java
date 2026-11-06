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

    @Test
    void enMemoire_unCarnetParCampagne() {
        var store = CampaignStore.inMemory();
        var a = store.create("A");
        var b = store.create("B");
        var page = store.notebook(a.id()).create("Session 1", null);

        assertEquals(List.of(page), store.notebook(a.id()).list());
        assertEquals(List.of(), store.notebook(b.id()).list());
        assertThrows(IllegalArgumentException.class, () -> store.notebook("inconnue"));
    }

    @Test
    void enMemoire_lesDonneesDUnModule_parCampagne() {
        var store = CampaignStore.inMemory();
        var a = store.create("A");
        var b = store.create("B");
        assertEquals(Optional.empty(), store.readModuleData(a.id(), "essai"));

        store.writeModuleData(a.id(), "essai", "{\"chaos\":6}");
        store.writeModuleData(b.id(), "essai", "{\"chaos\":2}");

        assertEquals(Optional.of("{\"chaos\":6}"), store.readModuleData(a.id(), "essai"));
        assertEquals(Optional.of("{\"chaos\":2}"), store.readModuleData(b.id(), "essai"));
        assertThrows(IllegalArgumentException.class, () -> store.readModuleData("inconnue", "essai"));
        assertThrows(IllegalArgumentException.class, () -> store.writeModuleData(a.id(), "../x", "{}"));
        assertThrows(IllegalArgumentException.class, () -> store.writeModuleData(a.id(), "", "{}"));
    }

    @Test
    void adaptateur_exposeLesDonneesDesModulesDuCoeur() {
        var store = CampaignStore.inMemory();
        var a = store.create("A");
        var data = new ModuleDataAdapter(store);

        assertEquals(true, data.exists(a.id()));
        assertEquals(false, data.exists("inconnue"));
        data.write(a.id(), "essai", "{}");
        assertEquals(Optional.of("{}"), data.read(a.id(), "essai"));
    }
}
