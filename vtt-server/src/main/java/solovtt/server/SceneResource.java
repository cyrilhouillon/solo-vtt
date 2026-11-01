package solovtt.server;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.ResponseStatus;
import solovtt.scene.Scene;

@Path("/api/scene")
public class SceneResource {

    private final SceneSession session;

    public SceneResource(SceneSession session) {
        this.session = session;
    }

    /** Crée une carte vide ; la précédente et ses créatures disparaissent. */
    @POST
    @APIResponse(responseCode = "201", description = "La carte créée",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.SceneDto.class)))
    @APIResponse(responseCode = "400", description = "Dimensions ou unité invalides",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.ErrorDto.class)))
    @ResponseStatus(201)
    public Dtos.SceneDto create(Dtos.NewSceneRequest request) {
        if (request == null || request.unit() == null) {
            throw new IllegalArgumentException("Indiquer la largeur, la hauteur et l'unité de la carte");
        }
        return Dtos.SceneDto.of(session.create(request.width(), request.height(), request.unit()), session);
    }

    @GET
    @APIResponse(responseCode = "200", description = "La carte et ses jetons")
    @APIResponse(responseCode = "409", description = "Aucune carte",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.ErrorDto.class)))
    public Dtos.SceneDto get() {
        return Dtos.SceneDto.of(requireScene(), session);
    }

    @POST
    @Path("/tokens")
    @ResponseStatus(201)
    @APIResponse(responseCode = "201", description = "Le jeton posé",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.TokenDto.class)))
    @APIResponse(responseCode = "400", description = "Créature du catalogue absente de la requête",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.ErrorDto.class)))
    @APIResponse(responseCode = "404", description = "Entrée de catalogue inconnue",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.ErrorDto.class)))
    @APIResponse(responseCode = "409", description = "Aucune carte, ou une décision attend une réponse",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.ErrorDto.class)))
    public Dtos.TokenDto place(Dtos.PlaceTokenRequest request) {
        requireScene();
        if (request == null || request.catalogId() == null) {
            throw new IllegalArgumentException("Choisir une créature du catalogue");
        }
        var entry = SceneSession.catalogEntry(request.catalogId())
                .orElseThrow(() -> new NotFoundException("Créature inconnue au catalogue : " + request.catalogId()));
        var token = session.place(entry, request.x(), request.y());
        return Dtos.TokenDto.of(token, entry, session.creature(token.entityId()).orElseThrow());
    }

    /** Retire un jeton et sa créature. */
    @DELETE
    @Path("/tokens/{id}")
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "Jeton et créature retirés")
    @APIResponse(responseCode = "404", description = "Jeton inconnu",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.ErrorDto.class)))
    @APIResponse(responseCode = "409", description = "Aucune carte, ou une décision attend une réponse",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.ErrorDto.class)))
    public void remove(@PathParam("id") String id) {
        if (!requireScene().contains(id)) throw new NotFoundException("Jeton inconnu : " + id);
        session.remove(id);
    }

    private Scene requireScene() {
        return session.scene().orElseThrow(() ->
                new ClientErrorException("Aucune carte : créer d'abord une carte", Response.Status.CONFLICT));
    }
}
