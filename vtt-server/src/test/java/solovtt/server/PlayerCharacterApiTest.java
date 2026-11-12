package solovtt.server;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static solovtt.server.Campagnes.campagne;

/**
 * Aria, Guerrière créée en guidé (équipement A, Défense : CA 17, 11 PV, Épée à deux mains +4), posée depuis le catalogue
 * de sa campagne. Sa copie est prise à la pose : la fiche peut changer ensuite sans toucher la créature posée.
 */
@QuarkusTest
class PlayerCharacterApiTest {

    @Inject
    ScriptedDice dice;

    @Inject
    CampaignStore campaigns;

    private String campaignId;
    private String aria;

    @BeforeEach
    void aria() {
        campaignId = Campagnes.creerUneNeuve();
        when().post(campagne() + "/dnd/sections").then().statusCode(200);
        aria = given().contentType(ContentType.JSON).body(Map.of("title", "Aria"))
                .post(campagne() + "/dnd/sheets").then().statusCode(200).extract().path("pageId");
        Map<String, Object> build = when().get(campagne() + "/dnd/classes").then().statusCode(200)
                .extract().path("[0].defaultBuild");
        given().contentType(ContentType.JSON).body(build).put(campagne() + "/dnd/sheets/" + aria + "/build")
                .then().statusCode(200);
        given().contentType(ContentType.JSON).body("{\"width\":30,\"height\":20,\"unit\":\"METRE\"}")
                .post(campagne() + "/scene").then().statusCode(201);
    }

    private static io.restassured.response.ValidatableResponse poser(String catalogId, double x, double y) {
        return given().contentType(ContentType.JSON)
                .body("{\"catalogId\":\"%s\",\"x\":%s,\"y\":%s}".formatted(catalogId, x, y))
                .post(campagne() + "/scene/tokens").then();
    }

    private Map<String, Object> valeurs() {
        return new HashMap<>(when().get(campagne() + "/dnd/sheets/" + aria).then().statusCode(200).extract().path("values"));
    }

    private void saisir(Map<String, Object> values) {
        given().contentType(ContentType.JSON).body(values).put(campagne() + "/dnd/sheets/" + aria).then().statusCode(200);
    }

    @Test
    void leCatalogue_listeLesPersonnagesDeLaCampagne_puisLesMonstres() {
        when().get(campagne() + "/catalog").then().statusCode(200)
                .body("find { it.id == '%s' }.name".formatted(aria), equalTo("Aria"))
                .body("find { it.id == '%s' }.kind".formatted(aria), equalTo("CHARACTER"))
                .body("id", hasItem("gobelin"));
        when().get("/api/campaigns/" + UUID.randomUUID() + "/catalog").then().statusCode(404);
    }

    @Test
    void unPJPose_joueAvecSaFiche_initiativeDeplacementEtAttaque() {
        Map<String, Object> fiche = valeurs();
        poser(aria, 1.5, 1.5).statusCode(201).body("id", equalTo(aria + "-1")).body("name", equalTo("Aria"))
                .body("kind", equalTo("CHARACTER"));
        poser("gobelin", 3, 1.5).statusCode(201);
        when().get(campagne() + "/creatures/" + aria + "-1").then().statusCode(200)
                .body("armorClass", equalTo(fiche.get("armorClass")))
                .body("maxHitPoints", equalTo(fiche.get("hitPointMax")))
                .body("attacks.label", hasItem("Épée à deux mains"));

        dice.script(20, 1);
        when().post(campagne() + "/combat").then().statusCode(200);
        when().get(campagne() + "/combat").then().statusCode(200).body("activeId", equalTo(aria + "-1"));
        given().contentType(ContentType.JSON).body("{\"x\":1.5,\"y\":12}")
                .post(campagne() + "/scene/tokens/" + aria + "-1/move").then().statusCode(400);
        given().contentType(ContentType.JSON).body("{\"x\":1.5,\"y\":3}")
                .post(campagne() + "/scene/tokens/" + aria + "-1/move").then().statusCode(200);

        dice.script(15, 6, 6);
        given().contentType(ContentType.JSON)
                .body("{\"attackerId\":\"%s-1\",\"attackId\":\"melee:Épée à deux mains\",\"targetId\":\"gobelin-1\"}".formatted(aria))
                .post(campagne() + "/attacks").then().statusCode(200);
        when().get(campagne() + "/creatures/gobelin-1").then().body("hitPoints", equalTo(0));
    }

