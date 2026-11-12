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

    private static String creerAvec(String name, List<String> modules) {
        return given().contentType(ContentType.JSON).body(Map.of("name", name, "modules", modules))
                .post("/api/campaigns").then().statusCode(201).body("modules", equalTo(modules)).extract().path("id");
    }

    @Test
    void lesModulesChoisisALaCreation_sontLesSeulsActifs() {
        String avec = "/api/campaigns/" + creerAvec("Avec Mythic " + UUID.randomUUID(), List.of("mythic"));
        String sans = "/api/campaigns/" + creerAvec("Sans module " + UUID.randomUUID(), List.of());

        given().get(avec + "/modules").then().statusCode(200).body("modules", equalTo(List.of("mythic")));
        given().get(avec + "/mythic/chaos").then().statusCode(200).body("chaos", equalTo(5));
        given().get(sans + "/modules").then().statusCode(200).body("modules", empty());
        given().get(sans + "/mythic/chaos").then().statusCode(404)
                .body("message", equalTo("Module inactif dans cette campagne : mythic"));
        given().post(sans + "/mythic/lists").then().statusCode(404);
        given().contentType(ContentType.JSON).body("{}").post(sans + "/oracle/yes-no").then().statusCode(404);
        given().contentType(ContentType.JSON).body("{}").post(avec + "/oracle/d100").then().statusCode(404);
        given().post(avec + "/dnd/sections").then().statusCode(404);
        // Les routes communes d'un module ne dépendent d'aucune campagne.
        given().get("/api/mythic/meaning-tables").then().statusCode(200);
        // Le reste de la campagne n'est pas touché.
        given().get(sans + "/notebook").then().statusCode(200);
    }

    @Test
    void sansListeDeModules_tousLesModulesInstallesSontActifs() {
        String id = creer("Sans choix " + UUID.randomUUID());
        given().get("/api/campaigns/" + id + "/modules").then().statusCode(200).body("modules", equalTo(List.of("dnd", "mythic", "oracle")));
        dice.script(37);
        given().contentType(ContentType.JSON).body("{\"question\":\"Le pont tient ?\"}").post("/api/campaigns/" + id + "/oracle/yes-no")
                .then().statusCode(200).body("question", equalTo("Le pont tient ?")).body("roll", equalTo(37)).body("yes", equalTo(true));
        List<Map<String, Object>> listees = given().get("/api/campaigns").then().statusCode(200).extract().jsonPath().getList("$");
        assertEquals(true, listees.stream().anyMatch(c -> c.get("id").equals(id) && ((List<?>) c.get("modules")).contains("mythic")));
    }

    @Test
    void unModuleInconnuOuInvalide_400_etAucuneCampagneCreee() {
        int avant = given().get("/api/campaigns").then().extract().jsonPath().getList("$").size();
        given().contentType(ContentType.JSON).body(Map.of("name", "X", "modules", List.of("inconnu"))).post("/api/campaigns")
                .then().statusCode(400).body("message", equalTo("Module inconnu : inconnu"));
        given().contentType(ContentType.JSON).body(Map.of("name", "X", "modules", List.of("Mythic/.."))).post("/api/campaigns")
                .then().statusCode(400);
        assertEquals(avant, given().get("/api/campaigns").then().extract().jsonPath().getList("$").size());
    }

    @Test
    void lesModulesInstalles_seListent() {
        given().get("/api/modules").then().statusCode(200).body("$", equalTo(List.of("dnd", "mythic", "oracle")));
    }

    @Test
    void lesModulesDUneCampagneInconnue_404() {
        given().get("/api/campaigns/inconnue/modules").then().statusCode(404);
    }

    private static void modules(String campagne, String corps, int statut) {
        given().contentType(ContentType.JSON).body(corps).put(campagne + "/modules").then().statusCode(statut);
    }

    @Test
    void desactiverPuisReactiverUnModule_gardeSesDonnees_sansToucherLesAutresCampagnes() {
        String a = "/api/campaigns/" + creerAvec("A " + UUID.randomUUID(), List.of("mythic"));
        String b = "/api/campaigns/" + creerAvec("B " + UUID.randomUUID(), List.of("mythic"));
        given().contentType(ContentType.JSON).body("{\"chaos\":7}").put(a + "/mythic/chaos").then().statusCode(200);
        String fils = given().post(a + "/mythic/lists").then().statusCode(200).extract().path("threadsPageId");

        given().contentType(ContentType.JSON).body("{\"modules\":[]}").put(a + "/modules")
                .then().statusCode(200).body("modules", empty());
        given().get(a + "/modules").then().body("modules", empty());
        given().get(a + "/mythic/chaos").then().statusCode(404);
        given().get(b + "/mythic/chaos").then().statusCode(200);
        // La page Fils reste dans le Carnet, lisible par ses liens.
        given().get(a + "/notebook/pages/" + fils).then().statusCode(200);

        given().contentType(ContentType.JSON).body("{\"modules\":[\"mythic\"]}").put(a + "/modules")
                .then().statusCode(200).body("modules", equalTo(List.of("mythic")));
        given().get(a + "/mythic/chaos").then().statusCode(200).body("chaos", equalTo(7));
        given().post(a + "/mythic/lists").then().statusCode(200).body("threadsPageId", equalTo(fils));
    }

    @Test
    void changerLesModules_refuseUnModuleInconnuUneListeAbsenteEtUneCampagneInconnue() {
        String a = "/api/campaigns/" + creerAvec("A " + UUID.randomUUID(), List.of("mythic"));
        modules(a, "{\"modules\":[\"inconnu\"]}", 400);
        modules(a, "{}", 400);
        given().get(a + "/modules").then().body("modules", equalTo(List.of("mythic")));
        modules("/api/campaigns/inconnue", "{\"modules\":[]}", 404);
    }
}
