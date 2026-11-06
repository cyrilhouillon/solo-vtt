package solovtt.server.persistence;

import solovtt.module.CommonModuleData;
import solovtt.server.CampaignStore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/** Les données communes des modules, une par fichier {@code <racine>/<module>.json}, écrites atomiquement. */
public final class FileCommonModuleData implements CommonModuleData {

    private final Path root;

    public FileCommonModuleData(Path root) {
        this.root = root.toAbsolutePath();
    }

    @Override
    public synchronized Optional<String> read(String moduleId) {
        Path file = fileOf(moduleId);
        if (!Files.exists(file)) return Optional.empty();
        try {
            return Optional.of(Files.readString(file));
        } catch (IOException e) {
            throw new UncheckedIOException("Données communes de module illisibles : " + file, e);
        }
    }

    @Override
    public synchronized void write(String moduleId, String json) {
        Path file = fileOf(moduleId);
        try {
            Files.createDirectories(root);
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, json);
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible d'écrire les données communes de module : " + file, e);
        }
    }

    private Path fileOf(String moduleId) {
        return root.resolve(CampaignStore.checkModuleId(moduleId) + ".json");
    }
}
