package solovtt.server;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/api/decisions")
public class DecisionResource {

    private final SceneSession session;

    public DecisionResource(SceneSession session) {
        this.session = session;
    }

    /** La décision qui suspend l'action en cours (200), ou aucune (204). */
    @GET
    @Path("/pending")
    @APIResponse(responseCode = "200", description = "La décision en attente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.DecisionDto.class)))
    @APIResponse(responseCode = "204", description = "Aucune décision en attente")
    public Response pending() {
        return session.pendingDecision()
                .map(d -> Response.ok(Dtos.DecisionDto.of(d, session)).build())
                .orElseGet(() -> Response.noContent().build());
    }

    /** Répond à la décision ; l'action reprend et rend la suite, comme une attaque. */
    @POST
    @Path("/{id}")
    @APIResponse(responseCode = "200", description = "La suite de l'action, et la décision suivante s'il y en a une")
    @APIResponse(responseCode = "400", description = "Option absente de la requête ou hors des options proposées")
    @APIResponse(responseCode = "404", description = "Aucune décision en attente avec cet id")
    public Dtos.AttackResultDto answer(@PathParam("id") String id, Dtos.AnswerRequest request) {
        if (request == null || request.option() == null) throw new IllegalArgumentException("Choisir une option");
        var outcome = session.answer(id, request.option())
                .orElseThrow(() -> new NotFoundException("Aucune décision en attente avec l'id " + id));
        return Dtos.AttackResultDto.of(outcome, session);
    }
}
