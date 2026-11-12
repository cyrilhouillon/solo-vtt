package dnd.server;

import dnd.engine.sheet.CharacterSheet;
import dnd.engine.sheet.SheetEvent;
import dnd.engine.sheet.SheetHistory;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import solovtt.module.ModuleData;
import solovtt.module.NotebookPages;

import java.util.List;

/**
 * Les fiches des personnages joueurs d'une campagne. Chaque fiche est une entrée du module dans le Carnet, rangée sous la
 * page « Personnages joueurs », racine de la section du module : le nom du personnage est le titre de sa page, et sa
 * fiche est l'histoire de ses événements ({@link SheetEvent}), gardée sous l'id de la page.
 */
@Path("/api/campaigns/{campaignId}/dnd")
public class SheetResource {

    static final String MODULE_ID = "dnd";
    static final String PLAYERS_TITLE = "Personnages joueurs";
    static final int MAX_TITLE_LENGTH = 120;

    private final ModuleData data;
    private final NotebookPages pages;
    private final SheetStore store;

    public SheetResource(ModuleData data, NotebookPages pages) {
        this.data = data;
        this.pages = pages;
        this.store = new SheetStore(data, MODULE_ID);
    }

    /**
     * Assure la page « Personnages joueurs » de la section du module : appelé à chaque ouverture du Carnet, il ne la crée
     * que si elle manque. Gardée, elle est retrouvée par son id, même renommée ; sinon une racine de la section titrée
     * « Personnages joueurs » est reprise, sinon la page est créée.
     */
    @POST
    @Path("/sections")
    @APIResponse(responseCode = "200", description = "L'id de la page Personnages joueurs",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DndDtos.SectionsDto.class)))
    @APIResponse(responseCode = "404", description = "Campagne inconnue")
    @APIResponse(responseCode = "409", description = "Fiches de personnage illisibles")
    public synchronized DndDtos.SectionsDto ensureSections(@PathParam("campaignId") String campaignId) {
        known(campaignId);
        return new DndDtos.SectionsDto(playersPage(campaignId));
    }

    /** Crée un personnage sous ce nom : une entrée sous « Personnages joueurs », et sa fiche vide. */
    @POST
    @Path("/sheets")
    @APIResponse(responseCode = "200", description = "La fiche créée, vide",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DndDtos.SheetDto.class)))
    @APIResponse(responseCode = "400", description = "Nom vide ou trop long")
    @APIResponse(responseCode = "404", description = "Campagne inconnue")
    @APIResponse(responseCode = "409", description = "Fiches de personnage illisibles")
    public synchronized DndDtos.SheetDto create(@PathParam("campaignId") String campaignId, DndDtos.NewSheetRequest request) {
        known(campaignId);
        String title = title(request == null ? null : request.title());
        String parent = playersPage(campaignId);
        String pageId = pages.createEntry(campaignId, MODULE_ID, parent, title).id();
        SheetEvent created = new SheetEvent.SheetCreated();
        store.write(campaignId, store.read(campaignId).withEvent(pageId, created));
        return DndDtos.SheetDto.of(pageId, SheetHistory.replay(List.of(created)));
    }

    @GET
    @Path("/sheets/{pageId}")
    @APIResponse(responseCode = "200", description = "La fiche, ses valeurs et leurs dérivés",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DndDtos.SheetDto.class)))
    @APIResponse(responseCode = "404", description = "Campagne inconnue, ou page qui n'est pas une fiche de la campagne")
    @APIResponse(responseCode = "409", description = "Fiches de personnage illisibles")
    public synchronized DndDtos.SheetDto read(@PathParam("campaignId") String campaignId, @PathParam("pageId") String pageId) {
        known(campaignId);
        return DndDtos.SheetDto.of(pageId, sheet(campaignId, store.read(campaignId), pageId));
    }

    /** Remplace les valeurs saisies de la fiche : un événement de plus dans son histoire. */
    @PUT
    @Path("/sheets/{pageId}")
    @APIResponse(responseCode = "200", description = "La fiche saisie, ses valeurs et leurs dérivés",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DndDtos.SheetDto.class)))
    @APIResponse(responseCode = "400", description = "Valeur hors bornes, avec la raison")
    @APIResponse(responseCode = "404", description = "Campagne inconnue, ou page qui n'est pas une fiche de la campagne")
    @APIResponse(responseCode = "409", description = "Fiches de personnage illisibles")
    public synchronized DndDtos.SheetDto edit(@PathParam("campaignId") String campaignId, @PathParam("pageId") String pageId,
                                              DndDtos.SheetValuesDto values) {
        known(campaignId);
        SheetStore.State state = store.read(campaignId);
        sheet(campaignId, state, pageId);
        if (values == null) throw new IllegalArgumentException("Une saisie porte les valeurs de la fiche");
        CharacterSheet edited = values.toSheet();
        store.write(campaignId, state.withEvent(pageId, new SheetEvent.SheetEdited(edited)));
        return DndDtos.SheetDto.of(pageId, edited);
    }

    /**
     * La fiche de cette page, pliée de son histoire ; 404 si la page n'est pas une fiche vivante du module, 409 si son
     * histoire ne se relit pas. Une entrée sans histoire (écriture échouée après la création de sa page) est une fiche vide.
     */
    private CharacterSheet sheet(String campaignId, SheetStore.State state, String pageId) {
        if (!pages.isEntry(campaignId, MODULE_ID, pageId)) throw new NotFoundException("Fiche inconnue : " + pageId);
        try {
            return SheetHistory.replay(state.history(pageId).orElse(List.of(new SheetEvent.SheetCreated())));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw SheetStore.unreadable();
        }
    }

    /** L'id de la page « Personnages joueurs », gardé ou retrouvé, sinon créée ; l'appelant tient le verrou. */
    private String playersPage(String campaignId) {
        SheetStore.State state = store.read(campaignId);
        List<NotebookPages.SectionPage> roots = pages.sectionRoots(campaignId, MODULE_ID);
        String kept = state.playersPageId();
        String id = roots.stream().map(NotebookPages.SectionPage::id).filter(root -> root.equals(kept)).findFirst()
                .or(() -> roots.stream().filter(root -> root.title().equals(PLAYERS_TITLE)).map(NotebookPages.SectionPage::id).findFirst())
                .orElseGet(() -> pages.createSectionRoot(campaignId, MODULE_ID, PLAYERS_TITLE, null).id());
        if (!id.equals(kept)) store.write(campaignId, state.withPlayersPage(id));
        return id;
    }

    /** Le nom d'un personnage, nettoyé comme un titre de page. */
    private static String title(String requested) {
        String title = requested == null ? "" : requested.strip();
        if (title.isEmpty()) throw new IllegalArgumentException("Donner un nom au personnage");
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Nom trop long (" + MAX_TITLE_LENGTH + " caractères au plus)");
        }
        return title;
    }

    private void known(String campaignId) {
        if (!data.exists(campaignId)) throw new NotFoundException("Campagne inconnue : " + campaignId);
    }
}
