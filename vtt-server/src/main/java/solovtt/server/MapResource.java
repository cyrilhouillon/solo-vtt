package solovtt.server;

import dnd.engine.catalog.CatalogEntry;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Les cartes enregistrées d'une campagne : enregistrer la carte en cours sous un nom, les lister, en charger une. */
public class MapResource {

    static final int MAX_NAME_LENGTH = 80;

    private final SceneSession session;
    /** Le catalogue de la campagne, d'où viennent les créatures d'une carte chargée. */
    private final Function<String, Optional<CatalogEntry>> catalog;

    public MapResource(SceneSession session, Function<String, Optional<CatalogEntry>> catalog) {
        this.session = session;
        this.catalog = catalog;
    }

    @GET
    @APIResponse(responseCode = "200", description = "Les cartes enregistrées de la campagne, triées par nom")
    public List<Dtos.SavedMapDto> list() {
        return session.maps().list().stream().map(Dtos.SavedMapDto::of).toList();
    }

    /** Enregistre la carte en cours sous un nom ; une carte du même nom est remplacée. La partie ne change pas. */
    @POST
    @ResponseStatus(201)
    @APIResponse(responseCode = "201", description = "La carte enregistrée",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.SavedMapDto.class)))
    @APIResponse(responseCode = "400", description = "Nom absent, vide ou trop long")
    @APIResponse(responseCode = "409", description = "Aucune carte")
    public Dtos.SavedMapDto save(Dtos.MapNameRequest request) {
        String name = nameOf(request);
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Nom trop long (" + MAX_NAME_LENGTH + " caractères au plus)");
        }
        if (session.scene().isEmpty()) {
            throw new ClientErrorException("Aucune carte : créer d'abord une carte", Response.Status.CONFLICT);
        }
        SavedMap map = session.snapshot(name);
        session.maps().save(map);
        return Dtos.SavedMapDto.of(map);
    }

    /**
     * Charge une carte enregistrée à la place de la carte en cours, comme une nouvelle carte : la partie repart de zéro,
     * avec des créatures neuves aux positions enregistrées. Permis en combat et pendant une décision.
     */
    @POST
    @Path("/load")
    @APIResponse(responseCode = "200", description = "La carte chargée et ses jetons",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.SceneDto.class)))
    @APIResponse(responseCode = "400", description = "Nom absent ou vide, ou fiche de personnage qui ne peut pas jouer")
    @APIResponse(responseCode = "404", description = "Aucune carte enregistrée sous ce nom")
    @APIResponse(responseCode = "409", description = "Une créature de la carte n'est plus au catalogue")
    public Dtos.SceneDto load(Dtos.MapNameRequest request) {
        String name = nameOf(request);
        SavedMap map = session.maps().find(name)
                .orElseThrow(() -> new NotFoundException("Aucune carte enregistrée sous le nom « " + name + " »"));
        return Dtos.SceneDto.of(session.load(map, catalog), session);
    }

    private static String nameOf(Dtos.MapNameRequest request) {
        String name = request == null || request.name() == null ? "" : request.name().strip();
        if (name.isEmpty()) throw new IllegalArgumentException("Donner un nom à la carte");
        return name;
    }
}
