package solovtt.server;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Une campagne : une histoire du joueur, avec sa propre partie, ses propres cartes enregistrées et son Carnet.
 * {@code modules} est la liste des modules actifs ; {@code null} seulement dans une fiche d'avant le choix des modules,
 * que le démarrage du serveur fige sur les modules alors installés ({@link CampaignModulesAtStartup}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Campaign(String id, String name, List<String> modules) {

    public Campaign {
        modules = modules == null ? null : List.copyOf(modules);
    }

    /** Une campagne dont tous les modules installés sont actifs. */
    public Campaign(String id, String name) {
        this(id, name, null);
    }
}
