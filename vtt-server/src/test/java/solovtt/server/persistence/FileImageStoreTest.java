package solovtt.server.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileImageStoreTest {

    @TempDir
    Path dir;

    private static final byte[] IMAGE = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};

    @Test
    void uneImageGardee_seRelitApresUnNouveauLancement() {
        String id = new FileImageStore(dir.resolve("images")).put(IMAGE, "image/png");

        var relue = new FileImageStore(dir.resolve("images")).find(id).orElseThrow();

        assertArrayEquals(IMAGE, relue.bytes());
        assertEquals("image/png", relue.mediaType());
        assertTrue(Files.exists(dir.resolve("images/" + id + ".png")));
    }

    @Test
    void laMemeImageGardeeDeuxFois_neFaitQuUnFichier() throws IOException {
        var store = new FileImageStore(dir);
        String premiere = store.put(IMAGE, "image/png");
        String seconde = store.put(IMAGE.clone(), "image/png");

        assertEquals(premiere, seconde);
        try (Stream<Path> files = Files.list(dir)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void unIdForge_neSortJamaisDuDossier() throws IOException {
        Files.write(dir.resolve("secret.png"), IMAGE);
        var store = new FileImageStore(dir.resolve("images"));

        assertTrue(store.find("../secret").isEmpty());
        assertTrue(store.find("0".repeat(64)).isEmpty());
        assertTrue(store.find(null).isEmpty());
    }
}
