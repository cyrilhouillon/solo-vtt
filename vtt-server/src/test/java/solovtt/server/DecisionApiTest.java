package solovtt.server;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Carte de 30 × 20 m : Guerrière (1,5 ; 1,5), Gobelin (12 ; 1,5), Magicienne au contact du Gobelin (12 ; 3).
 * Un d20 de 10 donne 14 au Cimeterre : touche la CA 12 de la Magicienne, rate la CA 17 qu'aurait Bouclier.
 */
@QuarkusTest
class DecisionApiTest {

    @Inject
    ScriptedDice dice;

    @BeforeEach
    void scene() {
        nouvelleCarte();
        poser("guerriere", 1.5, 1.5);
        poser("gobelin", 12, 1.5);
        poser("magicienne", 12, 3);
    }

    private static void nouvelleCarte() {
        given().contentType(ContentType.JSON).body("{\"width\":30,\"height\":20,\"unit\":\"METRE\"}")
                .post("/api/scene").then().statusCode(201);
    }

    private static ValidatableResponse poser(String catalogId, double x, double y) {
        return given().contentType(ContentType.JSON).body("{\"catalogId\":\"%s\",\"x\":%s,\"y\":%s}".formatted(catalogId, x, y))
                .post("/api/scene/tokens").then();
    }

    private static ValidatableResponse leGobelinFrappeLaMagicienne() {
        return given().contentType(ContentType.JSON)
                .body("{\"attackerId\":\"gobelin-1\",\"attackId\":\"melee:Cimeterre\",\"targetId\":\"magicienne-1\"}")
                .post("/api/attacks").then();
    }

    private static ValidatableResponse repondre(String decisionId, String option) {
        return given().contentType(ContentType.JSON).body("{\"option\":\"%s\"}".formatted(option))
                .post("/api/decisions/" + decisionId).then();
    }

    private static List<String> journal() {
        return given().get("/api/journal").then().statusCode(200).extract().jsonPath().getList("text", String.class);
    }

    /** Lance l'attaque qui pose la décision Bouclier ; rend l'id de la décision. */
    private String bouclierPropose(int... desApresLeD20) {
        int[] rolls = new int[desApresLeD20.length + 1];
        rolls[0] = 10;
        System.arraycopy(desApresLeD20, 0, rolls, 1, desApresLeD20.length);
        dice.script(rolls);
        return leGobelinFrappeLaMagicienne().statusCode(200).extract().path("decision.id");
    }

    @Test
    void uneAttaqueQueBouclierFeraitRater_poseLaDecision() {
        dice.script(10);

        leGobelinFrappeLaMagicienne().statusCode(200)
                .body("decision.id", notNullValue())
                .body("decision.chooserId", equalTo("magicienne-1"))
                .body("decision.chooserName", equalTo("Magicienne"))
                .body("decision.prompt", equalTo("Coup au but sur Magicienne (14 contre CA 12). Utiliser sa réaction ?"))
                .body("decision.options", equalTo(List.of("Lancer Bouclier", "Ne rien faire")))
                .body("entries.text", hasItem("d20 : 10"))
                .body("entries.text", not(hasItem(containsString("subit"))))
                .body("targetId", equalTo("magicienne-1"))
                .body("targetHitPoints", equalTo(7));
    }

    @Test
    void laDecisionEnAttenteSeRelit_puisDisparaitApresLaReponse() {
        String id = bouclierPropose();

        given().get("/api/decisions/pending").then().statusCode(200)
                .body("id", equalTo(id))
                .body("options", hasItem("Ne rien faire"));

        repondre(id, "Lancer Bouclier").statusCode(200);

        given().get("/api/decisions/pending").then().statusCode(204);
    }

    @Test
    void lancerBouclier_faitRaterLAttaque() {
        String id = bouclierPropose();

        repondre(id, "Lancer Bouclier").statusCode(200)
                .body("entries.text", hasItem("Magicienne utilise sa réaction : Bouclier"))
                .body("entries.text", hasItem("CA de Magicienne : +5 (Bouclier) → 17"))
                .body("entries.text", not(hasItem(containsString("subit"))))
                .body("entries.find { it.text.endsWith('Bouclier') }.highlight", equalTo("REACTION"))
                .body("entries.find { it.text.startsWith('Attaque de') }.highlight", equalTo("OUTCOME"))
                .body("entries.action", everyItem(equalTo(1)))
                .body("decision", nullValue())
                .body("targetHitPoints", equalTo(7))
                .body("targetMaxHitPoints", equalTo(7));

        given().get("/api/creatures/magicienne-1").then().body("hitPoints", equalTo(7)).body("armorClass", equalTo(17));
    }

    @Test
    void renoncerABouclier_leCoupPorte() {
        String id = bouclierPropose(3);

        repondre(id, "Ne rien faire").statusCode(200)
                .body("entries.text", hasItem("Magicienne subit 5 dégâts tranchants (PV 2/7)"))
                .body("decision", nullValue())
                .body("targetHitPoints", equalTo(2));
    }

    @Test
    void pendantLaDecision_attaquePoseEtRetraitSontRefuses() {
        bouclierPropose();
        List<String> avant = journal();

        leGobelinFrappeLaMagicienne().statusCode(409).body("message", containsString("Une décision attend une réponse"));
        poser("loup", 20, 10).statusCode(409).body("message", containsString("Une décision attend une réponse"));
        given().delete("/api/scene/tokens/guerriere-1").then().statusCode(409);

        assertEquals(avant, journal());
        given().get("/api/scene").then().statusCode(200).body("tokens.id", equalTo(List.of("guerriere-1", "gobelin-1", "magicienne-1")));
    }

    @Test
    void uneNouvelleCarteEffaceLaDecision() {
        bouclierPropose();

        nouvelleCarte();

        given().get("/api/decisions/pending").then().statusCode(204);
        given().get("/api/journal").then().body("$", empty());
    }

    @Test
    void repondreAUneDecisionInconnue_rend404() {
        repondre("d999", "Ne rien faire").statusCode(404);

        String id = bouclierPropose();
        repondre(id + "x", "Ne rien faire").statusCode(404);
    }

    @Test
    void uneOptionAbsente_rend400_etLaDecisionResteEnAttente() {
        String id = bouclierPropose();

        repondre(id, "Fuir").statusCode(400).body("message", containsString("Fuir"));

        given().get("/api/decisions/pending").then().statusCode(200).body("id", equalTo(id));
    }

    @Test
    void pendantUneDecision_laGrilleSeBasculeQuandMeme() {
        bouclierPropose();

        given().contentType(ContentType.JSON).body("{\"visible\":false}").put("/api/scene/grid").then()
                .statusCode(200).body("gridVisible", equalTo(false));
    }
}
