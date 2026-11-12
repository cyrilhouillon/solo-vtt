package solovtt.server;

import jakarta.ws.rs.GET;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.util.List;

/** Le catalogue de préparation d'une campagne : ses personnages joueurs, puis les monstres du SRD. */
public class CatalogResource {

    private final CampaignCatalog catalog;
    private final String campaignId;

    public CatalogResource(CampaignCatalog catalog, String campaignId) {
        this.catalog = catalog;
        this.campaignId = campaignId;
    }

    @GET
    @APIResponse(responseCode = "200", description = "Les personnages joueurs des modules actifs, puis les monstres")
    public List<Dtos.CatalogEntryDto> entries() {
        return catalog.entries(campaignId);
    }
}
