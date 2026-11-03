package solovtt.server.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import solovtt.scene.DistanceUnit;
import solovtt.server.SavedMap;
import solovtt.server.SavedMap.SavedToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonFileMapStoreTest {

    @TempDir
    Path dir;

    private static final SavedMap CRYPTE = new SavedMap("Crypte", 30, 20, DistanceUnit.METRE, "a".repeat(64), false, 0, 0,
            List.of(new SavedToken("gobelin", 12, 1.5), new SavedToken("loup", 25.3, 15)));
    private static final SavedMap GROTTE = new SavedMap("Grotte", 100, 65, DistanceUnit.FOOT, null, true, 0, 0, List.of());

    @Test
    void sansFichier_aucuneCarte() {
        var store = new JsonFileMapStore(dir.resolve("cartes.json"));
        assertEquals(List.of(), store.list());
        assertEquals(Optional.empty(), store.find("Crypte"));
    }

    @Test
    void deuxCartesEnregistrees_seRelisentParUnNouvelAdaptateur_trieesParNom() {
        Path file = dir.resolve("sous-dossier/cartes.json");
        var store = new JsonFileMapStore(file);
        store.save(GROTTE);
        store.save(CRYPTE);

        var relu = new JsonFileMapStore(file);
        assertEquals(List.of(CRYPTE, GROTTE), relu.list());
        assertEquals(Optional.of(GROTTE), relu.find("Grotte"));
    }

    @Test
    void unNomDejaPris_estRemplace_sansFichierTemporaireRestant() throws IOException {
        var store = new JsonFileMapStore(dir.resolve("cartes.json"));
        store.save(CRYPTE);
        var autre = new SavedMap("Crypte", 45, 30, DistanceUnit.METRE, null, true, 0, 0, List.of());
        store.save(autre);

        assertEquals(List.of(autre), store.list());
        try (Stream<Path> files = Files.list(dir)) {
            assertEquals(List.of(dir.resolve("cartes.json")), files.toList());
        }
    }

    @Test
    void leCalageDeLaGrille_seRelit() {
        Path file = dir.resolve("cartes.json");
        var calee = new SavedMap("Calée", 33, 22, DistanceUnit.METRE, "a".repeat(64), true, 0.25, 0.5, List.of());
        new JsonFileMapStore(file).save(calee);

        assertEquals(Optional.of(calee), new JsonFileMapStore(file).find("Calée"));
    }

    @Test
    void uneCarteEnregistreeAvantLeCalage_seRelitSansDecalage() throws IOException {
        Path file = dir.resolve("cartes.json");
        Files.writeString(file, """
                [ {
                  "name" : "Grotte", "width" : 100.0, "height" : 65.0, "unit" : "FOOT",
                  "imageId" : null, "gridVisible" : true, "tokens" : [ ]
                } ]
                """);

        assertEquals(Optional.of(GROTTE), new JsonFileMapStore(file).find("Grotte"));
    }

    @Test
    void unFichierIllisible_leveUneErreur_etNEstJamaisEcrase() throws IOException {
        Path file = dir.resolve("cartes.json");
        Files.writeString(file, "{ pas du json");
        var store = new JsonFileMapStore(file);

        assertThrows(RuntimeException.class, store::list);
        assertThrows(RuntimeException.class, () -> store.find("Crypte"));
        assertThrows(RuntimeException.class, () -> store.save(CRYPTE));
        assertEquals("{ pas du json", Files.readString(file));
    }
}
