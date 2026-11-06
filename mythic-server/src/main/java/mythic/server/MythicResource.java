package mythic.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import mythic.engine.ChaosFactor;
import mythic.engine.FateQuestion;
import mythic.engine.Odds;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import solovtt.engine.Dice;
import solovtt.module.ModuleData;
import solovtt.module.NotebookPages;

import java.util.Arrays;
import java.util.List;

/**
 * L'oracle Mythic d'une campagne : son facteur de chaos, gardé dans les données du module, et les questions oui/non à
 * la table de destin, dont un événement aléatoire tire son sens dans les tables du joueur s'il les a importées. Le jet
 * ne touche ni la partie ni son journal ; le client écrit le résultat dans la page ouverte. Les listes de fils et de
 * personnages sont deux pages de la section Mythic du Carnet, dont les ids sont gardés avec le chaos.
 */
@Path("/api/campaigns/{campaignId}/mythic")
public class MythicResource {

    static final String MODULE_ID = "mythic";
    static final int MAX_QUESTION_LENGTH = 500;

    private static final ObjectMapper JSON = new ObjectMapper();

    static final String THREADS_TITLE = "Fils";
    static final String CHARACTERS_TITLE = "Personnages";

    /** Une liste à puces vide : une entrée de liste est un élément de liste. */
    static final String EMPTY_LIST = "{\"type\":\"doc\",\"content\":[{\"type\":\"bulletList\",\"content\":"
            + "[{\"type\":\"listItem\",\"content\":[{\"type\":\"paragraph\"}]}]}]}";

    /** Ce que Mythic garde dans une campagne : le chaos ({@code null} : celui de départ) et les pages de ses listes. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record State(Integer chaos, String threadsPageId, String charactersPageId) {
        static final State EMPTY = new State(null, null, null);
    }

    private final ModuleData data;
    private final NotebookPages pages;
    private final MeaningStore meanings;
    private final Dice dice;

    public MythicResource(ModuleData data, NotebookPages pages, MeaningStore meanings, Dice dice) {
        this.data = data;
        this.pages = pages;
        this.meanings = meanings;
        this.dice = dice;
    }

    @GET
    @Path("/odds")
    @APIResponse(responseCode = "200", description = "Les probabilités, de la moins à la plus probable")
    public List<MythicDtos.OddsDto> odds() {
        return Arrays.stream(Odds.values()).map(MythicDtos.OddsDto::of).toList();
    }

    @GET
    @Path("/chaos")
    @APIResponse(responseCode = "200", description = "Le facteur de chaos de la campagne, 5 tant qu'il n'a pas été changé",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MythicDtos.ChaosDto.class)))
    public MythicDtos.ChaosDto chaos(@PathParam("campaignId") String campaignId) {
        return new MythicDtos.ChaosDto(chaosOf(campaignId).value());
    }

    @PUT
    @Path("/chaos")
    @APIResponse(responseCode = "200", description = "Le facteur de chaos gardé",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MythicDtos.ChaosDto.class)))
    @APIResponse(responseCode = "400", description = "Facteur de chaos absent ou hors de 1 à 9")
    public synchronized MythicDtos.ChaosDto setChaos(@PathParam("campaignId") String campaignId, MythicDtos.ChaosDto request) {
        known(campaignId);
        if (request == null) throw new IllegalArgumentException("Facteur de chaos absent");
        ChaosFactor chaos = ChaosFactor.of(request.chaos());
        State state = state(campaignId);
        write(campaignId, new State(chaos.value(), state.threadsPageId(), state.charactersPageId()));
        return new MythicDtos.ChaosDto(chaos.value());
    }

    /**
     * Assure les pages Fils et Personnages de la section Mythic du Carnet : appelé à chaque ouverture du Carnet, il ne
     * crée que celles qui manquent. Une page gardée est retrouvée par son id, même renommée ; si l'id est perdu, une racine
     * de la section qui porte encore son titre est reprise, sinon la page est créée, vide.
     */
    @POST
    @Path("/lists")
    @APIResponse(responseCode = "200", description = "Les ids des pages Fils et Personnages",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MythicDtos.ListsDto.class)))
    @APIResponse(responseCode = "404", description = "Campagne inconnue")
    public synchronized MythicDtos.ListsDto lists(@PathParam("campaignId") String campaignId) {
        known(campaignId);
        State state = state(campaignId);
        List<NotebookPages.SectionPage> roots = pages.sectionRoots(campaignId, MODULE_ID);
        String charactersKept = kept(state.charactersPageId(), roots);
        String threads = ensure(campaignId, state.threadsPageId(), THREADS_TITLE, roots, charactersKept);
        String characters = ensure(campaignId, state.charactersPageId(), CHARACTERS_TITLE, roots, threads);
        if (!threads.equals(state.threadsPageId()) || !characters.equals(state.charactersPageId())) {
            write(campaignId, new State(state.chaos(), threads, characters));
        }
        return new MythicDtos.ListsDto(threads, characters);
    }

    /** L'id gardé s'il est encore une racine de la section, sinon {@code null}. */
    private static String kept(String id, List<NotebookPages.SectionPage> roots) {
        return id != null && roots.stream().anyMatch(page -> page.id().equals(id)) ? id : null;
    }

    /** La page de la liste : gardée, reprise par son titre (hors de l'autre liste), ou créée. */
    private String ensure(String campaignId, String id, String title, List<NotebookPages.SectionPage> roots, String other) {
        String kept = kept(id, roots);
        if (kept != null && !kept.equals(other)) return kept;
        return roots.stream()
                .filter(page -> page.title().equals(title) && !page.id().equals(other))
                .map(NotebookPages.SectionPage::id)
                .findFirst()
                .orElseGet(() -> pages.createSectionRoot(campaignId, MODULE_ID, title, EMPTY_LIST).id());
    }

    /** Pose une question oui/non au chaos de la campagne. */
    @POST
    @Path("/questions")
    @APIResponse(responseCode = "200", description = "La réponse de l'oracle, son jet et l'éventuel événement aléatoire",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MythicDtos.AnswerDto.class)))
    @APIResponse(responseCode = "400", description = "Probabilité absente ou inconnue, ou question trop longue")
    public MythicDtos.AnswerDto ask(@PathParam("campaignId") String campaignId, MythicDtos.QuestionRequest request) {
        ChaosFactor chaos = chaosOf(campaignId);
        Odds odds = odds(request == null ? null : request.odds());
        String question = request.question() == null || request.question().isBlank() ? null : request.question().strip();
        if (question != null && question.length() > MAX_QUESTION_LENGTH)
            throw new IllegalArgumentException("Question trop longue (" + MAX_QUESTION_LENGTH + " caractères au plus)");
        return MythicDtos.AnswerDto.of(question, FateQuestion.ask(odds, chaos, meanings.find(), dice));
    }

    private ChaosFactor chaosOf(String campaignId) {
        known(campaignId);
        Integer chaos = state(campaignId).chaos();
        try {
            return chaos == null ? ChaosFactor.INITIAL : ChaosFactor.of(chaos);
        } catch (IllegalArgumentException e) {
            // Un chaos abîmé ne bloque pas l'oracle : il repart de celui de départ, et sera remplacé au prochain réglage.
            return ChaosFactor.INITIAL;
        }
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

    private static Odds odds(String code) {
        if (code == null) throw new IllegalArgumentException("Probabilité absente");
        try {
            return Odds.valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Probabilité inconnue : " + code);
        }
    }

    private void known(String campaignId) {
        if (!data.exists(campaignId)) throw new NotFoundException("Campagne inconnue : " + campaignId);
    }
}
