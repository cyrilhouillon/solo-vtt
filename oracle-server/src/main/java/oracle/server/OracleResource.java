package oracle.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import solovtt.engine.Dice;
import solovtt.module.ModuleData;
import solovtt.module.NotebookPages;

import java.util.List;
import java.util.stream.IntStream;

/**
 * L'oracle générique d'une campagne, sans marque : une question oui/non à chances égales, un d100, ou une ligne tirée
 * dans une des listes du joueur. Les listes sont les sous-pages de la page « Listes », racine de la section de l'oracle
 * dans le Carnet, dont l'id est gardé dans les données du module. Les dés sont tirés au serveur ; le jet ne touche ni la
 * partie ni son journal, et le client l'écrit dans la page ouverte.
 */
@Path("/api/campaigns/{campaignId}/oracle")
public class OracleResource {

    static final String MODULE_ID = "oracle";
    static final int MAX_QUESTION_LENGTH = 500;
    /** Oui de 1 à 50 sur le d100, non de 51 à 100 : chances égales. */
    static final int YES_UP_TO = 50;

    static final String LISTS_TITLE = "Listes";
    /** Une liste à puces vide : une ligne de liste est un élément de liste. */
    static final String EMPTY_LIST = "{\"type\":\"doc\",\"content\":[{\"type\":\"bulletList\",\"content\":"
            + "[{\"type\":\"listItem\",\"content\":[{\"type\":\"paragraph\"}]}]}]}";

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Ce que l'oracle garde dans une campagne : la page racine de ses listes. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record State(String listsPageId) {
        static final State EMPTY = new State(null);
    }

    private final ModuleData data;
    private final NotebookPages pages;
    private final Dice dice;

    public OracleResource(ModuleData data, NotebookPages pages, Dice dice) {
        this.data = data;
        this.pages = pages;
        this.dice = dice;
    }

    /**
     * Assure la page « Listes » de la section de l'oracle : appelé à chaque ouverture du Carnet, il ne la crée que si elle
     * manque. Gardée, elle est retrouvée par son id, même renommée ; sinon une racine de la section titrée « Listes » est
     * reprise, sinon la page est créée, avec une liste à puces vide.
     */
    @POST
    @Path("/lists")
    @APIResponse(responseCode = "200", description = "L'id de la page Listes",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OracleDtos.ListsRootDto.class)))
    @APIResponse(responseCode = "404", description = "Campagne inconnue")
    public synchronized OracleDtos.ListsRootDto ensureLists(@PathParam("campaignId") String campaignId) {
        known(campaignId);
        List<NotebookPages.SectionPage> roots = pages.sectionRoots(campaignId, MODULE_ID);
        String kept = state(campaignId).listsPageId();
        String id = roots.stream().map(NotebookPages.SectionPage::id).filter(root -> root.equals(kept)).findFirst()
                .or(() -> roots.stream().filter(root -> root.title().equals(LISTS_TITLE)).map(NotebookPages.SectionPage::id).findFirst())
                .orElseGet(() -> pages.createSectionRoot(campaignId, MODULE_ID, LISTS_TITLE, EMPTY_LIST).id());
        if (!id.equals(kept)) write(campaignId, new State(id));
        return new OracleDtos.ListsRootDto(id);
    }

    /** Les listes du joueur : les sous-pages vivantes de Listes, triées par titre ; aucune tant que Listes n'existe pas. */
    @GET
    @Path("/lists")
    @APIResponse(responseCode = "200", description = "Les listes, triées par titre")
    @APIResponse(responseCode = "404", description = "Campagne inconnue")
    public List<OracleDtos.ListDto> lists(@PathParam("campaignId") String campaignId) {
        known(campaignId);
        return listPages(campaignId).stream().map(page -> new OracleDtos.ListDto(page.id(), page.title())).toList();
    }

    /** Tire une ligne écrite de la liste, à chances égales ; les lignes vides sont sautées. */
    @POST
    @Path("/lists/{pageId}/draw")
    @APIResponse(responseCode = "200", description = "La ligne tirée, son dé et son numéro dans la liste",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OracleDtos.DrawDto.class)))
    @APIResponse(responseCode = "400", description = "Liste sans ligne écrite")
    @APIResponse(responseCode = "404", description = "Campagne inconnue, ou page qui n'est pas une liste")
    public OracleDtos.DrawDto draw(@PathParam("campaignId") String campaignId, @PathParam("pageId") String pageId) {
        known(campaignId);
        NotebookPages.SectionPage list = listPages(campaignId).stream().filter(page -> page.id().equals(pageId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Liste inconnue : " + pageId));
        List<ListLines.Line> lines = pages.sectionPageContent(campaignId, MODULE_ID, pageId)
                .map(content -> ListLines.of(content, id -> pages.pageTitle(campaignId, id)))
                .orElse(List.of());
        List<Integer> written = IntStream.range(0, lines.size()).filter(i -> lines.get(i).written()).boxed().toList();
        if (written.isEmpty()) throw new IllegalArgumentException("La liste " + list.title() + " est vide : y écrire des lignes");
        int roll = dice.roll(written.size());
        int index = written.get(roll - 1);
        ListLines.Line line = lines.get(index);
        return new OracleDtos.DrawDto(list.id(), list.title(), roll, written.size(), index + 1, line.inline(), line.text());
    }

    /** Les sous-pages de Listes ; aucune si la page gardée n'est plus une racine de la section. */
    private List<NotebookPages.SectionPage> listPages(String campaignId) {
        String root = state(campaignId).listsPageId();
        if (root == null) return List.of();
        return pages.childPages(campaignId, MODULE_ID, root);
    }

    /** L'état gardé ; un fichier abîmé se lit vide, et sera remplacé à la prochaine écriture. */
    private State state(String campaignId) {
        return data.read(campaignId, MODULE_ID).map(json -> {
            try {
                State read = JSON.readValue(json, State.class);
                return read == null ? State.EMPTY : read;
            } catch (JsonProcessingException e) {
                return State.EMPTY;
            }
        }).orElse(State.EMPTY);
    }

    private void write(String campaignId, State state) {
        try {
            data.write(campaignId, MODULE_ID, JSON.writeValueAsString(state));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @POST
    @Path("/yes-no")
    @APIResponse(responseCode = "200", description = "Oui ou non, à chances égales, avec son d100",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OracleDtos.YesNoDto.class)))
    @APIResponse(responseCode = "400", description = "Question trop longue")
    @APIResponse(responseCode = "404", description = "Campagne inconnue")
    public OracleDtos.YesNoDto yesNo(@PathParam("campaignId") String campaignId, OracleDtos.QuestionRequest request) {
        known(campaignId);
        String question = question(request);
        int roll = dice.roll(100);
        return new OracleDtos.YesNoDto(question, roll, roll <= YES_UP_TO);
    }

    @POST
    @Path("/d100")
    @APIResponse(responseCode = "200", description = "Un d100, de 1 à 100",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OracleDtos.D100Dto.class)))
    @APIResponse(responseCode = "400", description = "Question trop longue")
    @APIResponse(responseCode = "404", description = "Campagne inconnue")
    public OracleDtos.D100Dto d100(@PathParam("campaignId") String campaignId, OracleDtos.QuestionRequest request) {
        known(campaignId);
        String question = question(request);
        return new OracleDtos.D100Dto(question, dice.roll(100));
    }

    /** La question du joueur, nettoyée ; vide : {@code null}. */
    private static String question(OracleDtos.QuestionRequest request) {
        String value = request == null ? null : request.question();
        if (value == null || value.isBlank()) return null;
        String question = value.strip();
        if (question.length() > MAX_QUESTION_LENGTH)
            throw new IllegalArgumentException("Question trop longue (" + MAX_QUESTION_LENGTH + " caractères au plus)");
        return question;
    }

    private void known(String campaignId) {
        if (!data.exists(campaignId)) throw new NotFoundException("Campagne inconnue : " + campaignId);
    }
}
