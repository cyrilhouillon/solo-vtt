package solovtt.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

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

    @Test
    void unModuleRetireDeLAssemblage_nEstPlusActif_sansQueLaFicheChange() {
        var store = CampaignStore.inMemory();
        var campagne = store.create("Figée", List.of("mythic", "oracle"));

        assertEquals(List.of("mythic"), installes("mythic").activeIn(store.find(campagne.id()).orElseThrow()));
        assertEquals(List.of("mythic", "oracle"), store.find(campagne.id()).orElseThrow().modules());
        assertEquals(List.of("mythic"), installes("mythic").activeIn(new Campaign("x", "D'avant", null)));
    }

    @Test
    void uneErreurDuMagasin_nEmpechePasLeDemarrage() {
        CampaignStore memoire = CampaignStore.inMemory();
        memoire.create("D'avant");
        CampaignStore ecritureEnPanne = new CampaignStore() {
            @Override
            public List<Campaign> list() {
                return memoire.list();
            }

            @Override
            public Campaign create(String name, List<String> modules) {
                return memoire.create(name, modules);
            }

            @Override
            public Campaign setModules(String id, List<String> modules) {
                throw new UncheckedIOException(new IOException("disque plein"));
            }

            @Override
            public GameStore games(String id) {
                return memoire.games(id);
            }

            @Override
            public MapStore maps(String id) {
                return memoire.maps(id);
            }

            @Override
            public NotebookStore notebook(String id) {
                return memoire.notebook(id);
            }

            @Override
            public Optional<String> readModuleData(String id, String moduleId) {
                return memoire.readModuleData(id, moduleId);
            }

            @Override
            public void writeModuleData(String id, String moduleId, String json) {
                memoire.writeModuleData(id, moduleId, json);
            }
        };
        assertEquals(0, new CampaignModulesAtStartup(ecritureEnPanne, installes("mythic")).fixUnchosen());

        CampaignStore lectureEnPanne = new CampaignStore() {
            @Override
            public List<Campaign> list() {
                throw new UncheckedIOException(new IOException("dossier illisible"));
            }

            @Override
            public Campaign create(String name, List<String> modules) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Campaign setModules(String id, List<String> modules) {
                throw new UnsupportedOperationException();
            }

            @Override
            public GameStore games(String id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public MapStore maps(String id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public NotebookStore notebook(String id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<String> readModuleData(String id, String moduleId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void writeModuleData(String id, String moduleId, String json) {
                throw new UnsupportedOperationException();
            }
        };
        assertEquals(0, new CampaignModulesAtStartup(lectureEnPanne, installes("mythic")).fixUnchosen());
    }
}
