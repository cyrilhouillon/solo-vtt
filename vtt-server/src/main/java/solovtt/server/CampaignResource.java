package solovtt.server;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;

/**
 * Les campagnes : les lister, en créer une, et jouer dans chacune. Tout ce qui touche une partie vit sous
 * {@code /api/campaigns/{campaignId}/…} ; un id inconnu rend 404.
 */
@Path("/api/campaigns")
public class CampaignResource {

    static final int MAX_NAME_LENGTH = 80;

    private final CampaignStore campaigns;
    private final CampaignSessions sessions;
    private final ImageStore images;

    public CampaignResource(CampaignStore campaigns, CampaignSessions sessions, ImageStore images) {
        this.campaigns = campaigns;
        this.sessions = sessions;
        this.images = images;
    }

    @GET
    @APIResponse(responseCode = "200", description = "Les campagnes, triées par nom")
    public List<Dtos.CampaignDto> list() {
        return campaigns.list().stream().map(Dtos.CampaignDto::of).toList();
    }

    /** Crée une campagne neuve : ni carte, ni combat, ni carte enregistrée. */
    @POST
    @ResponseStatus(201)
    @APIResponse(responseCode = "201", description = "La campagne créée",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.CampaignDto.class)))
    @APIResponse(responseCode = "400", description = "Nom absent, vide ou trop long")
    public Dtos.CampaignDto create(Dtos.CampaignNameRequest request) {
        String name = request == null || request.name() == null ? "" : request.name().strip();
        if (name.isEmpty()) throw new IllegalArgumentException("Donner un nom à la campagne");
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Nom trop long (" + MAX_NAME_LENGTH + " caractères au plus)");
        }
        return Dtos.CampaignDto.of(campaigns.create(name));
    }

    @Path("/{campaignId}/scene")
    public SceneResource scene(@PathParam("campaignId") String campaignId) {
        return new SceneResource(sessions.of(campaignId), images);
    }

    @Path("/{campaignId}/maps")
    public MapResource maps(@PathParam("campaignId") String campaignId) {
        return new MapResource(sessions.of(campaignId));
    }

    @Path("/{campaignId}/creatures")
    public CreatureResource creatures(@PathParam("campaignId") String campaignId) {
        return new CreatureResource(sessions.of(campaignId));
    }

    @Path("/{campaignId}/attacks")
    public AttackResource attacks(@PathParam("campaignId") String campaignId) {
        return new AttackResource(sessions.of(campaignId));
    }

    @Path("/{campaignId}/journal")
    public JournalResource journal(@PathParam("campaignId") String campaignId) {
        return new JournalResource(sessions.of(campaignId));
    }

    @Path("/{campaignId}/decisions")
    public DecisionResource decisions(@PathParam("campaignId") String campaignId) {
        return new DecisionResource(sessions.of(campaignId));
    }

    /** Le Carnet ne touche pas la partie : il ne fait pas rejouer la session de la campagne. */
    @Path("/{campaignId}/notebook")
    public NotebookResource notebook(@PathParam("campaignId") String campaignId) {
        if (campaigns.find(campaignId).isEmpty()) throw new NotFoundException("Campagne inconnue : " + campaignId);
        return new NotebookResource(campaigns.notebook(campaignId));
    }

    @Path("/{campaignId}/combat")
    public CombatResource combat(@PathParam("campaignId") String campaignId) {
        return new CombatResource(sessions.of(campaignId));
    }
}
