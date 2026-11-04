package solovtt.server;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static solovtt.server.Campagnes.campagne;
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
        Campagnes.creerUneNeuve();
        nouvelleCarte(30, 20, "METRE").statusCode(201)
                .body("unit", equalTo("METRE"))
                .body("unitSymbol", equalTo("m"))
                .body("tokens", empty());
    }

    private static io.restassured.response.ValidatableResponse nouvelleCarte(double w, double h, String unit) {
        return given().contentType(ContentType.JSON)
                .body("{\"width\":%s,\"height\":%s,\"unit\":\"%s\"}".formatted(w, h, unit))
                .post(campagne() + "/scene").then();
    }

    private static io.restassured.response.ValidatableResponse poser(String catalogId, double x, double y) {
        return given().contentType(ContentType.JSON)
                .body("{\"catalogId\":\"%s\",\"x\":%s,\"y\":%s}".formatted(catalogId, x, y))
                .post(campagne() + "/scene/tokens").then();
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

        given().get(campagne() + "/scene").then().statusCode(200)
                .body("tokens.id", hasItems("gobelin-1", "gobelin-2"))
                .body("tokens.find { it.id == 'gobelin-2' }.x", equalTo(6.0f))
                .body("tokens.find { it.id == 'gobelin-2' }.catalogId", equalTo("gobelin"));
    }

    @Test
    void chaqueJetonDonneLaCaEtLesPvDeSaCreature() {
        poser("squelette", 1, 1).statusCode(201)
                .body("armorClass", equalTo(14)).body("hitPoints", equalTo(13)).body("maxHitPoints", equalTo(13));

        given().get(campagne() + "/scene").then().statusCode(200)
                .body("tokens.find { it.id == 'squelette-1' }.armorClass", equalTo(14))
                .body("tokens.find { it.id == 'squelette-1' }.hitPoints", equalTo(13));
    }

    @Test
    void laFeuilleDecritLaCreature() {
        poser("squelette", 1, 1);

        given().get(campagne() + "/creatures/squelette-1").then().statusCode(200)
                .body("name", equalTo("Squelette"))
                .body("armorClass", equalTo(14))
                .body("hitPoints", equalTo(13))
                .body("maxHitPoints", equalTo(13))
                .body("speed.value", equalTo(9.0f))
                .body("speed.unitSymbol", equalTo("m"))
                .body("attacks.label", equalTo(java.util.List.of("Épée courte", "Arc court")))
                .body("attacks[0].damage", equalTo("1d6+3"))
                .body("attacks[0].reach.value", equalTo(1.5f))
                .body("attacks[1].range.normal", equalTo(24.0f))
                .body("attacks[1].range.longRange", equalTo(96.0f))
                .body("vulnerabilities", hasItems("contondants"));
    }

    @Test
    void surUneCarteEnPieds_laVitesseEstEnPieds() {
        nouvelleCarte(100, 100, "FOOT").statusCode(201);
        poser("loup", 1, 1);

        given().get(campagne() + "/creatures/loup-1").then().statusCode(200)
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
        given().get(campagne() + "/creatures/fantome-1").then().statusCode(404).body("message", containsString("fantome-1"));    }

    @Test
    void recreerUneCarteVideLaScene() {
        poser("loup", 1, 1).statusCode(201);
        nouvelleCarte(10, 10, "METRE").statusCode(201).body("tokens", empty());
        given().get(campagne() + "/creatures/loup-1").then().statusCode(404);
        poser("loup", 1, 1).body("id", equalTo("loup-1"));
    }

    @Test
    void retirerUnJetonRetireAussiSaCreature() {
        poser("loup", 1, 1).statusCode(201);
        poser("loup", 4, 4).statusCode(201);

        given().delete(campagne() + "/scene/tokens/loup-1").then().statusCode(204);

        given().get(campagne() + "/scene").then().body("tokens.id", equalTo(java.util.List.of("loup-2")));
        given().get(campagne() + "/creatures/loup-1").then().statusCode(404);
        given().delete(campagne() + "/scene/tokens/loup-1").then().statusCode(404).body("message", containsString("loup-1"));
        poser("loup", 2, 2).body("id", equalTo("loup-3"));
    }

    @Test
    void uneCarteDeDimensionInvalideEstRefusee() {
        nouvelleCarte(0, 10, "METRE").statusCode(400).body("message", containsString("positives"));
    }

    private static io.restassured.response.ValidatableResponse grille(String body) {
        return given().contentType(ContentType.JSON).body(body).put(campagne() + "/scene/grid").then();
    }

    @Test
    void uneCarteSurUneImage_porteSonImage_etAfficheSaGrille() {
        String imageId = ImageApiTest.envoyer(ImageApiTest.PNG, "image/png");

        given().contentType(ContentType.JSON)
                .body("{\"width\":45,\"height\":45,\"unit\":\"METRE\",\"imageId\":\"%s\"}".formatted(imageId))
                .post(campagne() + "/scene").then().statusCode(201)
                .body("width", equalTo(45.0f))
                .body("imageId", equalTo(imageId))
                .body("gridVisible", equalTo(true));
        given().get(campagne() + "/scene").then().statusCode(200).body("imageId", equalTo(imageId));
    }

    @Test
    void uneCarteSansImage_nAPasDImage() {
        given().get(campagne() + "/scene").then().statusCode(200)
                .body("imageId", org.hamcrest.Matchers.nullValue())
                .body("gridVisible", equalTo(true));
    }

    @Test
    void uneImageInconnue_estRefusee_etLaCarteEnCoursReste() {
        poser("gobelin", 3, 4).statusCode(201);

        given().contentType(ContentType.JSON)
                .body("{\"width\":45,\"height\":45,\"unit\":\"METRE\",\"imageId\":\"%s\"}".formatted("0".repeat(64)))
                .post(campagne() + "/scene").then().statusCode(400)
                .body("message", containsString("Image inconnue"));
        given().get(campagne() + "/scene").then().statusCode(200).body("width", equalTo(30.0f)).body("tokens", hasSize(1));
    }

    @Test
    void masquerPuisAfficherLaGrille() {
        grille("{\"visible\":false}").statusCode(200).body("gridVisible", equalTo(false));
        given().get(campagne() + "/scene").then().body("gridVisible", equalTo(false));

        grille("{\"visible\":true}").statusCode(200).body("gridVisible", equalTo(true));
    }

    @Test
    void uneNouvelleCarte_afficheDeNouveauSaGrille() {
        grille("{\"visible\":false}").statusCode(200);

        nouvelleCarte(30, 20, "METRE").statusCode(201).body("gridVisible", equalTo(true));
    }

    private static io.restassured.response.ValidatableResponse caler(String body) {
        return given().contentType(ContentType.JSON).body(body).put(campagne() + "/scene/alignment").then();
    }

    private static void carteSurImage() {
        String imageId = ImageApiTest.envoyer(ImageApiTest.PNG, "image/png");
        given().contentType(ContentType.JSON)
                .body("{\"width\":30,\"height\":20,\"unit\":\"METRE\",\"imageId\":\"%s\"}".formatted(imageId))
                .post(campagne() + "/scene").then().statusCode(201)
                .body("gridOffsetX", equalTo(0.0f))
                .body("gridOffsetY", equalTo(0.0f));
    }

    @Test
    void calerLaGrille_metLaCarteEtSesJetonsALEchelle() {
        carteSurImage();
        poser("loup", 3, 4.5).statusCode(201);

        caler("{\"width\":33,\"height\":22,\"gridOffsetX\":0.25,\"gridOffsetY\":0.5}").statusCode(200)
                .body("width", equalTo(33.0f))
                .body("height", equalTo(22.0f))
                .body("gridOffsetX", equalTo(0.25f))
                .body("gridOffsetY", equalTo(0.5f))
                .body("tokens[0].x", equalTo(3.3f))
                .body("tokens[0].y", equalTo(4.95f));
        given().get(campagne() + "/scene").then().body("width", equalTo(33.0f)).body("gridOffsetY", equalTo(0.5f));
    }

    @Test
    void calerUneCarteSansImage_estRefuse() {
        caler("{\"width\":33,\"height\":22,\"gridOffsetX\":0,\"gridOffsetY\":0}").statusCode(409)
                .body("message", containsString("sans image"));
    }

    @Test
    void calerAvecDesCasesNonCarrees_ouUnDecalageHorsDeLaCase_estRefuse_etLaCarteReste() {
        carteSurImage();

        caler("{\"width\":33,\"height\":20,\"gridOffsetX\":0,\"gridOffsetY\":0}").statusCode(400)
                .body("message", containsString("carrées"));
        caler("{\"width\":33,\"height\":22,\"gridOffsetX\":1,\"gridOffsetY\":0}").statusCode(400);
        caler("{\"width\":33,\"height\":22,\"gridOffsetX\":0}").statusCode(400);
        given().get(campagne() + "/scene").then().body("width", equalTo(30.0f));
    }

    @Test
    void basculerLaGrilleSansDireSiElleSAffiche_estRefuse() {
        grille("{}").statusCode(400);
    }
}
