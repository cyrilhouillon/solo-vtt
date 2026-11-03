package solovtt.server;

import io.restassured.http.ContentType;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/** Les tests d'API jouent chacun dans une campagne neuve : la session n'ouvre aucune campagne d'elle-même. */
final class Campagnes {

    private Campagnes() {}

    /** Crée une campagne puis l'ouvre ; rend son id. */
    static String ouvrirUneNeuve() {
        String id = given().contentType(ContentType.JSON).body(Map.of("name", "Campagne " + UUID.randomUUID()))
                .post("/api/campaigns").then().statusCode(201).extract().path("id");
        given().post("/api/campaigns/{id}/open", id).then().statusCode(200);
        return id;
    }
}
