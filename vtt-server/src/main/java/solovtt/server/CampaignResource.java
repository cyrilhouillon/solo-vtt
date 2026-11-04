package solovtt.server;

import jakarta.ws.rs.GET;
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
 * {@code /api/campaigns/{id}/…} ; un id inconnu rend 404.
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

    @Path("/{id}/scene")
    public SceneResource scene(@PathParam("id") String id) {
        return new SceneResource(sessions.of(id), images);
    }

    @Path("/{id}/maps")
    public MapResource maps(@PathParam("id") String id) {
        return new MapResource(sessions.of(id));
    }

    @Path("/{id}/creatures")
    public CreatureResource creatures(@PathParam("id") String id) {
        return new CreatureResource(sessions.of(id));
    }

    @Path("/{id}/attacks")
    public AttackResource attacks(@PathParam("id") String id) {
        return new AttackResource(sessions.of(id));
    }

    @Path("/{id}/journal")
    public JournalResource journal(@PathParam("id") String id) {
        return new JournalResource(sessions.of(id));
    }

    @Path("/{id}/decisions")
    public DecisionResource decisions(@PathParam("id") String id) {
        return new DecisionResource(sessions.of(id));
    }

    @Path("/{id}/combat")
    public CombatResource combat(@PathParam("id") String id) {
        return new CombatResource(sessions.of(id));
    }
}
