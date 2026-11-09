package solovtt.server;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static solovtt.server.Campagnes.campagne;

/**
 * Les combats d'une campagne et leurs entrées du Carnet. Carte de 30 × 20 m : Guerrière (+2) au contact du Gobelin
 * guerrier (+2). Initiative : d20 de 10 et 15.
 */
@QuarkusTest
class CombatsApiTest {

    @Inject
    ScriptedDice dice;

    @BeforeEach
    void carte() {
        Campagnes.creerUneNeuve();
        nouvelleCarte();
        poser("guerriere", 1.5, 1.5);
        poser("gobelin", 3, 1.5);
    }

    private static void nouvelleCarte() {
        given().contentType(ContentType.JSON).body("{\"width\":30,\"height\":20,\"unit\":\"METRE\"}")
                .post(campagne() + "/scene").then().statusCode(201);
    }

    private static void poser(String catalogId, double x, double y) {
        given().contentType(ContentType.JSON).body("{\"catalogId\":\"%s\",\"x\":%s,\"y\":%s}".formatted(catalogId, x, y))
                .post(campagne() + "/scene/tokens").then().statusCode(201);
    }

    private void lancer() {
        dice.script(10, 15);
        when().post(campagne() + "/combat").then().statusCode(200);
    }

    private static List<Map<String, Object>> pages() {
        return when().get(campagne() + "/notebook").then().statusCode(200).extract().path("pages");
    }

    /** Les entrées de combat du Carnet. */
    private static List<Map<String, Object>> entrees() {
        return pages().stream().filter(p -> p.get("combatId") != null).toList();
    }

    @Test
    void lancerUnCombat_creeSonEntreeDansLaSectionDesCombats() {
        lancer();
        lancer2();

        List<Map<String, Object>> entrees = entrees();
        assertEquals(List.of("Combat 1", "Combat 2"), entrees.stream().map(p -> p.get("title")).sorted().toList());
        var racine = pages().stream().filter(p -> "combats".equals(p.get("section"))).toList();
        assertEquals(1, racine.size());
        assertEquals("Combats", racine.getFirst().get("title"));
        for (var entree : entrees) {
            assertEquals(racine.getFirst().get("id"), entree.get("parentId"));
            assertNotNull(entree.get("combatId"));
        }
    }

    /** Termine le premier combat puis en lance un second sur la même carte. */
    private void lancer2() {
        when().post(campagne() + "/combat/end").then().statusCode(200);
        lancer();
    }

    @Test
    void uneEntreeDeCombat_nAPasDeTexte_niDeSousPage() {
        lancer();
        String id = (String) entrees().getFirst().get("id");

        given().contentType(ContentType.JSON).body("{\"content\":{\"type\":\"doc\",\"content\":[]}}")
                .put(campagne() + "/notebook/pages/" + id + "/content").then().statusCode(400);
        given().contentType(ContentType.JSON).body(Map.of("title", "Notes", "parentId", id))
                .post(campagne() + "/notebook/pages").then().statusCode(400);
        given().contentType(ContentType.JSON).body(Map.of("title", "Embuscade"))
                .put(campagne() + "/notebook/pages/" + id + "/title").then().statusCode(204);
        when().get(campagne() + "/notebook/pages/" + id).then().statusCode(200)
                .body("title", equalTo("Embuscade"))
                .body("combatId", equalTo(entrees().getFirst().get("combatId")));
        JsonPath trouve = when().get(campagne() + "/notebook/search?q=embuscade").then().statusCode(200).extract().jsonPath();
        assertEquals(id, trouve.getString("[0].id"));
        assertNotNull(trouve.getString("[0].combatId"));
    }

    private static String combatId() {
        return (String) entrees().getFirst().get("combatId");
    }

    @Test
    void unCombatEnCours_seLitSurLaCarteEnCours() {
        lancer();

        when().get(campagne() + "/combats/" + combatId()).then().statusCode(200)
                .body("ended", equalTo(false))
                .body("current", equalTo(true))
                .body("scene.tokens.size()", equalTo(2));
    }

    @Test
    void apresLaFin_lEcranResteSurLaCarte_etLeCombatSeLitTermine() {
        lancer();
        dice.script(18, 5);
        given().contentType(ContentType.JSON)
                .body("{\"attackerId\":\"gobelin-1\",\"attackId\":\"melee:Cimeterre\",\"targetId\":\"guerriere-1\"}")
                .post(campagne() + "/attacks").then().statusCode(200);
        when().post(campagne() + "/combat/end").then().statusCode(200);
        int pv = when().get(campagne() + "/scene").then().statusCode(200).extract().path("tokens[0].hitPoints");

        when().get(campagne() + "/combat").then().statusCode(204);
        given().contentType(ContentType.JSON).body("{\"x\":10,\"y\":10}")
                .post(campagne() + "/scene/tokens/gobelin-1/move").then().statusCode(200);
        when().get(campagne() + "/combats/" + combatId()).then().statusCode(200)
                .body("ended", equalTo(true))
                .body("current", equalTo(false))
                .body("scene.tokens[0].hitPoints", equalTo(pv))
                .body("scene.tokens[1].x", equalTo(3.0f));
        when().post(campagne() + "/combats/" + combatId() + "/resume").then().statusCode(409);
    }

    @Test
    void uneNouvelleCarte_laisseLeCombatEnCours_quiSeReprend() {
        lancer();
        String id = combatId();
        nouvelleCarte();

        when().get(campagne() + "/combats/" + id).then().statusCode(200)
                .body("ended", equalTo(false))
                .body("current", equalTo(false));
        when().get(campagne() + "/combat").then().statusCode(204);

        when().post(campagne() + "/combats/" + id + "/resume").then().statusCode(204);

        when().get(campagne() + "/combats/" + id).then().statusCode(200).body("current", equalTo(true));
        when().get(campagne() + "/combat").then().statusCode(200).body("round", equalTo(1));
        when().get(campagne() + "/scene").then().statusCode(200).body("tokens.size()", equalTo(2));
        when().post(campagne() + "/combats/" + id + "/resume").then().statusCode(204);
    }

    @Test
    void unCombatInconnu_rend404() {
        when().get(campagne() + "/combats/0f8fad5b-d9cb-469f-a165-70867728950e").then().statusCode(404);
        when().get(campagne() + "/combats/inconnu").then().statusCode(404);
        when().post(campagne() + "/combats/inconnu/resume").then().statusCode(404);
    }
}
