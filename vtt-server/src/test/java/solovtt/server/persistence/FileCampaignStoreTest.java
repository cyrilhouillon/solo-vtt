package solovtt.server.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import solovtt.scene.DistanceUnit;
import solovtt.server.Campaign;
import solovtt.server.SavedGame;
import solovtt.server.SavedMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileCampaignStoreTest {

    @TempDir
    Path dir;

    private static final SavedGame PARTIE = new SavedGame(List.of(
            new SavedGame.SavedCommand.Create(30, 20, DistanceUnit.METRE, null, null, List.of())));
    private static final SavedMap CRYPTE = new SavedMap("Crypte", 30, 20, DistanceUnit.METRE, null, true, 0, 0, List.of());

    @Test
    void sansDossier_aucuneCampagne() {
        var store = new FileCampaignStore(dir.resolve("campagnes"));
        assertEquals(List.of(), store.list());
        assertEquals(Optional.empty(), store.find("inconnue"));
    }

    @Test
    void deuxCampagnes_seRelisentParUnNouvelAdaptateur_avecLeurPartieEtLeursCartes() {
        Path root = dir.resolve("campagnes");
        var store = new FileCampaignStore(root);
        var strahd = store.create("Strahd");
        var phandelver = store.create("Phandelver");
        store.games(phandelver.id()).save(PARTIE);
        store.maps(phandelver.id()).save(CRYPTE);

        var relu = new FileCampaignStore(root);
        assertEquals(List.of(phandelver, strahd), relu.list());
        assertEquals(Optional.of(PARTIE), relu.games(phandelver.id()).load());
        assertEquals(List.of(CRYPTE), relu.maps(phandelver.id()).list());
        assertEquals(Optional.empty(), relu.games(strahd.id()).load());
        assertEquals(List.of(), relu.maps(strahd.id()).list());
        assertEquals(List.of(root.resolve(phandelver.id()).resolve("campagne.json"),
                        root.resolve(phandelver.id()).resolve("cartes.json"),
                        root.resolve(phandelver.id()).resolve("partie.json")),
                fichiers(root.resolve(phandelver.id())));
    }

    @Test
    void leCarnet_vitDansLeDossierDeSaCampagne_unParCampagne() {
        Path root = dir.resolve("campagnes");
        var store = new FileCampaignStore(root);
        var strahd = store.create("Strahd");
        var phandelver = store.create("Phandelver");
        var page = store.notebook(strahd.id()).create("Session 1", null);

        var relu = new FileCampaignStore(root);
        assertEquals(List.of(page), relu.notebook(strahd.id()).list());
        assertEquals(List.of(), relu.notebook(phandelver.id()).list());
        assertEquals(true, Files.exists(root.resolve(strahd.id()).resolve("carnet").resolve(page.id() + ".json")));
        assertThrows(IllegalArgumentException.class, () -> store.notebook("../" + strahd.id()));
    }

    @Test
    void lesModulesChoisis_seRelisent_uneFicheSansModules_seLitAvecTous() throws IOException {
        Path root = dir.resolve("campagnes");
        var store = new FileCampaignStore(root);
        var avec = store.create("Avec Mythic", List.of("mythic"));
        var sans = store.create("Sans module", List.of());
        var ancienne = store.create("D'avant");

        var relu = new FileCampaignStore(root);
        assertEquals(Optional.of(List.of("mythic")), relu.find(avec.id()).map(Campaign::modules));
        assertEquals(Optional.of(List.of()), relu.find(sans.id()).map(Campaign::modules));
        assertEquals(Optional.empty(), relu.find(ancienne.id()).map(Campaign::modules));
        assertEquals(false, Files.readString(root.resolve(ancienne.id()).resolve("campagne.json")).contains("modules"));

        Files.writeString(root.resolve(ancienne.id()).resolve("campagne.json"),
                "{\"id\":\"%s\",\"name\":\"D'avant\"}".formatted(ancienne.id()));
        assertEquals(Optional.of(new Campaign(ancienne.id(), "D'avant", null)), new FileCampaignStore(root).find(ancienne.id()));
    }

    @Test
    void changerLesModules_reecritLaFiche_sansToucherLesDonnees() {
        Path root = dir.resolve("campagnes");
        var store = new FileCampaignStore(root);
        var strahd = store.create("Strahd", List.of("mythic"));
        store.writeModuleData(strahd.id(), "mythic", "{\"chaos\":7}");

        assertEquals(new Campaign(strahd.id(), "Strahd", List.of()), store.setModules(strahd.id(), List.of()));
        var relu = new FileCampaignStore(root);
        assertEquals(Optional.of(List.of()), relu.find(strahd.id()).map(Campaign::modules));
        assertEquals(Optional.of("{\"chaos\":7}"), relu.readModuleData(strahd.id(), "mythic"));
        assertThrows(IllegalArgumentException.class, () -> store.setModules("inconnue", List.of()));
        // Un id qui n'en est pas un ne lit aucune fiche hors du dossier de la campagne.
        assertEquals(Optional.empty(), store.find("../" + strahd.id()));
        assertEquals(Optional.empty(), store.find(strahd.id() + "\\..\\" + strahd.id()));
        assertEquals(Optional.empty(), store.find(strahd.id().toUpperCase()));
        assertEquals(Optional.empty(), store.find(null));
    }

    @Test
    void memeNom_deuxCampagnesDistinctes() {
        var store = new FileCampaignStore(dir);
        store.create("Strahd");
        store.create("Strahd");
        assertEquals(2, store.list().size());
    }

    @Test
    void unDossierSansFicheOuIllisible_estIgnore_etJamaisEcrase() throws IOException {
        var store = new FileCampaignStore(dir);
        Campaign valide = store.create("Phandelver");
        Files.createDirectories(dir.resolve("vide"));
        Path illisible = dir.resolve("abime").resolve("campagne.json");
        Files.createDirectories(illisible.getParent());
        Files.writeString(illisible, "{pas du json");

        assertEquals(List.of(valide), store.list());
        assertEquals("{pas du json", Files.readString(illisible));
    }

    @Test
    void unIdInconnu_nOuvreAucunChemin() {
        var store = new FileCampaignStore(dir);
        assertThrows(IllegalArgumentException.class, () -> store.games("../partie"));
        assertThrows(IllegalArgumentException.class, () -> store.maps("inconnue"));
    }

    @Test
    void lesDonneesDUnModule_vivent_dansLeDossierDeSaCampagne_etSurviventAUnNouvelAdaptateur() throws IOException {
        Path root = dir.resolve("campagnes");
        var store = new FileCampaignStore(root);
        var strahd = store.create("Strahd");
        var phandelver = store.create("Phandelver");
        assertEquals(Optional.empty(), store.readModuleData(strahd.id(), "essai"));

        store.writeModuleData(strahd.id(), "essai", "{\"chaos\":6}");
        store.writeModuleData(strahd.id(), "essai", "{\"chaos\":7}");
        store.writeModuleData(phandelver.id(), "essai", "{\"chaos\":2}");

        var relu = new FileCampaignStore(root);
        assertEquals(Optional.of("{\"chaos\":7}"), relu.readModuleData(strahd.id(), "essai"));
        assertEquals(Optional.of("{\"chaos\":2}"), relu.readModuleData(phandelver.id(), "essai"));
        assertEquals(List.of(root.resolve(strahd.id()).resolve("modules").resolve("essai.json")),
                fichiers(root.resolve(strahd.id()).resolve("modules")));
    }

    @Test
    void unIdDeModuleInvalide_nEcritRien() {
        var store = new FileCampaignStore(dir);
        var strahd = store.create("Strahd");
        assertThrows(IllegalArgumentException.class, () -> store.writeModuleData(strahd.id(), "../evade", "{}"));
        assertThrows(IllegalArgumentException.class, () -> store.writeModuleData(strahd.id(), "", "{}"));
        assertThrows(IllegalArgumentException.class, () -> store.readModuleData("inconnue", "essai"));
        assertEquals(false, Files.exists(dir.resolve(strahd.id()).resolve("modules")));
        assertEquals(false, Files.exists(dir.resolve("evade.json")));
    }

    private static List<Path> fichiers(Path dossier) throws RuntimeException {
        try (var files = Files.list(dossier)) {
            return files.sorted().toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
