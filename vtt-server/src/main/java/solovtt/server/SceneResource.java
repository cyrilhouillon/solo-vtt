package solovtt.server;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.ResponseStatus;
import solovtt.scene.Scene;

public class SceneResource {

    private final SceneSession session;
    private final ImageStore images;

    public SceneResource(SceneSession session, ImageStore images) {
        this.session = session;
        this.images = images;
    }

    /** Crée une carte, vide ou sur une image déjà envoyée ; la précédente et ses créatures disparaissent. */
    @POST
    @APIResponse(responseCode = "201", description = "La carte créée",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.SceneDto.class)))
    @APIResponse(responseCode = "400", description = "Dimensions ou unité invalides, ou image inconnue")
    @ResponseStatus(201)
    public Dtos.SceneDto create(Dtos.NewSceneRequest request) {
        if (request == null || request.unit() == null) {
            throw new IllegalArgumentException("Indiquer la largeur, la hauteur et l'unité de la carte");
        }
        if (request.imageId() != null && images.find(request.imageId()).isEmpty()) {
            throw new IllegalArgumentException("Image inconnue : l'envoyer d'abord");
        }
        return Dtos.SceneDto.of(session.create(request.width(), request.height(), request.unit(), request.imageId()), session);
    }

    /** Affiche ou masque la grille de la carte ; permis en combat et pendant une décision. */
    @PUT
    @Path("/grid")
    @APIResponse(responseCode = "200", description = "La carte, avec son choix de grille",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.SceneDto.class)))
    @APIResponse(responseCode = "400", description = "Choix absent de la requête")
    @APIResponse(responseCode = "409", description = "Aucune carte")
    public Dtos.SceneDto showGrid(Dtos.GridRequest request) {
        Scene scene = requireScene();
        if (request == null || request.visible() == null) {
            throw new IllegalArgumentException("Indiquer si la grille s'affiche");
        }
        session.showGrid(request.visible());
        return Dtos.SceneDto.of(scene, session);
    }

    /**
     * Cale la grille sur l'image : la carte et ses jetons changent d'échelle ensemble, la grille part du décalage donné.
     * Refusé en combat, pendant une décision et sans image.
     */
    @PUT
    @Path("/alignment")
    @APIResponse(responseCode = "200", description = "La carte mise à l'échelle, ses jetons et le décalage de sa grille",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.SceneDto.class)))
    @APIResponse(responseCode = "400", description = "Champ absent, dimension non positive, décalage hors de [0, 1) ou cases non carrées")
    @APIResponse(responseCode = "409", description = "Aucune carte, carte sans image, combat en cours, ou une décision attend une réponse")
    public Dtos.SceneDto alignGrid(Dtos.AlignmentRequest request) {
        requireScene();
        if (request == null || request.width() == null || request.height() == null
                || request.gridOffsetX() == null || request.gridOffsetY() == null) {
            throw new IllegalArgumentException("Indiquer la largeur, la hauteur et le décalage de la grille");
        }
        Scene aligned = session.alignGrid(request.width(), request.height(), request.gridOffsetX(), request.gridOffsetY());
        return Dtos.SceneDto.of(aligned, session);
    }

    @GET
    @APIResponse(responseCode = "200", description = "La carte et ses jetons")
    @APIResponse(responseCode = "409", description = "Aucune carte")
    public Dtos.SceneDto get() {
        return Dtos.SceneDto.of(requireScene(), session);
    }

    @POST
    @Path("/tokens")
    @ResponseStatus(201)
    @APIResponse(responseCode = "201", description = "Le jeton posé ; en combat, la créature tire son initiative et entre dans l'ordre",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.TokenDto.class)))
    @APIResponse(responseCode = "400", description = "Créature du catalogue absente de la requête")
    @APIResponse(responseCode = "404", description = "Entrée de catalogue inconnue")
    @APIResponse(responseCode = "409", description = "Aucune carte, ou une décision attend une réponse")
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

    /**
     * Déplace un jeton. En combat, la créature active est limitée par sa vitesse restante et son déplacement entre au
     * journal ; les autres se repositionnent librement.
     */
    @POST
    @Path("/tokens/{id}/move")
    @APIResponse(responseCode = "200", description = "Les entrées ajoutées au journal (aucune pour un déplacement libre) et le combat",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.ActionResultDto.class)))
    @APIResponse(responseCode = "400", description = "Position absente ou hors de la carte, ou déplacement refusé par les règles (vitesse restante, vitesse de 0)")
    @APIResponse(responseCode = "404", description = "Jeton inconnu")
    @APIResponse(responseCode = "409", description = "Aucune carte, ou une décision attend une réponse")
    public Dtos.ActionResultDto move(@PathParam("id") String id, Dtos.MoveTokenRequest request) {
        if (!requireScene().contains(id)) throw new NotFoundException("Jeton inconnu : " + id);
        if (request == null || request.x() == null || request.y() == null) {
            throw new IllegalArgumentException("Indiquer la position d'arrivée");
        }
        return Dtos.ActionResultDto.of(session.move(id, request.x(), request.y()), session);
    }

    /** Retire un jeton et sa créature. */
    @DELETE
    @Path("/tokens/{id}")
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "Jeton et créature retirés ; en combat, la créature quitte l'ordre, et si elle jouait, le tour suivant s'ouvre")
    @APIResponse(responseCode = "404", description = "Jeton inconnu")
    @APIResponse(responseCode = "409", description = "Aucune carte, ou une décision attend une réponse")
    public void remove(@PathParam("id") String id) {
        if (!requireScene().contains(id)) throw new NotFoundException("Jeton inconnu : " + id);
        session.remove(id);
    }

    private Scene requireScene() {
        return session.scene().orElseThrow(() ->
                new ClientErrorException("Aucune carte : créer d'abord une carte", Response.Status.CONFLICT));
    }
}
