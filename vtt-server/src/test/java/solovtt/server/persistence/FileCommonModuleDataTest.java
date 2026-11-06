package solovtt.server.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileCommonModuleDataTest {

    @TempDir
    Path dir;

    @Test
    void unModuleSansDocument_neLitRien() {
        assertEquals(Optional.empty(), new FileCommonModuleData(dir.resolve("modules")).read("essai"));
    }

    @Test
    void leDocumentDUnModule_vitDansSonFichier_etSurvitAUnNouvelAdaptateur() throws IOException {
        Path root = dir.resolve("modules");
        var data = new FileCommonModuleData(root);

        data.write("essai", "{\"tables\":1}");
        data.write("essai", "{\"tables\":2}");

        assertEquals(Optional.of("{\"tables\":2}"), new FileCommonModuleData(root).read("essai"));
        try (Stream<Path> files = Files.list(root)) {
            assertEquals(List.of(root.resolve("essai.json")), files.toList());
        }
    }

    @Test
    void unIdDeModuleInvalide_nOuvreAucunChemin() {
        var data = new FileCommonModuleData(dir);
        assertThrows(IllegalArgumentException.class, () -> data.write("../evade", "{}"));
        assertThrows(IllegalArgumentException.class, () -> data.write("A", "{}"));
        assertThrows(IllegalArgumentException.class, () -> data.read("../evade"));
    }

    @Test
    void enMemoire_leDocumentSeRelit_etLIdEstVerifie() {
        var data = CommonModuleDataProducer.inMemory();
        assertEquals(Optional.empty(), data.read("essai"));
        data.write("essai", "{}");
        assertEquals(Optional.of("{}"), data.read("essai"));
        assertThrows(IllegalArgumentException.class, () -> data.read("../evade"));
    }
}
