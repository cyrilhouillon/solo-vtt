package solovtt.server.persistence;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import solovtt.server.ImageStore;

import java.nio.file.Path;
import java.util.Optional;

/** Le stockage des images de carte : le dossier de {@code solovtt.images.path}, ou la mémoire si ce chemin est vide (profil de test). */
public class ImageStoreProducer {

    @Produces
    @Singleton
    ImageStore imageStore(@ConfigProperty(name = "solovtt.images.path") Optional<String> path) {
        return path.filter(p -> !p.isBlank())
                .<ImageStore>map(p -> new FileImageStore(Path.of(p)))
                .orElseGet(ImageStore::inMemory);
    }
}
