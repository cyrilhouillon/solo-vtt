package solovtt.server;

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
