package dnd.server;

import dnd.engine.sheet.Ability;
import dnd.engine.sheet.AbilityRoll;
import dnd.engine.sheet.Build;
import dnd.engine.sheet.CharacterClass;
import dnd.engine.sheet.SheetEvent;
import dnd.engine.sheet.SheetHistory;
import dnd.engine.sheet.SheetState;
import dnd.engine.sheet.Species;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import solovtt.engine.Dice;
import solovtt.module.ModuleData;
import solovtt.module.NotebookPages;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

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
    private final Dice dice;

    public SheetResource(ModuleData data, NotebookPages pages, Dice dice) {
        this.data = data;
        this.pages = pages;
        this.store = new SheetStore(data, MODULE_ID);
        this.dice = dice;
    }

    /** Les options de chaque classe pour la création guidée. */
    @GET
    @Path("/classes")
    @APIResponse(responseCode = "200", description = "Les options de chaque classe",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(type = SchemaType.ARRAY, implementation = DndDtos.ClassOptionsDto.class)))
    @APIResponse(responseCode = "404", description = "Campagne inconnue")
    public List<DndDtos.ClassOptionsDto> classes(@PathParam("campaignId") String campaignId) {
        known(campaignId);
        return Arrays.stream(CharacterClass.values()).map(DndDtos.ClassOptionsDto::of).toList();
    }

    /** Les espèces et leurs sous-choix pour la création guidée, communes à toutes les classes. */
    @GET
    @Path("/species")
    @APIResponse(responseCode = "200", description = "Les espèces et leurs sous-choix",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(type = SchemaType.ARRAY, implementation = DndDtos.SpeciesOptionsDto.class)))
    @APIResponse(responseCode = "404", description = "Campagne inconnue")
    public List<DndDtos.SpeciesOptionsDto> species(@PathParam("campaignId") String campaignId) {
        known(campaignId);
        return Arrays.stream(Species.values()).map(DndDtos.SpeciesOptionsDto::of).toList();
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
        return emit(campaignId, store.read(campaignId), pageId, new SheetEvent.SheetCreated());
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
        return emit(campaignId, state, pageId, new SheetEvent.SheetEdited(values.toSheet()));
    }

    /**
     * Applique ces choix guidés à la fiche : seuls les champs dont la valeur dérivée change depuis la construction
     * précédente sont réécrits, si bien qu'une retouche survit tant que son choix ne change pas.
     */
    @PUT
    @Path("/sheets/{pageId}/build")
    @APIResponse(responseCode = "200", description = "La fiche construite, ses valeurs, leurs dérivés et sa construction",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DndDtos.SheetDto.class)))
    @APIResponse(responseCode = "400", description = "Construction impossible ou incomplète, avec la raison ; rien n'est écrit")
    @APIResponse(responseCode = "404", description = "Campagne inconnue, ou page qui n'est pas une fiche de la campagne")
    @APIResponse(responseCode = "409", description = "Fiches de personnage illisibles")
    public synchronized DndDtos.SheetDto build(@PathParam("campaignId") String campaignId, @PathParam("pageId") String pageId,
                                               DndDtos.BuildDto request) {
        known(campaignId);
        SheetStore.State state = store.read(campaignId);
        SheetState sheet = sheet(campaignId, state, pageId);
        if (request == null) throw new IllegalArgumentException("Une construction porte ses choix");
        Build build = request.toBuild();
        build.validate(sheet.rolls());
        return emit(campaignId, state, pageId, new SheetEvent.SheetBuilt(build, sheet.rebuild(build)));
    }

    /** Lance les six jets de caractéristique de la fiche, quatre d6 chacun : une seule fois par fiche. */
    @POST
    @Path("/sheets/{pageId}/ability-rolls")
    @APIResponse(responseCode = "200", description = "La fiche et ses six jets",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DndDtos.SheetDto.class)))
    @APIResponse(responseCode = "404", description = "Campagne inconnue, ou page qui n'est pas une fiche de la campagne")
    @APIResponse(responseCode = "409", description = "Dés déjà lancés pour cette fiche, ou fiches de personnage illisibles")
    public synchronized DndDtos.SheetDto rollAbilities(@PathParam("campaignId") String campaignId,
                                                       @PathParam("pageId") String pageId) {
        known(campaignId);
        SheetStore.State state = store.read(campaignId);
        if (sheet(campaignId, state, pageId).rolls() != null) {
            throw new ClientErrorException("Les dés de caractéristique de cette fiche sont déjà lancés", Response.Status.CONFLICT);
        }
        List<AbilityRoll> rolls = Stream.generate(() -> AbilityRoll.roll(dice)).limit(Ability.values().length).toList();
        return emit(campaignId, state, pageId, new SheetEvent.AbilitiesRolled(rolls));
    }

    /** Ajoute l'événement à l'histoire de la fiche, l'écrit et rend la fiche repliée. */
    private DndDtos.SheetDto emit(String campaignId, SheetStore.State state, String pageId, SheetEvent event) {
        SheetStore.State next = state.withEvent(pageId, event);
        SheetState sheet = SheetHistory.replay(next.history(pageId).orElseThrow());
        store.write(campaignId, next);
        return DndDtos.SheetDto.of(pageId, sheet);
    }

    /**
     * La fiche de cette page, pliée de son histoire ; 404 si la page n'est pas une fiche vivante du module, 409 si son
     * histoire ne se relit pas. Une entrée sans histoire (écriture échouée après la création de sa page) est une fiche vide.
     */
    private SheetState sheet(String campaignId, SheetStore.State state, String pageId) {
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
