package solovtt.server;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CampaignModulesAtStartupTest {

    /** Les modules installés à ce démarrage, sans Quarkus. */
    private static InstalledModules installes(String... ids) {
        return new InstalledModules(List.of(ids));
    }

    @Test
    void uneCampagneSansChoix_recoitLesModulesInstalles_lesAutresNeChangentPas() {
        var store = CampaignStore.inMemory();
        var ancienne = store.create("D'avant");
        var choisie = store.create("Choisie", List.of());

        assertEquals(1, new CampaignModulesAtStartup(store, installes("mythic", "oracle")).fixUnchosen());
        assertEquals(List.of("mythic", "oracle"), store.find(ancienne.id()).orElseThrow().modules());
        assertEquals(List.of(), store.find(choisie.id()).orElseThrow().modules());

        // Un module installé plus tard ne s'active pas d'office.
        assertEquals(0, new CampaignModulesAtStartup(store, installes("mythic", "oracle", "nouveau")).fixUnchosen());
        assertEquals(List.of("mythic", "oracle"), store.find(ancienne.id()).orElseThrow().modules());
    }
}
