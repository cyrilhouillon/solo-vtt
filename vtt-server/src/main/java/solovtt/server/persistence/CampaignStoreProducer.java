package solovtt.server.persistence;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import solovtt.server.CampaignStore;

import java.nio.file.Path;
import java.util.Optional;

/** Le stockage des campagnes : le dossier de {@code solovtt.campaigns.path}, ou la mémoire si ce chemin est vide (profil de test). */
public class CampaignStoreProducer {

    @Produces
    @Singleton
    CampaignStore campaignStore(@ConfigProperty(name = "solovtt.campaigns.path") Optional<String> path) {
        return path.filter(p -> !p.isBlank())
                .<CampaignStore>map(p -> new FileCampaignStore(Path.of(p)))
                .orElseGet(CampaignStore::inMemory);
    }
}
