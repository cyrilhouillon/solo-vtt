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
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Carte de 30 × 20 m : Guerrière (1,5 ; 1,5), Squelette au contact (3 ; 1,5), Gobelin à 10,5 m (12 ; 1,5),
 * Magicienne au contact du Gobelin (12 ; 3). Dés scriptés : d20, puis dés de dégâts.
 */
@QuarkusTest
class AttackApiTest {

    @Inject
    ScriptedDice dice;

    @BeforeEach
    void scene() {
        nouvelleCarte(30, 20, "METRE");
        poser("guerriere", 1.5, 1.5);
        poser("squelette", 3, 1.5);
        poser("gobelin", 12, 1.5);
        poser("magicienne", 12, 3);
    }

    private static void nouvelleCarte(double w, double h, String unit) {
        given().contentType(ContentType.JSON).body("{\"width\":%s,\"height\":%s,\"unit\":\"%s\"}".formatted(w, h, unit))
                .post("/api/scene").then().statusCode(201);
    }

    private static void poser(String catalogId, double x, double y) {
        given().contentType(ContentType.JSON).body("{\"catalogId\":\"%s\",\"x\":%s,\"y\":%s}".formatted(catalogId, x, y))
                .post("/api/scene/tokens").then().statusCode(201);
    }

    private static ValidatableResponse attaquer(String attackerId, String attackId, String targetId) {
        return given().contentType(ContentType.JSON)
                .body("{\"attackerId\":\"%s\",\"attackId\":\"%s\",\"targetId\":\"%s\"}".formatted(attackerId, attackId, targetId))
                .post("/api/attacks").then();
    }

    private static List<String> journal() {
        return given().get("/api/journal").then().statusCode(200).extract().jsonPath().getList("text", String.class);
    }

    @Test
    void laGuerriereFrappeLeSqueletteAuContact() {
        dice.script(15, 6);

        attaquer("guerriere-1", "melee:Épée longue", "squelette-1").statusCode(200)
                .body("entries.text", hasItem("Guerrière attaque Squelette (Épée longue)"))
                .body("entries.text", hasItem("d20 : 15"))
                .body("entries.text", hasItem("Attaque de Guerrière : 20 contre CA 14 → TOUCHÉ"))
                .body("entries.text", hasItem("Squelette subit 11 dégâts tranchants (PV 2/13)"))
                .body("entries.action", everyItem(equalTo(1)))
                .body("entries.find { it.highlight == 'OUTCOME' }.roll.outcome", equalTo("HIT"))
                .body("entries.find { it.highlight == 'OUTCOME' }.roll.total", equalTo(20))
                .body("entries.find { it.highlight == 'OUTCOME' }.roll.armorClass", equalTo(14))
                .body("entries.find { it.highlight == 'DAMAGE' }.damage.amount", equalTo(11))
                .body("entries.find { it.highlight == 'DAMAGE' }.damage.type", equalTo("tranchants"))
                .body("targetId", equalTo("squelette-1"))
                .body("targetHitPoints", equalTo(2));

        given().get("/api/creatures/squelette-1").then().body("hitPoints", equalTo(2));
        assertTrue(journal().contains("Squelette subit 11 dégâts tranchants (PV 2/13)"), journal().toString());
    }

    @Test
    void unAdversaireTireALArcSurLaGuerriere() {
        dice.script(19, 4);

        attaquer("gobelin-1", "ranged:Arc court", "guerriere-1").statusCode(200)
                .body("entries.text", hasItem("d20 : 19"))
                .body("entries.text", hasItem("Guerrière subit 6 dégâts perforants (PV 6/12)"));
    }

    @Test
    void enDiagonale_leGobelinFrappeLaMagicienneAuContact() {
        dice.script(15, 3);

        // Gobelin (12 ; 1,5) et Magicienne (13,5 ; 3) : 2,12 m de centre à centre, dans l'allonge + demi-case.
        given().contentType(ContentType.JSON).body("{\"x\":13.5,\"y\":3}").post("/api/scene/tokens/magicienne-1/move").then().statusCode(200);
        attaquer("gobelin-1", "melee:Cimeterre", "magicienne-1").statusCode(200)
                .body("entries.text", hasItem("Gobelin guerrier attaque Magicienne (Cimeterre)"));
    }

    @Test
    void uneCibleHorsDAllongeEstRefuseeAvecLaRaison() {
        attaquer("guerriere-1", "melee:Épée longue", "gobelin-1").statusCode(400)
                .body("message", equalTo("Épée longue : cible à 10,5 m, hors d'allonge (1,5 m)"));

        assertEquals(List.of(), journal());
    }

    @Test
    void laMagicienneToucheeNePoseAucuneDecision() {
        dice.script(15, 3);

        attaquer("gobelin-1", "melee:Cimeterre", "magicienne-1").statusCode(200)
                .body("entries.text", hasItem("Magicienne subit 5 dégâts tranchants (PV 2/7)"));

        // Une nouvelle attaque est acceptée : aucune décision n'est restée en attente.
        dice.script(1);
        attaquer("gobelin-1", "melee:Cimeterre", "magicienne-1").statusCode(200)
                .body("entries.find { it.highlight == 'OUTCOME' }.roll.outcome", equalTo("MISS"))
                .body("entries.findAll { it.damage != null }", empty());
    }

    @Test
    void lesIdsInconnusSontRefuses() {
        attaquer("fantome-1", "melee:Épée longue", "squelette-1").statusCode(404).body("message", containsString("fantome-1"));
        attaquer("guerriere-1", "melee:Épée longue", "fantome-1").statusCode(404);
        attaquer("guerriere-1", "melee:Hache", "squelette-1").statusCode(400).body("message", containsString("Attaque inconnue"));
    }

    @Test
    void uneNouvelleCarteVideLeJournal() {
        dice.script(15, 6);
        attaquer("guerriere-1", "melee:Épée longue", "squelette-1").statusCode(200);

        nouvelleCarte(10, 10, "METRE");

        given().get("/api/journal").then().statusCode(200).body("$", empty());
    }

    @Test
    void surUneCarteEnPieds_laFeuilleDonneAllongeEtPorteeEnPieds() {
        nouvelleCarte(100, 100, "FOOT");
        poser("magicienne", 5, 5);

        given().get("/api/creatures/magicienne-1").then().statusCode(200)
                .body("attacks", hasSize(2))
                .body("attacks[0].label", equalTo("Dague"))
                .body("attacks[0].reach.value", equalTo(5.0f))
                .body("attacks[0].reachLimit.value", equalTo(7.5f))
                .body("attacks[0].reach.unitSymbol", equalTo("ft"))
                .body("attacks[1].label", equalTo("Dague (lancer)"))
                .body("attacks[1].range.normal", equalTo(20.0f))
                .body("attacks[1].range.longRange", equalTo(60.0f));
    }
}
