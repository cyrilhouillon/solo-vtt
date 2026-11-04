package solovtt.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.io.UncheckedIOException;

/** Le Carnet d'une campagne : ses pages, leur contenu et la dernière page consultée. */
public class NotebookResource {

    static final int MAX_TITLE_LENGTH = 120;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final NotebookStore notebook;

    public NotebookResource(NotebookStore notebook) {
        this.notebook = notebook;
    }

    @GET
    @APIResponse(responseCode = "200", description = "Les pages, triées par titre, et la dernière page consultée",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.NotebookDto.class)))
    public Dtos.NotebookDto get() {
        return new Dtos.NotebookDto(notebook.list().stream().map(Dtos.PageSummaryDto::of).toList(),
                notebook.lastPage().orElse(null));
    }

    /** Crée une page vide sous ce titre ; deux pages peuvent porter le même titre. */
    @POST
    @Path("/pages")
    @ResponseStatus(201)
    @APIResponse(responseCode = "201", description = "La page créée, sans contenu",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.PageDto.class)))
    @APIResponse(responseCode = "400", description = "Titre absent, vide ou trop long")
    public Dtos.PageDto create(Dtos.PageTitleRequest request) {
        String title = request == null || request.title() == null ? "" : request.title().strip();
        if (title.isEmpty()) throw new IllegalArgumentException("Donner un titre à la page");
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Titre trop long (" + MAX_TITLE_LENGTH + " caractères au plus)");
        }
        return dto(notebook.create(title, null));
    }

    @GET
    @Path("/pages/{pageId}")
    @APIResponse(responseCode = "200", description = "La page et son contenu",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.PageDto.class)))
    @APIResponse(responseCode = "404", description = "Aucune page avec cet id")
    public Dtos.PageDto page(@PathParam("pageId") String pageId) {
        return dto(known(pageId));
    }

    /** Remplace le contenu de la page par le document de l'éditeur, gardé tel quel. */
    @PUT
    @Path("/pages/{pageId}/content")
    @APIResponse(responseCode = "204", description = "Contenu enregistré")
    @APIResponse(responseCode = "400", description = "Contenu absent ou qui n'est pas un document")
    @APIResponse(responseCode = "404", description = "Aucune page avec cet id")
    public void saveContent(@PathParam("pageId") String pageId, Dtos.PageContentRequest request) {
        known(pageId);
        JsonNode content = request == null ? null : request.content();
        if (content == null || !content.isObject() || !"doc".equals(content.path("type").asText())) {
            throw new IllegalArgumentException("Le contenu doit être un document de l'éditeur (type « doc »)");
        }
        notebook.saveContent(pageId, content.toString());
    }

    @PUT
    @Path("/last-page")
    @APIResponse(responseCode = "204", description = "Dernière page notée")
    @APIResponse(responseCode = "400", description = "Page absente")
    @APIResponse(responseCode = "404", description = "Aucune page avec cet id")
    public void setLastPage(Dtos.LastPageRequest request) {
        if (request == null || request.pageId() == null) throw new IllegalArgumentException("Donner la page consultée");
        notebook.setLastPage(known(request.pageId()).id());
    }

    /** {@code ErrorMapper} rend 400 pour une {@code IllegalArgumentException} : une page inconnue se lève ici en 404. */
    private Page known(String pageId) {
        return notebook.find(pageId).orElseThrow(() -> new NotFoundException("Aucune page avec l'id " + pageId));
    }

    private static Dtos.PageDto dto(Page page) {
        try {
            return new Dtos.PageDto(page.id(), page.title(), page.content() == null ? null : JSON.readTree(page.content()));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Contenu de page illisible : " + page.id(), e);
        }
    }
}
