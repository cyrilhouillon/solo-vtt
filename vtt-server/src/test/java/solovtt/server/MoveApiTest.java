package solovtt.server;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static solovtt.server.Campagnes.campagne;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * Carte de 30 × 20 m : Guerrière, Gobelin guerrier (vitesse 9 m) et Squelette, posés dans cet ordre.
 * d20 d'initiative 10, 15, 4 : Gobelin guerrier joue d'abord.
 */
@QuarkusTest
class MoveApiTest {

    @Inject
    ScriptedDice dice;

    @BeforeEach
    void scene() {
        Campagnes.creerUneNeuve();
        nouvelleCarte("METRE");
    }

    private static void nouvelleCarte(String unit) {
        given().contentType(ContentType.JSON).body("{\"width\":30,\"height\":20,\"unit\":\"%s\"}".formatted(unit))
                .post(campagne() + "/scene").then().statusCode(201);
        poser("guerriere", 1.5, 1.5);
        poser("gobelin", 3, 1.5);
        poser("squelette", 12, 12);
    }

    private static void poser(String catalogId, double x, double y) {
        given().contentType(ContentType.JSON).body("{\"catalogId\":\"%s\",\"x\":%s,\"y\":%s}".formatted(catalogId, x, y))
                .post(campagne() + "/scene/tokens").then().statusCode(201);
    }

    private static ValidatableResponse deplacer(String tokenId, double x, double y) {
        return given().contentType(ContentType.JSON).body("{\"x\":%s,\"y\":%s}".formatted(x, y))
                .post(campagne() + "/scene/tokens/%s/move".formatted(tokenId)).then();
    }

    private void lancer(int... d20) {
        dice.script(d20);
        when().post(campagne() + "/combat").then().statusCode(200);
    }

    private static ValidatableResponse jeton(String tokenId) {
        return when().get(campagne() + "/scene").then().statusCode(200).rootPath("tokens.find { it.id == '%s' }".formatted(tokenId));
    }

    @Test
    void horsCombat_leDeplacementEstLibre_etHorsJournal() {
        deplacer("squelette-1", 29, 19).statusCode(200)
                .body("entries", equalTo(List.of()))
                .body("combat", nullValue());

        jeton("squelette-1").body("x", equalTo(29.0f)).body("y", equalTo(19.0f));
        when().get(campagne() + "/journal").then().body("size()", equalTo(0));
    }

    @Test
    void enCombat_laCreatureActiveSeDeplaceDansSaVitesse_etLeJournalLeDit() {
        lancer(10, 15, 4);

        deplacer("gobelin-1", 9, 1.5).statusCode(200)
                .body("entries.text", equalTo(List.of("Gobelin guerrier se déplace de 6 m (reste 3 m)")))
                .body("combat.movementLeft", equalTo(3.0f));

        jeton("gobelin-1").body("x", equalTo(9.0f));
        when().get(campagne() + "/combat").then().body("movementLeft", equalTo(3.0f));
    }

    @Test
    void enCombat_uneCreatureNonActiveSeRepositionneLibrement() {
        lancer(10, 15, 4);

        deplacer("squelette-1", 29, 1).statusCode(200)
                .body("entries", equalTo(List.of()))
                .body("combat.movementLeft", equalTo(9.0f));

        jeton("squelette-1").body("x", equalTo(29.0f));
    }

    @Test
    void auDelaDeLaVitesseRestante_refusAvecLaRaison_sansBougerNiEcrire() {
        lancer(10, 15, 4);
        int avant = when().get(campagne() + "/journal").then().extract().path("size()");

        deplacer("gobelin-1", 13, 1.5).statusCode(400)
                .body("message", equalTo("Gobelin guerrier ne peut plus parcourir que 9 m ce tour-ci (déplacement de 10 m)"));

        jeton("gobelin-1").body("x", equalTo(3.0f));
        when().get(campagne() + "/journal").then().body("size()", equalTo(avant));
    }

    @Test
    void horsDeLaCarte_refuse_sansEvenement() {
        lancer(10, 15, 4);
        int avant = when().get(campagne() + "/journal").then().extract().path("size()");

        deplacer("gobelin-1", 3, -1).statusCode(400);

        jeton("gobelin-1").body("y", equalTo(1.5f));
        when().get(campagne() + "/journal").then().body("size()", equalTo(avant));
        when().get(campagne() + "/combat").then().body("movementLeft", equalTo(9.0f));
    }

    @Test
    void jetonInconnu_404_etPositionAbsente_400() {
        deplacer("fantome", 1, 1).statusCode(404);
        given().contentType(ContentType.JSON).body("{}").post(campagne() + "/scene/tokens/gobelin-1/move").then().statusCode(400);
    }

    @Test
    void uneDecisionEnAttente_bloqueLeDeplacement() {
        lancer(10, 10, 1); // Guerrière et Gobelin guerrier à 12 : « Qui joue ? »

        deplacer("squelette-1", 20, 12).statusCode(409);
    }

    @Test
    void auTourSuivantDeLaCreature_saVitesseEstPleine() {
        lancer(10, 15, 4);
        deplacer("gobelin-1", 12, 1.5).statusCode(200).body("combat.movementLeft", equalTo(0.0f));

        for (int i = 0; i < 3; i++) when().post(campagne() + "/combat/end-turn").then().statusCode(200);

        when().get(campagne() + "/combat").then().body("activeId", equalTo("gobelin-1")).body("movementLeft", equalTo(9.0f));
    }

    @Test
    void surUneCarteEnPieds_laVitesseRestanteEstEnPieds() {
        nouvelleCarte("FOOT");
        lancer(10, 15, 4);

        when().get(campagne() + "/combat").then().body("movementLeft", equalTo(30.0f));
        deplacer("gobelin-1", 28, 1.5).statusCode(200)
                .body("entries.text[0]", equalTo("Gobelin guerrier se déplace de 25 ft (reste 5 ft)"))
                .body("combat.movementLeft", equalTo(5.0f));
    }
}
