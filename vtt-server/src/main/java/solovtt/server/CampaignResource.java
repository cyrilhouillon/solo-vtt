package solovtt.server;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;

/** Les campagnes : les lister, en créer une, ouvrir celle où l'on joue. */
@Path("/api/campaigns")
public class CampaignResource {

    static final int MAX_NAME_LENGTH = 80;

    private final SceneSession session;
    private final CampaignStore campaigns;

    public CampaignResource(SceneSession session, CampaignStore campaigns) {
        this.session = session;
        this.campaigns = campaigns;
    }

    @GET
    @APIResponse(responseCode = "200", description = "Les campagnes, triées par nom")
    public List<Dtos.CampaignDto> list() {
        return campaigns.list().stream().map(Dtos.CampaignDto::of).toList();
    }

    /** Crée une campagne neuve : ni carte, ni combat, ni carte enregistrée. Elle n'est pas ouverte. */
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

    /** La campagne ouverte, ou 204 si aucune (au lancement du serveur). */
    @GET
    @Path("/current")
    @APIResponse(responseCode = "200", description = "La campagne ouverte",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.CampaignDto.class)))
    @APIResponse(responseCode = "204", description = "Aucune campagne ouverte")
    public Response current() {
        return session.campaign()
                .map(c -> Response.ok(Dtos.CampaignDto.of(c)).build())
                .orElseGet(() -> Response.noContent().build());
    }

    /**
     * Ouvre la campagne : la partie en cours et les cartes enregistrées deviennent les siennes, reprises à l'identique.
     * Permis à tout moment, même en combat ou pendant une décision : la campagne quittée est déjà gardée.
     */
    @POST
    @Path("/{id}/open")
    @APIResponse(responseCode = "200", description = "La campagne ouverte",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.CampaignDto.class)))
    @APIResponse(responseCode = "404", description = "Aucune campagne avec cet id")
    public Dtos.CampaignDto open(@PathParam("id") String id) {
        Campaign campaign = campaigns.find(id).orElseThrow(() -> new NotFoundException("Campagne inconnue : " + id));
        session.open(campaign);
        return Dtos.CampaignDto.of(campaign);
    }
}
