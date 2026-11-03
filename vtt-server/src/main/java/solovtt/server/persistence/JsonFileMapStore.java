package solovtt.server.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import solovtt.server.MapStore;
import solovtt.server.SavedMap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Les cartes enregistrées, toutes dans un fichier JSON. Chaque enregistrement relit la liste puis la réécrit entière,
 * par un fichier voisin et un déplacement atomique. Un fichier illisible n'est jamais écrasé : enregistrer échoue.
 */
public final class JsonFileMapStore implements MapStore {

    private static final TypeReference<List<SavedMap>> MAPS = new TypeReference<>() {};

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public JsonFileMapStore(Path file) {
        this.file = file.toAbsolutePath();
    }

    @Override
    public synchronized List<SavedMap> list() {
        return read().stream().sorted(BY_NAME).toList();
    }

    @Override
    public synchronized void save(SavedMap map) {
        List<SavedMap> maps = new ArrayList<>(read());
        maps.removeIf(m -> m.name().equals(map.name()));
        maps.add(map);
        maps.sort(BY_NAME);
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            mapper.writeValue(temporary.toFile(), maps);
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible d'enregistrer la carte : " + file, e);
        }
    }

    private List<SavedMap> read() {
        if (!Files.exists(file)) return List.of();
        try {
            return mapper.readValue(file.toFile(), MAPS);
        } catch (IOException e) {
            throw new UncheckedIOException("Cartes enregistrées illisibles : " + file, e);
        }
    }
}
