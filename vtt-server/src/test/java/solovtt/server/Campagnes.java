package solovtt.server;

import io.restassured.http.ContentType;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/** Les tests d'API jouent chacun dans une campagne neuve : sa partie ne voit rien des autres tests. */
final class Campagnes {

    /** Le préfixe des routes de la campagne du test en cours. */
    private static String courante;

    private Campagnes() {}

    /** Crée une campagne, qui devient celle des routes de {@link #campagne()} ; rend son id. */
    static String creerUneNeuve() {
        String id = given().contentType(ContentType.JSON).body(Map.of("name", "Campagne " + UUID.randomUUID()))
                .post("/api/campaigns").then().statusCode(201).extract().path("id");
        courante = "/api/campaigns/" + id;
        return id;
    }

    /** {@code /api/campaigns/<id>} de la campagne ouverte par le dernier {@link #creerUneNeuve()}. */
    static String campagne() {
        return courante;
    }
}
