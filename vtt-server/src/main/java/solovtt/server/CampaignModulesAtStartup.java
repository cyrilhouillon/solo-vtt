package solovtt.server;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.util.List;

/**
 * Au démarrage, une campagne qui n'a jamais choisi ses modules (fiche d'avant le choix) reçoit la liste explicite des
 * modules installés à ce moment : un module installé plus tard n'y sera pas actif d'office, le joueur l'active lui-même.
 */
@ApplicationScoped
public class CampaignModulesAtStartup {

    private static final System.Logger LOG = System.getLogger(CampaignModulesAtStartup.class.getName());

    private final CampaignStore campaigns;
    private final InstalledModules installed;

    public CampaignModulesAtStartup(CampaignStore campaigns, InstalledModules installed) {
        this.campaigns = campaigns;
        this.installed = installed;
    }

    void onStart(@Observes StartupEvent event) {
        fixUnchosen();
    }

    /**
     * Écrit les modules installés dans chaque campagne qui n'en a pas choisi ; rend leur nombre. Une erreur d'écriture ou
     * de lecture n'empêche pas le serveur de démarrer : la campagne garde sa fiche, lue comme « tous les modules
     * installés », et sera figée au prochain démarrage.
     */
    int fixUnchosen() {
        List<Campaign> all;
        try {
            all = campaigns.list();
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Campagnes illisibles : modules non figés", e);
            return 0;
        }
        int fixed = 0;
        for (Campaign campaign : all) {
            if (campaign.modules() != null) continue;
            try {
                campaigns.setModules(campaign.id(), installed.ids());
                fixed++;
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "Modules non figés pour la campagne " + campaign.id(), e);
            }
        }
        return fixed;
    }
}
