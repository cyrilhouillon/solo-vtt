package solovtt.server.persistence;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import solovtt.server.GameStore;

import java.nio.file.Path;
import java.util.Optional;

/** Le stockage de la partie : le fichier JSON de {@code solovtt.save.path}, ou aucun si ce chemin est vide (profil de test). */
public class GameStoreProducer {

    @Produces
    @Singleton
    GameStore gameStore(@ConfigProperty(name = "solovtt.save.path") Optional<String> path) {
        return path.filter(p -> !p.isBlank())
                .<GameStore>map(p -> new JsonFileGameStore(Path.of(p)))
                .orElseGet(GameStore::none);
    }
}
