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

    /** Une page dont le parent est illisible, ou qui boucle sur elle-même, est rendue à la racine. */
    @GET
    @APIResponse(responseCode = "200", description = "Les pages triées par titre, avec leur parent, et la dernière consultée",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.NotebookDto.class)))
    public Dtos.NotebookDto get() {
        return new Dtos.NotebookDto(PageTree.rooted(notebook.list()).stream().map(Dtos.PageSummaryDto::of).toList(),
                notebook.lastPage().orElse(null));
    }

    /** Crée une page vide sous ce titre, à la racine ou sous une page ; deux pages peuvent porter le même titre. */
    @POST
    @Path("/pages")
    @ResponseStatus(201)
    @APIResponse(responseCode = "201", description = "La page créée, sans contenu",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.PageDto.class)))
    @APIResponse(responseCode = "400", description = "Titre absent, vide ou trop long")
    @APIResponse(responseCode = "404", description = "Aucune page avec l'id du parent")
    public Dtos.PageDto create(Dtos.CreatePageRequest request) {
        String title = title(request == null ? null : request.title());
        String parentId = request.parentId();
        if (parentId != null) known(parentId);
        return dto(notebook.create(title, parentId));
    }

    @PUT
    @Path("/pages/{pageId}/title")
    @APIResponse(responseCode = "204", description = "Page renommée")
    @APIResponse(responseCode = "400", description = "Titre absent, vide ou trop long")
    @APIResponse(responseCode = "404", description = "Aucune page avec cet id")
    public void rename(@PathParam("pageId") String pageId, Dtos.PageTitleRequest request) {
        known(pageId);
        notebook.rename(pageId, title(request == null ? null : request.title()));
    }

    /** Range la page sous une autre, ou à la racine ({@code parentId} absent) ; ses sous-pages la suivent. */
    @PUT
    @Path("/pages/{pageId}/parent")
    @APIResponse(responseCode = "204", description = "Page déplacée, avec ses sous-pages")
    @APIResponse(responseCode = "400", description = "Le parent est la page elle-même ou l'une de ses sous-pages")
    @APIResponse(responseCode = "404", description = "Aucune page avec cet id, ou avec l'id du parent")
    public void move(@PathParam("pageId") String pageId, Dtos.ParentRequest request) {
        known(pageId);
        String parentId = request == null ? null : request.parentId();
        if (parentId != null) known(parentId);
        notebook.move(pageId, parentId);
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

    /** Le titre nettoyé, à la création comme au renommage. */
    private static String title(String requested) {
        String title = requested == null ? "" : requested.strip();
        if (title.isEmpty()) throw new IllegalArgumentException("Donner un titre à la page");
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Titre trop long (" + MAX_TITLE_LENGTH + " caractères au plus)");
        }
        return title;
    }

    /** {@code ErrorMapper} rend 400 pour une {@code IllegalArgumentException} : une page inconnue se lève ici en 404. */
    private Page known(String pageId) {
        return notebook.find(pageId).orElseThrow(() -> new NotFoundException("Aucune page avec l'id " + pageId));
    }

    private static Dtos.PageDto dto(Page page) {
        try {
            JsonNode content = page.content() == null ? null : JSON.readTree(page.content());
            return new Dtos.PageDto(page.id(), page.title(), page.parentId(), content);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Contenu de page illisible : " + page.id(), e);
        }
    }
}
