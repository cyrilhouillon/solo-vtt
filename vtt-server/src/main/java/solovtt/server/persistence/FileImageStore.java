package solovtt.server.persistence;

import solovtt.server.ImageStore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Les images de carte dans un dossier, un fichier {@code <empreinte>.<png|jpg|webp>} par image. Un fichier existant
 * n'est jamais réécrit ; l'écriture passe par un fichier voisin puis un déplacement atomique.
 */
public final class FileImageStore implements ImageStore {

    private static final Map<String, String> EXTENSIONS = Map.of("image/png", "png", "image/jpeg", "jpg", "image/webp", "webp");
    /** Seule une empreinte SHA-256 nomme un fichier : un id forgé ne sort jamais du dossier. */
    private static final Pattern ID = Pattern.compile("[0-9a-f]{64}");

    private final Path dir;

    public FileImageStore(Path dir) {
        this.dir = dir.toAbsolutePath();
    }

    @Override
    public String put(byte[] bytes, String mediaType) {
        String extension = EXTENSIONS.get(mediaType);
        if (extension == null) throw new IllegalArgumentException("Type d'image non pris en charge : " + mediaType);
        String id = ImageStore.idOf(bytes);
        Path file = dir.resolve(id + "." + extension);
        if (Files.exists(file)) return id;
        try {
            Files.createDirectories(dir);
            Path temporary = dir.resolve(id + "." + extension + ".tmp");
            Files.write(temporary, bytes);
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de garder l'image : " + file, e);
        }
        return id;
    }

    @Override
    public Optional<StoredImage> find(String id) {
        if (id == null || !ID.matcher(id).matches()) return Optional.empty();
        for (var type : EXTENSIONS.entrySet()) {
            Path file = dir.resolve(id + "." + type.getValue());
            if (!Files.exists(file)) continue;
            try {
                return Optional.of(new StoredImage(Files.readAllBytes(file), type.getKey()));
            } catch (IOException e) {
                throw new UncheckedIOException("Image illisible : " + file, e);
            }
        }
        return Optional.empty();
    }
}