    @Test
    void retoucherRenommerSupprimerLaFiche_neChangePasLePJPose_niEnRepriseNiEnRejeu() {
        int ca = (int) valeurs().get("armorClass");
        poser(aria, 1.5, 1.5).statusCode(201);
        poser("gobelin", 3, 1.5).statusCode(201);
        dice.script(20, 1);
        when().post(campagne() + "/combat").then().statusCode(200);

        Map<String, Object> retouchee = valeurs();
        retouchee.put("armorClass", 25);
        saisir(retouchee);
        given().contentType(ContentType.JSON).body(Map.of("title", "Aria la Brave"))
                .put(campagne() + "/notebook/pages/" + aria + "/title").then().statusCode(204);
        when().delete(campagne() + "/notebook/pages/" + aria).then().statusCode(204);
        when().post(campagne() + "/combat/end").then().statusCode(200);

        when().get(campagne() + "/creatures/" + aria + "-1").then().body("armorClass", equalTo(ca)).body("name", equalTo("Aria"));
        var reprise = new SceneSession(Dice.scripted(), campaigns.games(campaignId));
        assertEquals(ca, reprise.creature(aria + "-1").orElseThrow().armorClass());
        assertEquals("Aria", reprise.creature(aria + "-1").orElseThrow().name());
        String combatId = (String) ((List<Map<String, Object>>) when().get(campagne() + "/notebook").then().extract().path("pages"))
                .stream().filter(p -> p.get("combatId") != null).findFirst().orElseThrow().get("combatId");
        when().get(campagne() + "/combats/" + combatId + "/replay").then().statusCode(200)
                .body("steps[-1].sheets.find { it.id == '%s-1' }.armorClass".formatted(aria), equalTo(ca))
                .body("steps[-1].sheets.find { it.id == '%s-1' }.name".formatted(aria), equalTo("Aria"));
    }

    @Test
    void sansLeModuleDnd_niPersonnageAuCatalogue_niPose_maisUnPJDejaPoseSeRejoue() {
        poser(aria, 1.5, 1.5).statusCode(201);
        given().contentType(ContentType.JSON).body(Map.of("modules", List.of()))
                .put(campagne() + "/modules").then().statusCode(200);

        when().get(campagne() + "/catalog").then().statusCode(200).body("id", not(hasItem(aria))).body("id", hasItem("gobelin"));
        poser(aria, 6, 6).statusCode(404);
        var reprise = new SceneSession(Dice.scripted(), campaigns.games(campaignId));
        assertEquals("Aria", reprise.creature(aria + "-1").orElseThrow().name());
    }

    @Test
    void uneFicheAZeroPVMax_neSePosePas() {
        Map<String, Object> vide = valeurs();
        vide.put("hitPointMax", 0);
        vide.put("hitPoints", 0);
        saisir(vide);

        poser(aria, 1.5, 1.5).statusCode(400).body("message", equalTo("PV maximum à 0 : compléter la fiche avant de la poser"));
    }

    @Test
    void uneCarteEnregistree_reprendLaFicheAuChargement_etRefuseUneFicheSupprimee() {
        poser(aria, 1.5, 1.5).statusCode(201);
        String nom = "Crypte " + UUID.randomUUID();
        given().contentType(ContentType.JSON).body(Map.of("name", nom)).post(campagne() + "/maps").then().statusCode(201);

        Map<String, Object> retouchee = valeurs();
        retouchee.put("armorClass", 25);
        saisir(retouchee);
        given().contentType(ContentType.JSON).body(Map.of("name", nom)).post(campagne() + "/maps/load").then().statusCode(200);
        when().get(campagne() + "/creatures/" + aria + "-1").then().body("armorClass", equalTo(25));

        when().delete(campagne() + "/notebook/pages/" + aria).then().statusCode(204);
        given().contentType(ContentType.JSON).body(Map.of("name", nom)).post(campagne() + "/maps/load").then().statusCode(409);
        when().get(campagne() + "/creatures/" + aria + "-1").then().statusCode(200).body("armorClass", equalTo(25));
    }
}
