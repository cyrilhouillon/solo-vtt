package solovtt.server;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import java.util.List;

/**
 * Refuse (404) les routes d'un module installé mais inactif dans la campagne visée,
 * {@code /api/campaigns/{id}/<module>/…} : un module inactif se comporte comme un module absent, sans avoir à le vérifier
 * lui-même. Les routes communes d'un module ({@code /api/<module>/…}) restent ouvertes ; une campagne inconnue est
 * laissée à la ressource.
 */
public class InactiveModuleFilter {

    private final InstalledModules installed;
    private final CampaignStore campaigns;

    public InactiveModuleFilter(InstalledModules installed, CampaignStore campaigns) {
        this.installed = installed;
        this.campaigns = campaigns;
    }

    @ServerRequestFilter
    public void refuseInactiveModule(ContainerRequestContext request) {
        List<String> segments = List.of(request.getUriInfo().getPath().replaceFirst("^/", "").split("/"));
        if (segments.size() < 4 || !segments.get(0).equals("api") || !segments.get(1).equals("campaigns")) return;
        String module = segments.get(3);
        if (!installed.contains(module)) return;
        campaigns.find(segments.get(2))
                .filter(campaign -> !installed.activeIn(campaign).contains(module))
                .ifPresent(campaign -> {
                    throw new NotFoundException("Module inactif dans cette campagne : " + module);
                });
    }
}
