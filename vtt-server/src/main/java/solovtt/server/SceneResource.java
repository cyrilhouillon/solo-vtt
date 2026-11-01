package solovtt.server;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.ResponseStatus;
import solovtt.scene.Scene;

@Path("/api/scene")
public class SceneResource {

    private final SceneSession session;

    public SceneResource(SceneSession session) {
        this.session = session;
    }

    /** Crée une carte vide ; la précédente et ses créatures disparaissent. */
    @POST
    @ResponseStatus(201)
    public Dtos.SceneDto create(Dtos.NewSceneRequest request) {
        if (request == null || request.unit() == null) {
            throw new IllegalArgumentException("Indiquer la largeur, la hauteur et l'unité de la carte");
        }
        return Dtos.SceneDto.of(session.create(request.width(), request.height(), request.unit()), session);
    }

    @GET
    public Dtos.SceneDto get() {
        return Dtos.SceneDto.of(requireScene(), session);
    }

    @POST
    @Path("/tokens")
    @ResponseStatus(201)
    public Dtos.TokenDto place(Dtos.PlaceTokenRequest request) {
        requireScene();
        if (request == null || request.catalogId() == null) {
            throw new IllegalArgumentException("Choisir une créature du catalogue");
        }
        var entry = SceneSession.catalogEntry(request.catalogId())
                .orElseThrow(() -> new NotFoundException("Créature inconnue au catalogue : " + request.catalogId()));
        var token = session.place(entry, request.x(), request.y());
        var creature = session.creature(token.entityId()).orElseThrow();
        return new Dtos.TokenDto(token.id(), token.entityId(), creature.name(), entry.kind(),
                token.position().x(), token.position().y());
    }

    private Scene requireScene() {
        return session.scene().orElseThrow(() ->
                new ClientErrorException("Aucune carte : créer d'abord une carte", Response.Status.CONFLICT));
    }
}
