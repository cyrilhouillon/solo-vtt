package solovtt.server;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Au démarrage, une campagne qui n'a jamais choisi ses modules (fiche d'avant le choix) reçoit la liste explicite des
 * modules installés à ce moment : un module installé plus tard n'y sera pas actif d'office, le joueur l'active lui-même.
 */
@ApplicationScoped
public class CampaignModulesAtStartup {

    private final CampaignStore campaigns;
    private final InstalledModules installed;

    public CampaignModulesAtStartup(CampaignStore campaigns, InstalledModules installed) {
        this.campaigns = campaigns;
        this.installed = installed;
    }

    void onStart(@Observes StartupEvent event) {
        fixUnchosen();
    }

    /** Écrit les modules installés dans chaque campagne qui n'en a pas choisi ; rend leur nombre. */
    int fixUnchosen() {
        int fixed = 0;
        for (Campaign campaign : campaigns.list()) {
            if (campaign.modules() != null) continue;
            campaigns.setModules(campaign.id(), installed.ids());
            fixed++;
        }
        return fixed;
    }
}
