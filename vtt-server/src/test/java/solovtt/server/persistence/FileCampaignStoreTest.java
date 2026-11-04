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
        var page = store.notebook(strahd.id()).create("Session 1");

        var relu = new FileCampaignStore(root);
        assertEquals(List.of(page), relu.notebook(strahd.id()).list());
        assertEquals(List.of(), relu.notebook(phandelver.id()).list());
        assertEquals(true, Files.exists(root.resolve(strahd.id()).resolve("carnet").resolve(page.id() + ".json")));
        assertThrows(IllegalArgumentException.class, () -> store.notebook("../" + strahd.id()));
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

    private static List<Path> fichiers(Path dossier) throws RuntimeException {
        try (var files = Files.list(dossier)) {
            return files.sorted().toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
