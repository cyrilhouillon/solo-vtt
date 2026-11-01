package solovtt.server;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/api/attacks")
public class AttackResource {

    private final SceneSession session;

    public AttackResource(SceneSession session) {
        this.session = session;
    }

    /**
     * Résout l'attaque, ou la suspend sur une décision ; une cible que les règles refusent rend 400 avec la raison,
     * une décision en attente rend 409.
     */
    @POST
    @APIResponse(responseCode = "200", description = "Les entrées ajoutées au journal, les PV de la cible, et la décision qui suspend l'attaque s'il y en a une")
    @APIResponse(responseCode = "400", description = "Requête incomplète, attaque inconnue, ou cible refusée par les règles (allonge, portée)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.ErrorDto.class)))
    @APIResponse(responseCode = "404", description = "Attaquant ou cible inconnus",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.ErrorDto.class)))
    @APIResponse(responseCode = "409", description = "Aucune carte, ou une décision attend une réponse",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.ErrorDto.class)))
    public Dtos.AttackResultDto attack(Dtos.AttackRequest request) {
        if (session.scene().isEmpty()) {
            throw new ClientErrorException("Aucune carte : créer d'abord une carte", Response.Status.CONFLICT);
        }
        if (request == null || request.attackerId() == null || request.attackId() == null || request.targetId() == null) {
            throw new IllegalArgumentException("Indiquer l'attaquant, l'attaque et la cible");
        }
        for (String id : new String[] {request.attackerId(), request.targetId()}) {
            if (session.creature(id).isEmpty()) throw new NotFoundException("Créature inconnue : " + id);
        }
        return Dtos.AttackResultDto.of(session.attack(request.attackerId(), request.attackId(), request.targetId()), session);
    }
}
