package solovtt.server.persistence;

import solovtt.server.CombatStore;
import solovtt.server.GameStore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** Les combats d'une campagne, un fichier par combat : {@code <dossier>/<id>.json}, au format de la partie ({@link JsonFileGameStore}). */
public final class JsonFileCombatStore implements CombatStore {

    private static final String EXTENSION = ".json";

    private final Path dir;

    public JsonFileCombatStore(Path dir) {
        this.dir = dir.toAbsolutePath();
    }

    @Override
    public GameStore game(String combatId) {
        return new JsonFileGameStore(dir.resolve(CombatStore.checkId(combatId) + EXTENSION));
    }

    @Override
    public List<String> ids() {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(f -> f.getFileName().toString())
                    .filter(name -> name.endsWith(EXTENSION))
                    .map(name -> name.substring(0, name.length() - EXTENSION.length()))
                    .filter(id -> {
                        try {
                            CombatStore.checkId(id);
                            return true;
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    })
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Combats illisibles : " + dir, e);
        }
    }
}
