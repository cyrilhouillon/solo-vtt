package solovtt.server;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static solovtt.server.Campagnes.campagne;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Carte de 30 × 20 m, grille masquée : Gobelin (12 ; 1,5), Magicienne au contact (12 ; 3), Loup (25 ; 15).
 * Un d20 de 10 donne 14 au Cimeterre du Gobelin : touche la CA 12 de la Magicienne, et Bouclier est proposé.
 * Les cartes enregistrées vivent le temps de la JVM : chaque test prend un nom à lui.
 */
@QuarkusTest
class MapApiTest {

    @Inject
    ScriptedDice dice;

    private final String nom = "Crypte " + UUID.randomUUID();

    @BeforeEach
    void scene() {
        Campagnes.creerUneNeuve();
        given().contentType(ContentType.JSON).body("{\"width\":30,\"height\":20,\"unit\":\"METRE\"}")
                .post(campagne() + "/scene").then().statusCode(201).body("mapName", nullValue());
        given().contentType(ContentType.JSON).body("{\"visible\":false}").put(campagne() + "/scene/grid").then().statusCode(200);
        poser("gobelin", 12, 1.5);
        poser("magicienne", 12, 3);
        poser("loup", 25, 15);
    }

    private static void poser(String catalogId, double x, double y) {
        given().contentType(ContentType.JSON).body("{\"catalogId\":\"%s\",\"x\":%s,\"y\":%s}".formatted(catalogId, x, y))
                .post(campagne() + "/scene/tokens").then().statusCode(201);
    }

    private static ValidatableResponse enregistrer(String name) {
        return given().contentType(ContentType.JSON).body(Map.of("name", name)).post(campagne() + "/maps").then();
    }

    private static ValidatableResponse charger(String name) {
        return given().contentType(ContentType.JSON).body(Map.of("name", name)).post(campagne() + "/maps/load").then();
    }

    /** Le Gobelin frappe la Magicienne : la décision Bouclier attend ; rend son id. */
    private static String bouclierPropose() {
        return given().contentType(ContentType.JSON)
                .body("{\"attackerId\":\"gobelin-1\",\"attackId\":\"melee:Cimeterre\",\"targetId\":\"magicienne-1\"}")
                .post(campagne() + "/attacks").then().statusCode(200).extract().path("decision.id");
    }

    private static int tailleDuJournal() {
        return given().get(campagne() + "/journal").then().statusCode(200).extract().jsonPath().getList("$").size();
    }

    private String champ(String field) {
        return "find { it.name == '%s' }.%s".formatted(nom, field);
    }

    @Test
    void uneCarteCalee_enregistreePuisChargee_garderSonCalage() {
        String imageId = ImageApiTest.envoyer(ImageApiTest.PNG, "image/png");
        given().contentType(ContentType.JSON)
                .body("{\"width\":30,\"height\":20,\"unit\":\"METRE\",\"imageId\":\"%s\"}".formatted(imageId))
                .post(campagne() + "/scene").then().statusCode(201);
        poser("loup", 3, 4.5);
        given().contentType(ContentType.JSON).body("{\"width\":33,\"height\":22,\"gridOffsetX\":0.25,\"gridOffsetY\":0.5}")
                .put(campagne() + "/scene/alignment").then().statusCode(200);
        enregistrer(nom).statusCode(201);
        given().contentType(ContentType.JSON).body("{\"width\":30,\"height\":20,\"unit\":\"METRE\"}")
                .post(campagne() + "/scene").then().statusCode(201);

        charger(nom).statusCode(200)
                .body("width", equalTo(33.0f))
                .body("height", equalTo(22.0f))
                .body("gridOffsetX", equalTo(0.25f))
                .body("gridOffsetY", equalTo(0.5f))
                .body("tokens[0].x", equalTo(3.3f))
                .body("tokens[0].y", equalTo(4.95f));
    }

