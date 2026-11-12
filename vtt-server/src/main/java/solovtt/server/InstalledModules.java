package solovtt.server;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import solovtt.module.InstalledModule;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Les modules installés dans l'application, tels qu'ils se déclarent ({@link InstalledModule}) : le cœur n'en nomme aucun. */
@ApplicationScoped
public class InstalledModules {

    private final List<String> ids;

    @Inject
    public InstalledModules(Instance<InstalledModule> modules) {
        this(modules.stream().map(InstalledModule::id).toList());
    }

    /** Ces modules, tels qu'ils se déclarent : sert aussi aux tests, sans Quarkus. */
    InstalledModules(List<String> ids) {
        this.ids = ids.stream().map(CampaignStore::checkModuleId).distinct().sorted().toList();
        // La section du Carnet de ce nom est celle des combats, que tient le cœur.
        if (this.ids.contains(NotebookStore.COMBATS_SECTION)) {
            throw new IllegalArgumentException("Id de module réservé par le cœur : " + NotebookStore.COMBATS_SECTION);
        }
    }

    /** Les ids des modules installés, triés. */
    public List<String> ids() {
        return ids;
    }

    /** Vrai si un module installé porte cet id. */
    public boolean contains(String id) {
        return ids.contains(id);
    }

    /**
     * Le choix du joueur, sans doublon, dans l'ordre des modules installés.
     *
     * @throws IllegalArgumentException si un id n'est pas celui d'un module installé (400)
     */
    public List<String> check(List<String> chosen) {
        if (chosen == null) throw new IllegalArgumentException("Modules absents");
        Set<String> wanted = new LinkedHashSet<>();
        for (String id : chosen) {
            if (id == null || !ids.contains(id)) throw new IllegalArgumentException("Module inconnu : " + id);
            wanted.add(id);
        }
        return ids.stream().filter(wanted::contains).toList();
    }

    /**
     * Les modules actifs d'une campagne : les siens encore installés (un module retiré de l'assemblage n'est plus actif
     * nulle part, et l'enregistrement suivant l'efface de la fiche), ou tous les modules installés si elle n'en a pas
     * choisi.
     */
    public List<String> activeIn(Campaign campaign) {
        return campaign.modules() == null ? ids : campaign.modules().stream().filter(ids::contains).toList();
    }
}
