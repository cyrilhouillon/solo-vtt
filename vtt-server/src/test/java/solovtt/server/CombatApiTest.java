package solovtt.server;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Carte de 30 × 20 m : Guerrière (+2), Gobelin guerrier (+2) au contact, Squelette (+3), posés dans cet ordre.
 * d20 d'initiative 10, 15, 4 : Gobelin guerrier 17, Guerrière 12, Squelette 7.
 */
@QuarkusTest
class CombatApiTest {

    @Inject
    ScriptedDice dice;

    @BeforeEach
    void scene() {
        nouvelleCarte();
        poser("guerriere", 1.5, 1.5).statusCode(201);
        poser("gobelin", 3, 1.5).statusCode(201);
        poser("squelette", 12, 12).statusCode(201);
    }

    private static void nouvelleCarte() {
        given().contentType(ContentType.JSON).body("{\"width\":30,\"height\":20,\"unit\":\"METRE\"}")
                .post("/api/scene").then().statusCode(201);
    }

    private static ValidatableResponse poser(String catalogId, double x, double y) {
        return given().contentType(ContentType.JSON).body("{\"catalogId\":\"%s\",\"x\":%s,\"y\":%s}".formatted(catalogId, x, y))
                .post("/api/scene/tokens").then();
    }

    private static ValidatableResponse lancer() {
        return when().post("/api/combat").then();
    }

    private static ValidatableResponse finDuTour() {
        return when().post("/api/combat/end-turn").then();
    }

    @Test
    void sansCombat_rienARendre() {
        when().get("/api/combat").then().statusCode(204);
    }

    @Test
    void lancerLeCombat_rendLOrdreEtLaCreatureActive() {
        dice.script(10, 15, 4);

        lancer().statusCode(200)
                .body("combat.round", equalTo(1))
                .body("combat.activeId", equalTo("gobelin-1"))
                .body("combat.groups.initiative", equalTo(List.of(17, 12, 7)))
                .body("combat.groups[0].members[0].name", equalTo("Gobelin guerrier"))
                .body("combat.groups[0].members[0].played", equalTo(true))
                .body("targetId", nullValue())
                .body("decision", nullValue())
                .body("entries.text", hasItem("Ordre de jeu : Gobelin guerrier (17), Guerrière (12), Squelette (7)"))
                .body("entries.text[-1]", equalTo("Tour de Gobelin guerrier"));

        when().get("/api/combat").then().statusCode(200).body("activeId", equalTo("gobelin-1"));
    }

    @Test
    void finDuTour_enchaineLesTours_puisLeRoundSuivant() {
        dice.script(10, 15, 4);
        lancer().statusCode(200);

        finDuTour().statusCode(200).body("combat.activeId", equalTo("guerriere-1"));
        finDuTour().statusCode(200).body("combat.activeId", equalTo("squelette-1"));
        finDuTour().statusCode(200)
                .body("combat.activeId", equalTo("gobelin-1"))
                .body("combat.round", equalTo(2))
                .body("entries.text", equalTo(List.of("Fin du tour de Squelette", "Round 2", "Tour de Gobelin guerrier")));

        when().get("/api/journal").then().statusCode(200)
                .body("text", hasItem("Le combat commence"))
                .body("text", hasItem("Round 1"))
                .body("text", hasItem("Tour de Guerrière"))
                .body("text[-1]", equalTo("Tour de Gobelin guerrier"));
    }

    @Test
    void exAequo_quiJoue_laReponseRendLaCreatureActive_sansCible() {
        dice.script(10, 10, 1);

        String id = lancer().statusCode(200)
                .body("decision.prompt", equalTo("Qui joue ?"))
                .body("decision.options", equalTo(List.of("Guerrière", "Gobelin guerrier")))
                .body("combat.activeId", nullValue())
                .body("combat.candidates", equalTo(List.of("guerriere-1", "gobelin-1")))
                .extract().path("decision.id");
        finDuTour().statusCode(409);

        given().contentType(ContentType.JSON).body("{\"option\":\"Gobelin guerrier\"}")
                .post("/api/decisions/" + id).then().statusCode(200)
                .body("targetId", nullValue())
                .body("decision", nullValue())
                .body("combat.activeId", equalTo("gobelin-1"))
                .body("entries.text", equalTo(List.of("Tour de Gobelin guerrier")));
    }

    @Test
    void enCombat_uneCreatureInactiveNAttaquePas() {
        dice.script(10, 15, 4);
        lancer().statusCode(200);

        given().contentType(ContentType.JSON)
                .body("{\"attackerId\":\"guerriere-1\",\"attackId\":\"melee:Épée longue\",\"targetId\":\"gobelin-1\"}")
                .post("/api/attacks").then().statusCode(400)
                .body("message", equalTo("Ce n'est pas le tour de Guerrière"));
    }

    @Test
    void enCombat_unePoseEntreDansLOrdre_unRetraitEnSort() {
        dice.script(10, 15, 4, /* Loup */ 12);
        lancer().statusCode(200);

        poser("loup", 20, 3).statusCode(201).body("id", equalTo("loup-1"));
        when().get("/api/combat").then().statusCode(200)
                .body("groups.initiative", equalTo(List.of(17, 14, 12, 7)))
                .body("groups[1].members[0].creatureId", equalTo("loup-1"));
        when().get("/api/journal").then().body("text[-1]", equalTo("Loup entre dans le combat (initiative 14)"));

        when().delete("/api/scene/tokens/gobelin-1").then().statusCode(204);
        when().get("/api/combat").then().statusCode(200)
                .body("activeId", equalTo("loup-1"))
                .body("groups.initiative", not(hasItem(17)));
        when().get("/api/journal").then()
                .body("text", hasItem("Gobelin guerrier quitte le combat"))
                .body("text[-1]", equalTo("Tour de Loup"));
    }

    @Test
    void refus_sansCreature_dejaLance_horsCombat() {
        finDuTour().statusCode(409).body("message", equalTo("Aucun combat en cours"));
        dice.script(10, 15, 4);
        lancer().statusCode(200);
        lancer().statusCode(409).body("message", equalTo("Le combat est déjà lancé"));

        nouvelleCarte();
        lancer().statusCode(409).body("message", containsString("Aucune créature"));
    }

    @Test
    void uneDecisionEnAttente_bloqueLeCombatLaPoseEtLeRetrait() {
        dice.script(10, 10, 1);
        lancer().statusCode(200).body("decision", notNullValue());

        lancer().statusCode(409).body("message", containsString("Une décision attend une réponse"));
        finDuTour().statusCode(409).body("message", containsString("Une décision attend une réponse"));
        poser("loup", 20, 3).statusCode(409);
        when().delete("/api/scene/tokens/squelette-1").then().statusCode(409);
    }

    @Test
    void nouvelleCarte_effaceLeCombat() {
        dice.script(10, 15, 4);
        lancer().statusCode(200);

        nouvelleCarte();

        when().get("/api/combat").then().statusCode(204);
    }
}
