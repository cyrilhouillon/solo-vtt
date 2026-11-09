package oracle.server;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import solovtt.engine.Dice;
import solovtt.module.ModuleData;

/**
 * L'oracle générique d'une campagne, sans marque : une question oui/non à chances égales, ou un d100. Les dés sont tirés
 * au serveur ; le jet ne touche ni la partie ni son journal, et le client l'écrit dans la page ouverte.
 */
@Path("/api/campaigns/{campaignId}/oracle")
public class OracleResource {

    static final String MODULE_ID = "oracle";
    static final int MAX_QUESTION_LENGTH = 500;
    /** Oui de 1 à 50 sur le d100, non de 51 à 100 : chances égales. */
    static final int YES_UP_TO = 50;

    private final ModuleData data;
    private final Dice dice;

    public OracleResource(ModuleData data, Dice dice) {
        this.data = data;
        this.dice = dice;
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
