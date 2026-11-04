package solovtt.server.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import solovtt.server.Page;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFileNotebookStoreTest {

    @TempDir
    Path dir;

    private static final String ARRIVEE = "{\"type\":\"doc\",\"content\":[{\"type\":\"heading\",\"attrs\":{\"level\":1},"
            + "\"content\":[{\"type\":\"text\",\"text\":\"Arrivée\"}]},{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\","
            + "\"marks\":[{\"type\":\"bold\"}],\"text\":\"Pluie\"}]}]}";

    @Test
    void sansDossier_carnetVide() {
        var store = new JsonFileNotebookStore(dir.resolve("carnet"));
        assertEquals(List.of(), store.list());
        assertEquals(Optional.empty(), store.lastPage());
    }

    @Test
    void pagesContenusEtDernierePage_seRelisentParUnNouvelAdaptateur() throws IOException {
        Path carnet = dir.resolve("carnet");
        var store = new JsonFileNotebookStore(carnet);
        var session = store.create("Session 1");
        var bertrand = store.create("Bertrand");
        store.saveContent(session.id(), ARRIVEE);
        store.setLastPage(session.id());

        var relu = new JsonFileNotebookStore(carnet);
        assertEquals(List.of(bertrand, new Page(session.id(), "Session 1", ARRIVEE)), relu.list());
        assertEquals(Optional.of(session.id()), relu.lastPage());
        assertTrue(Files.readString(carnet.resolve(session.id() + ".json")).contains("\"content\" : {"),
                "le contenu est un objet JSON imbriqué, pas une chaîne");
        try (Stream<Path> files = Files.list(carnet)) {
            assertEquals(Stream.of(bertrand.id() + ".json", "carnet.json", session.id() + ".json").sorted().toList(),
                    files.map(f -> f.getFileName().toString()).sorted().toList());
        }
    }

    @Test
    void uneFicheIllisibleOuIncoherente_estIgnoree_etJamaisEcrasee() throws IOException {
        Path carnet = dir.resolve("carnet");
        var store = new JsonFileNotebookStore(carnet);
        var valide = store.create("Session 1");
        Path illisible = carnet.resolve("abimee.json");
        Files.writeString(illisible, "{pas du json");
        Files.writeString(carnet.resolve("autre.json"), "{\"id\":\"pas-autre\",\"title\":\"X\",\"content\":null}");

        assertEquals(List.of(valide), store.list());
        assertThrows(IllegalArgumentException.class, () -> store.saveContent("abimee", ARRIVEE));
        assertEquals("{pas du json", Files.readString(illisible));
    }

    @Test
    void unIdQuiNestPasCeluiDUnePage_nOuvreAucunChemin() {
        Path carnet = dir.resolve("carnet");
        var store = new JsonFileNotebookStore(carnet);
        store.create("Session 1");

        assertThrows(IllegalArgumentException.class, () -> store.saveContent("../campagne", ARRIVEE));
        assertThrows(IllegalArgumentException.class, () -> store.setLastPage("inconnue"));
        assertFalse(Files.exists(dir.resolve("campagne.json")));
    }

    @Test
    void uneFicheIllisibleNommeeParUnId_nEstPasTrouvee_etJamaisEcrasee() throws IOException {
        Path carnet = dir.resolve("carnet");
        var store = new JsonFileNotebookStore(carnet);
        store.create("Session 1");
        String id = UUID.randomUUID().toString();
        Path abimee = carnet.resolve(id + ".json");
        Files.writeString(abimee, "{pas du json");

        assertEquals(Optional.empty(), store.find(id));
        assertThrows(IllegalArgumentException.class, () -> store.saveContent(id, ARRIVEE));
        assertEquals("{pas du json", Files.readString(abimee));
    }

    @Test
    void uneDernierePageIllisible_estVide() throws IOException {
        Path carnet = dir.resolve("carnet");
        Files.createDirectories(carnet);
        Files.writeString(carnet.resolve("carnet.json"), "{pas du json");
        assertEquals(Optional.empty(), new JsonFileNotebookStore(carnet).lastPage());
    }
}
