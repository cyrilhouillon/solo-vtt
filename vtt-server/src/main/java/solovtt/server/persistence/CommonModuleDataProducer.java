package solovtt.server.persistence;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import solovtt.module.CommonModuleData;
import solovtt.server.CampaignStore;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Les données communes des modules : le dossier de {@code solovtt.modules.path}, ou la mémoire si ce chemin est vide
 * (profil de test).
 */
public class CommonModuleDataProducer {

    @Produces
    @Singleton
    CommonModuleData commonModuleData(@ConfigProperty(name = "solovtt.modules.path") Optional<String> path) {
        return path.filter(p -> !p.isBlank())
                .<CommonModuleData>map(p -> new FileCommonModuleData(Path.of(p)))
                .orElseGet(CommonModuleDataProducer::inMemory);
    }

    /** Garde les données communes en mémoire, le temps du processus. */
    static CommonModuleData inMemory() {
        Map<String, String> documents = new ConcurrentHashMap<>();
        return new CommonModuleData() {
            @Override
            public Optional<String> read(String moduleId) {
                return Optional.ofNullable(documents.get(CampaignStore.checkModuleId(moduleId)));
            }

            @Override
            public void write(String moduleId, String json) {
                documents.put(CampaignStore.checkModuleId(moduleId), json);
            }
        };
    }
}
