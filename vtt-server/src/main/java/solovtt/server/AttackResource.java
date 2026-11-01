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

    /** Résout l'attaque ; une cible que les règles refusent rend 400 avec la raison. */
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
        var entries = session.attack(request.attackerId(), request.attackId(), request.targetId());
        var target = session.creature(request.targetId()).orElseThrow();
        return new Dtos.AttackResultDto(entries.stream().map(Dtos.JournalEntryDto::of).toList(),
                target.id(), target.hitPoints(), target.maxHitPoints());
    }
}
