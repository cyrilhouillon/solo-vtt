package solovtt.server;

import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/api/creatures")
public class CreatureResource {

    private final SceneSession session;

    public CreatureResource(SceneSession session) {
        this.session = session;
    }

    /** La feuille d'une créature posée sur la carte. */
    @GET
    @Path("/{id}")
    @APIResponse(responseCode = "200", description = "La feuille, longueurs dans l'unité de la carte")
    @APIResponse(responseCode = "404", description = "Créature inconnue")
    public Dtos.CreatureSheetDto sheet(@PathParam("id") String id) {
        var creature = session.creature(id).orElseThrow(() -> new NotFoundException("Créature inconnue : " + id));
        var unit = session.scene().orElseThrow().unit();
        return Dtos.CreatureSheetDto.of(creature, session.entryOf(id).kind(), unit);
    }
}
