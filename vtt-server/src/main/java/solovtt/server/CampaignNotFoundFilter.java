package solovtt.server;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;

/**
 * Documente le 404 d'une campagne inconnue sur chaque route d'une partie : smallrye-openapi ignore les
 * {@code @APIResponse} des localisateurs de sous-ressources de {@link CampaignResource}.
 */
public class CampaignNotFoundFilter implements OASFilter {

    private static final String PREFIX = "/api/campaigns/{campaignId}/";

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        openAPI.getPaths().getPathItems().forEach((path, item) -> {
            if (!path.startsWith(PREFIX)) return;
            item.getOperations().values().forEach(operation -> {
                var responses = operation.getResponses();
                if (responses.hasAPIResponse("404")) {
                    var existing = responses.getAPIResponse("404");
                    existing.setDescription(existing.getDescription() + ", ou aucune campagne avec cet id");
                } else {
                    responses.addAPIResponse("404", OASFactory.createAPIResponse().description("Aucune campagne avec cet id"));
                }
            });
        });
    }
}
