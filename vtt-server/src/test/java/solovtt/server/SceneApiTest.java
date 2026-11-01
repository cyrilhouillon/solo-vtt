package solovtt.server;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class SceneApiTest {

    @BeforeEach
    void carteVideDeTrenteSurVingtMetres() {
        nouvelleCarte(30, 20, "METRE").statusCode(201)
                .body("unit", equalTo("METRE"))
                .body("unitSymbol", equalTo("m"))
                .body("tokens", empty());
    }

    private static io.restassured.response.ValidatableResponse nouvelleCarte(double w, double h, String unit) {
        return given().contentType(ContentType.JSON)
                .body("{\"width\":%s,\"height\":%s,\"unit\":\"%s\"}".formatted(w, h, unit))
                .post("/api/scene").then();
    }

    private static io.restassured.response.ValidatableResponse poser(String catalogId, double x, double y) {
        return given().contentType(ContentType.JSON)
                .body("{\"catalogId\":\"%s\",\"x\":%s,\"y\":%s}".formatted(catalogId, x, y))
                .post("/api/scene/tokens").then();
    }

    @Test
    void leCatalogueListeLesCinqCreaturesSrd() {
        given().get("/api/catalog").then().statusCode(200)
                .body("$", hasSize(5))
                .body("id", hasItems("guerriere", "magicienne", "gobelin", "squelette", "loup"))
                .body("find { it.id == 'gobelin' }.kind", equalTo("MONSTER"));
    }

    @Test
    void poserDeuxFoisLeMemeMonstreDonneDeuxCreaturesDistinctes() {
        poser("gobelin", 3, 4).statusCode(201).body("id", equalTo("gobelin-1")).body("name", equalTo("Gobelin guerrier"));
        poser("gobelin", 6, 8).statusCode(201).body("id", equalTo("gobelin-2")).body("name", equalTo("Gobelin guerrier 2"));

        given().get("/api/scene").then().statusCode(200)
                .body("tokens.id", hasItems("gobelin-1", "gobelin-2"))
                .body("tokens.find { it.id == 'gobelin-2' }.x", equalTo(6.0f))
                .body("tokens.find { it.id == 'gobelin-2' }.catalogId", equalTo("gobelin"));
    }

    @Test
    void laFeuilleDecritLaCreature() {
        poser("squelette", 1, 1);

        given().get("/api/creatures/squelette-1").then().statusCode(200)
                .body("name", equalTo("Squelette"))
                .body("armorClass", equalTo(13))
                .body("hitPoints", equalTo(13))
                .body("maxHitPoints", equalTo(13))
                .body("speed.value", equalTo(9.0f))
                .body("speed.unitSymbol", equalTo("m"))
                .body("weapon.name", equalTo("Épée courte"))
                .body("weapon.damage", equalTo("1d6+2"))
                .body("vulnerabilities", hasItems("contondants"));
    }

    @Test
    void surUneCarteEnPieds_laVitesseEstEnPieds() {
        nouvelleCarte(100, 100, "FOOT").statusCode(201);
        poser("loup", 1, 1);

        given().get("/api/creatures/loup-1").then().statusCode(200)
                .body("speed.value", equalTo(40.0f))
                .body("speed.unitSymbol", equalTo("ft"));
    }

    @Test
    void unJetonHorsDeLaCarteEstRefuseAvecLaRaison() {
        poser("loup", 31, 1).statusCode(400).body("message", containsString("hors de la carte"));
    }

    @Test
    void lesIdsInconnusRendent404() {
        poser("dragon", 1, 1).statusCode(404).body("message", containsString("dragon"));
        given().get("/api/creatures/fantome-1").then().statusCode(404).body("message", containsString("fantome-1"));    }

    @Test
    void recreerUneCarteVideLaScene() {
        poser("loup", 1, 1).statusCode(201);
        nouvelleCarte(10, 10, "METRE").statusCode(201).body("tokens", empty());
        given().get("/api/creatures/loup-1").then().statusCode(404);
        poser("loup", 1, 1).body("id", equalTo("loup-1"));
    }

    @Test
    void retirerUnJetonRetireAussiSaCreature() {
        poser("loup", 1, 1).statusCode(201);
        poser("loup", 4, 4).statusCode(201);

        given().delete("/api/scene/tokens/loup-1").then().statusCode(204);

        given().get("/api/scene").then().body("tokens.id", equalTo(java.util.List.of("loup-2")));
        given().get("/api/creatures/loup-1").then().statusCode(404);
        given().delete("/api/scene/tokens/loup-1").then().statusCode(404).body("message", containsString("loup-1"));
        poser("loup", 2, 2).body("id", equalTo("loup-3"));
    }

    @Test
    void uneCarteDeDimensionInvalideEstRefusee() {
        nouvelleCarte(0, 10, "METRE").statusCode(400).body("message", containsString("positives"));
    }
}
