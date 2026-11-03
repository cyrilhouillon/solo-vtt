package solovtt.server.persistence;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import solovtt.server.MapStore;

import java.nio.file.Path;
import java.util.Optional;

/** Le stockage des cartes enregistrées : le fichier JSON de {@code solovtt.maps.path}, ou la mémoire si ce chemin est vide (profil de test). */
public class MapStoreProducer {

    @Produces
    @Singleton
    MapStore mapStore(@ConfigProperty(name = "solovtt.maps.path") Optional<String> path) {
        return path.filter(p -> !p.isBlank())
                .<MapStore>map(p -> new JsonFileMapStore(Path.of(p)))
                .orElseGet(MapStore::inMemory);
    }
}
