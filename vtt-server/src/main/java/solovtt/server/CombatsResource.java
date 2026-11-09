package solovtt.server;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

/**
 * Les combats d'une campagne : chacun se relit (son état actuel, ou final s'il est terminé) et, en cours, se reprend.
 * Un combat se relit en rejouant sa partie : un combat qui ne se rejoue plus (règles changées) est illisible (409).
 */
public class CombatsResource {

    private final CombatStore combats;
    private final SceneSession session;

    public CombatsResource(CombatStore combats, SceneSession session) {
        this.combats = combats;
        this.session = session;
    }

    /** L'état du combat : terminé ou non, joué sur la carte en cours ou non, et sa carte telle qu'il l'a laissée. */
    @GET
    @Path("/{combatId}")
    @APIResponse(responseCode = "200", description = "Le combat et sa carte",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.CombatEntryDto.class)))
    @APIResponse(responseCode = "404", description = "Aucun combat avec cet id")
    @APIResponse(responseCode = "409", description = "Combat illisible : il ne se rejoue plus")
    public Dtos.CombatEntryDto combat(@PathParam("combatId") String combatId) {
        SceneSession replayed = replayed(combatId, game(combatId));
        return new Dtos.CombatEntryDto(combatId, replayed.combatId().isEmpty(), isCurrent(combatId),
                Dtos.SceneDto.of(replayed.scene().orElseThrow(), replayed));
    }

    /**
     * Reprend un combat en cours là où il en était : sa partie remplace celle de la carte en cours. Sans effet s'il est
     * déjà sur la carte en cours.
     */
    @POST
    @Path("/{combatId}/resume")
    @APIResponse(responseCode = "204", description = "Le combat est la partie en cours")
    @APIResponse(responseCode = "404", description = "Aucun combat avec cet id")
    @APIResponse(responseCode = "409", description = "Combat terminé (il se rejoue, il ne se reprend pas) ou illisible")
    public void resume(@PathParam("combatId") String combatId) {
        if (isCurrent(combatId)) return;
        SavedGame game = game(combatId);
        if (replayed(combatId, game).combatId().isEmpty()) {
            throw new ActionRefusedException("Ce combat est terminé : il se rejoue, il ne se reprend pas");
        }
        session.resume(game);
    }

    /** Le combat terminé, pas à pas : une action par pas, avec ce qu'on voit à chacun. */
    @GET
    @Path("/{combatId}/replay")
    @APIResponse(responseCode = "200", description = "Les pas du combat, le pas 0 étant l'état au lancement",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.ReplayDto.class)))
    @APIResponse(responseCode = "404", description = "Aucun combat avec cet id")
    @APIResponse(responseCode = "409", description = "Combat en cours (il se rejoue une fois terminé) ou illisible")
    public Dtos.ReplayDto replay(@PathParam("combatId") String combatId) {
        SavedGame game = game(combatId);
        if (replayed(combatId, game).combatId().isPresent()) {
            throw new ActionRefusedException("Ce combat est en cours : il se rejoue une fois terminé");
        }
        try {
            return new Dtos.ReplayDto(combatId, CombatReplay.of(game, combatId));
        } catch (RuntimeException e) {
            throw new ActionRefusedException("Combat illisible, il ne se rejoue plus : " + combatId);
        }
    }

    private boolean isCurrent(String combatId) {
        return session.combatId().filter(combatId::equals).isPresent();
    }

    /** La partie du combat ; un id qui n'en est pas un est inconnu. */
    private SavedGame game(String combatId) {
        try {
            return combats.game(combatId).load().orElseThrow(() -> new NotFoundException("Combat inconnu : " + combatId));
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Combat inconnu : " + combatId);
        }
    }

    static SceneSession replayed(String combatId, SavedGame game) {
        try {
            return SceneSession.replayed(game);
        } catch (RuntimeException e) {
            throw new ActionRefusedException("Combat illisible, il ne se rejoue plus : " + combatId);
        }
    }
}
