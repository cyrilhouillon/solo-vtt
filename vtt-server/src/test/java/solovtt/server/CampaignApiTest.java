package solovtt.server;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Campagne A, carte de 30 × 20 m : Gobelin (12 ; 1,5), Magicienne au contact (12 ; 3), Loup (25 ; 15).
 * Initiative : Gobelin 15, Magicienne 2, Loup 1. Un d20 de 10 donne 14 au Cimeterre du Gobelin : touche la CA 12 de la
 * Magicienne, et Bouclier est proposé. Les campagnes vivent le temps de la JVM : chaque test crée les siennes.
 */
@QuarkusTest
class CampaignApiTest {

    @Inject
    ScriptedDice dice;

    private static String creer(String name) {
        return given().contentType(ContentType.JSON).body(Map.of("name", name))
                .post("/api/campaigns").then().statusCode(201).body("name", equalTo(name.strip())).extract().path("id");
    }

    private static void poser(String campagne, String catalogId, double x, double y) {
        given().contentType(ContentType.JSON).body("{\"catalogId\":\"%s\",\"x\":%s,\"y\":%s}".formatted(catalogId, x, y))
                .post(campagne + "/scene/tokens").then().statusCode(201);
    }

    @Test
    void creer_puisLister_parNom() {
        String suffixe = UUID.randomUUID().toString();
        String b = creer("  b " + suffixe + "  ");
        String a = creer("A " + suffixe);

        List<String> ids = given().get("/api/campaigns").then().statusCode(200).extract().jsonPath().getList("id");
        assertEquals(List.of(a, b), ids.stream().filter(id -> id.equals(a) || id.equals(b)).toList());
    }

    @Test
    void nomAbsentVideOuTropLong_400() {
        given().contentType(ContentType.JSON).body("{}").post("/api/campaigns").then().statusCode(400);
        given().contentType(ContentType.JSON).body(Map.of("name", "   ")).post("/api/campaigns").then().statusCode(400);
        given().contentType(ContentType.JSON).body(Map.of("name", "x".repeat(81))).post("/api/campaigns").then().statusCode(400);
    }

    @Test
    void uneCampagneInconnue_404() {
        given().get("/api/campaigns/inconnue/scene").then().statusCode(404);
        given().get("/api/campaigns/inconnue/maps").then().statusCode(404);
        given().post("/api/campaigns/inconnue/combat").then().statusCode(404);
    }

    @Test
    void uneCampagneNeuve_naNiCarteNiCombatNiCarteEnregistree() {
        String c = "/api/campaigns/" + creer("Neuve " + UUID.randomUUID());

        given().get(c + "/scene").then().statusCode(409);
        given().get(c + "/combat").then().statusCode(204);
        given().get(c + "/journal").then().statusCode(200).body("$", empty());
        given().get(c + "/maps").then().statusCode(200).body("$", empty());
    }

    @Test
    void deuxCampagnes_jouentCoteACote_sansSeMarcherDessus() {
        String a = "/api/campaigns/" + creer("Phandelver " + UUID.randomUUID());
        String b = "/api/campaigns/" + creer("Strahd " + UUID.randomUUID());
        given().contentType(ContentType.JSON).body("{\"width\":30,\"height\":20,\"unit\":\"METRE\"}").post(a + "/scene").then().statusCode(201);
        poser(a, "gobelin", 12, 1.5);
        poser(a, "magicienne", 12, 3);
        poser(a, "loup", 25, 15);
        given().contentType(ContentType.JSON).body(Map.of("name", "Crypte")).post(a + "/maps").then().statusCode(201);
        dice.script(15, 2, 1, 10);
        given().post(a + "/combat").then().statusCode(200);
        String decision = given().contentType(ContentType.JSON)
                .body("{\"attackerId\":\"gobelin-1\",\"attackId\":\"melee:Cimeterre\",\"targetId\":\"magicienne-1\"}")
                .post(a + "/attacks").then().statusCode(200).extract().path("decision.id");
        List<Object> journal = given().get(a + "/journal").then().extract().jsonPath().getList("$");

        given().get(b + "/scene").then().statusCode(409);
        given().get(b + "/combat").then().statusCode(204);
        given().get(b + "/decisions/pending").then().statusCode(204);
        given().get(b + "/maps").then().statusCode(200).body("name", not(hasItem("Crypte")));
        given().contentType(ContentType.JSON).body("{\"width\":10,\"height\":10,\"unit\":\"METRE\"}").post(b + "/scene").then().statusCode(201);

        given().get(a + "/scene").then().statusCode(200)
                .body("width", equalTo(30.0f))
                .body("tokens.id", equalTo(List.of("gobelin-1", "magicienne-1", "loup-1")));
        given().get(a + "/combat").then().statusCode(200).body("activeId", equalTo("gobelin-1"));
        given().get(a + "/decisions/pending").then().statusCode(200).body("id", equalTo(decision));
        assertEquals(journal, given().get(a + "/journal").then().extract().jsonPath().getList("$"));
        given().get(a + "/maps").then().statusCode(200).body("name", equalTo(List.of("Crypte")));

        dice.script(3);
        given().contentType(ContentType.JSON).body("{\"option\":\"Ne rien faire\"}")
                .post(a + "/decisions/" + decision).then().statusCode(200);
        given().get(a + "/creatures/magicienne-1").then().body("hitPoints", equalTo(2));
    }
}
