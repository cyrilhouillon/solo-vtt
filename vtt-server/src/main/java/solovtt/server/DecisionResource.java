package solovtt.server;

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
    public Response pending() {
        return session.pendingDecision()
                .map(d -> Response.ok(Dtos.DecisionDto.of(d, session)).build())
                .orElseGet(() -> Response.noContent().build());
    }

    /** Répond à la décision ; l'action reprend et rend la suite, comme une attaque. */
    @POST
    @Path("/{id}")
    public Dtos.AttackResultDto answer(@PathParam("id") String id, Dtos.AnswerRequest request) {
        if (request == null || request.option() == null) throw new IllegalArgumentException("Choisir une option");
        var outcome = session.answer(id, request.option())
                .orElseThrow(() -> new NotFoundException("Aucune décision en attente avec l'id " + id));
        return Dtos.AttackResultDto.of(outcome, session);
    }
}
