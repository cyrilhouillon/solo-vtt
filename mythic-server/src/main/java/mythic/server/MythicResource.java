package mythic.server;

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

import java.util.Arrays;
import java.util.List;

/**
 * L'oracle Mythic d'une campagne : son facteur de chaos, gardé dans les données du module, et les questions oui/non à
 * la table de destin, dont un événement aléatoire tire son sens dans les tables du joueur s'il les a importées. Le jet
 * ne touche ni la partie ni son journal ; le client écrit le résultat dans la page ouverte.
 */
@Path("/api/campaigns/{campaignId}/mythic")
public class MythicResource {

    static final String MODULE_ID = "mythic";
    static final int MAX_QUESTION_LENGTH = 500;

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Ce que Mythic garde dans une campagne. */
    record State(int chaos) {}

    private final ModuleData data;
    private final MeaningStore meanings;
    private final Dice dice;

    public MythicResource(ModuleData data, MeaningStore meanings, Dice dice) {
        this.data = data;
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
        try {
            data.write(campaignId, MODULE_ID, JSON.writeValueAsString(new State(chaos.value())));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return new MythicDtos.ChaosDto(chaos.value());
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
        return data.read(campaignId, MODULE_ID).map(json -> {
            try {
                return ChaosFactor.of(JSON.readValue(json, State.class).chaos());
            } catch (JsonProcessingException | IllegalArgumentException e) {
                // Un fichier abîmé ne bloque pas l'oracle : il repart du chaos de départ, et sera remplacé au prochain réglage.
                return ChaosFactor.INITIAL;
            }
        }).orElse(ChaosFactor.INITIAL);
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