    @Test
    void enregistrer_puisLister() {
        enregistrer("  " + nom + "  ").statusCode(201)
                .body("name", equalTo(nom))
                .body("creatureCount", equalTo(3));

        given().get(campagne() + "/maps").then().statusCode(200)
                .body("name", hasItem(nom))
                .body(champ("width"), equalTo(30.0f))
                .body(champ("height"), equalTo(20.0f))
                .body(champ("unitSymbol"), equalTo("m"))
                .body(champ("creatureCount"), equalTo(3));
    }

    @Test
    void enregistrerEnCombat_neToucheNiLeCombatNiLeJournal() {
        dice.script(15, 2, 1);
        given().post(campagne() + "/combat").then().statusCode(200);
        int journal = tailleDuJournal();

        enregistrer(nom).statusCode(201);

        assertEquals(journal, tailleDuJournal());
        given().get(campagne() + "/combat").then().statusCode(200);
    }

    @Test
    void charger_apresUnCombat_redonneDesCreaturesNeuves_sansCombatNiJournal() {
        enregistrer(nom).statusCode(201);
        dice.script(15, 2, 1, 10, 3);
        given().post(campagne() + "/combat").then().statusCode(200);
        given().contentType(ContentType.JSON).body("{\"option\":\"Ne rien faire\"}")
                .post(campagne() + "/decisions/" + bouclierPropose()).then().statusCode(200);
        given().get(campagne() + "/creatures/magicienne-1").then().body("hitPoints", equalTo(2));

        charger(nom).statusCode(200)
                .body("width", equalTo(30.0f))
                .body("height", equalTo(20.0f))
                .body("unit", equalTo("METRE"))
                .body("gridVisible", equalTo(false))
                .body("mapName", equalTo(nom))
                .body("tokens.id", equalTo(List.of("gobelin-1", "magicienne-1", "loup-1")))
                .body("tokens.find { it.id == 'magicienne-1' }.x", equalTo(12.0f))
                .body("tokens.find { it.id == 'magicienne-1' }.y", equalTo(3.0f))
                .body("tokens.find { it.id == 'magicienne-1' }.hitPoints", equalTo(7));

        given().get(campagne() + "/combat").then().statusCode(204);
        given().get(campagne() + "/journal").then().statusCode(200).body("$", empty());
        given().get(campagne() + "/scene").then().statusCode(200).body("mapName", equalTo(nom));
    }

    @Test
    void chargerDeuxFois_redonneLaMemeScene() {
        enregistrer(nom).statusCode(201);

        String premiere = charger(nom).statusCode(200).extract().asString();
        String seconde = charger(nom).statusCode(200).extract().asString();

        assertEquals(premiere, seconde);
    }

    @Test
    void chargerPendantUneDecision_laJette() {
        enregistrer(nom).statusCode(201);
        dice.script(10);
        bouclierPropose();

        charger(nom).statusCode(200);

        given().get(campagne() + "/decisions/pending").then().statusCode(204);
    }

    @Test
    void uneNouvelleCarteApresUnChargement_nAPlusDeNom() {
        enregistrer(nom).statusCode(201);
        charger(nom).statusCode(200);

        given().contentType(ContentType.JSON).body("{\"width\":10,\"height\":10,\"unit\":\"FOOT\"}")
                .post(campagne() + "/scene").then().statusCode(201).body("mapName", nullValue());
    }

    @Test
    void unNomVideOuTropLong_rend400() {
        enregistrer("   ").statusCode(400).body("message", containsString("nom"));
        given().contentType(ContentType.JSON).body("{}").post(campagne() + "/maps").then().statusCode(400);
        enregistrer("x".repeat(MapResource.MAX_NAME_LENGTH + 1)).statusCode(400).body("message", containsString("trop long"));
        charger(" ").statusCode(400);
    }

    @Test
    void chargerUnNomInconnu_rend404_etLaCarteEnCoursReste() {
        charger(nom).statusCode(404).body("message", containsString(nom));

        given().get(campagne() + "/scene").then().statusCode(200).body("tokens", hasSize(3)).body("gridVisible", equalTo(false));
    }
}
