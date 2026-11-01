package solovtt.server;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/api/combat")
public class CombatResource {

    private final SceneSession session;

    public CombatResource(SceneSession session) {
        this.session = session;
    }

    /** Le combat en cours (200), ou aucun (204). */
    @GET
    @APIResponse(responseCode = "200", description = "Le round, la créature qui joue, les candidats et l'ordre de jeu",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.CombatDto.class)))
    @APIResponse(responseCode = "204", description = "Aucun combat en cours")
    public Response combat() {
        return session.combat()
                .map(c -> Response.ok(Dtos.CombatDto.of(c, session)).build())
                .orElseGet(() -> Response.noContent().build());
    }

    /** Lance le combat : chaque créature tire son initiative ; « Qui joue ? » attend si le premier rang est partagé. */
    @POST
    @APIResponse(responseCode = "200", description = "Les entrées du journal, le combat, et la décision « Qui joue ? » s'il y en a une")
    @APIResponse(responseCode = "409", description = "Aucune carte, aucune créature, combat déjà lancé, ou une décision attend une réponse")
    public Dtos.ActionResultDto start() {
        requireScene();
        return Dtos.ActionResultDto.of(session.startCombat(), session);
    }

    /** Termine le tour de la créature active ; ouvre le suivant, ou un nouveau round après le dernier rang. */
    @POST
    @Path("/end-turn")
    @APIResponse(responseCode = "200", description = "Les entrées du journal, le combat, et la décision « Qui joue ? » s'il y en a une")
    @APIResponse(responseCode = "409", description = "Aucune carte, aucun combat, ou une décision attend une réponse")
    public Dtos.ActionResultDto endTurn() {
        requireScene();
        return Dtos.ActionResultDto.of(session.endTurn(), session);
    }

    private void requireScene() {
        if (session.scene().isEmpty()) {
            throw new ClientErrorException("Aucune carte : créer d'abord une carte", Response.Status.CONFLICT);
        }
    }
}
