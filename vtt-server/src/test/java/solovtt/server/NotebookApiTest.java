package solovtt.server;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static solovtt.server.Campagnes.campagne;

/** Le Carnet d'une campagne neuve : « Session 1 », puis « Bertrand ». */
@QuarkusTest
class NotebookApiTest {

    private static final String ARRIVEE = """
            {"type":"doc","content":[{"type":"heading","attrs":{"level":1},"content":[{"type":"text","text":"Arrivée"}]}]}""";

    @BeforeEach
    void campagneNeuve() {
        Campagnes.creerUneNeuve();
    }

    private static String carnet() {
        return campagne() + "/notebook";
    }

    private static String creer(String title) {
        return given().contentType(ContentType.JSON).body(Map.of("title", title))
                .post(carnet() + "/pages").then().statusCode(201).extract().path("id");
    }

    private static void enregistrer(String pageId, String content, int status) {
        given().contentType(ContentType.JSON).body("{\"content\":" + content + "}")
                .put(carnet() + "/pages/" + pageId + "/content").then().statusCode(status);
    }

    @Test
    void uneCampagneNeuve_aUnCarnetVide() {
        given().get(carnet()).then().statusCode(200).body("pages", empty()).body("lastPageId", nullValue());
    }

    @Test
    void creerUnePage_titreNettoye_sansContenu_listeeParTitre() {
        given().contentType(ContentType.JSON).body(Map.of("title", "  Session 1 "))
                .post(carnet() + "/pages").then().statusCode(201)
                .body("title", equalTo("Session 1")).body("content", nullValue());
        creer("Bertrand");

        given().get(carnet()).then().statusCode(200).body("pages.title", equalTo(List.of("Bertrand", "Session 1")));
    }

    @Test
    void leContenu_seRelitALIdentique() {
        String session = creer("Session 1");
        enregistrer(session, ARRIVEE, 204);

        given().get(carnet() + "/pages/" + session).then().statusCode(200)
                .body("title", equalTo("Session 1"))
                .body("content.type", equalTo("doc"))
                .body("content.content[0].attrs.level", equalTo(1))
                .body("content.content[0].content[0].text", equalTo("Arrivée"));
    }

    @Test
    void laDernierePageConsultee_estGardee() {
        String session = creer("Session 1");
        String bertrand = creer("Bertrand");
        given().contentType(ContentType.JSON).body(Map.of("pageId", session)).put(carnet() + "/last-page").then().statusCode(204);
        given().contentType(ContentType.JSON).body(Map.of("pageId", bertrand)).put(carnet() + "/last-page").then().statusCode(204);

        given().get(carnet()).then().body("lastPageId", equalTo(bertrand));
    }

    @Test
    void unTitreAbsentVideOuTropLong_400() {
        given().contentType(ContentType.JSON).body("{}").post(carnet() + "/pages").then().statusCode(400);
        given().contentType(ContentType.JSON).body(Map.of("title", "   ")).post(carnet() + "/pages").then().statusCode(400);
        given().contentType(ContentType.JSON).body(Map.of("title", "x".repeat(121))).post(carnet() + "/pages").then().statusCode(400);
        given().get(carnet()).then().body("pages", empty());
    }

    @Test
    void unContenuQuiNestPasUnDocument_400_etLaPageGardeLeSien() {
        String session = creer("Session 1");
        enregistrer(session, ARRIVEE, 204);

        given().contentType(ContentType.JSON).body("{}").put(carnet() + "/pages/" + session + "/content").then().statusCode(400);
        enregistrer(session, "\"texte\"", 400);
        enregistrer(session, "{\"type\":\"paragraph\"}", 400);

        given().get(carnet() + "/pages/" + session).then().body("content.content[0].content[0].text", equalTo("Arrivée"));
    }

    @Test
    void unePageInconnue_404() {
        creer("Session 1");
        given().get(carnet() + "/pages/inconnue").then().statusCode(404);
        enregistrer("inconnue", ARRIVEE, 404);
        given().contentType(ContentType.JSON).body(Map.of("pageId", "inconnue")).put(carnet() + "/last-page").then().statusCode(404);
        given().contentType(ContentType.JSON).body("{}").put(carnet() + "/last-page").then().statusCode(400);
    }

    @Test
    void uneCampagneInconnue_404() {
        given().get("/api/campaigns/inconnue/notebook").then().statusCode(404);
        given().contentType(ContentType.JSON).body(Map.of("title", "Session 1"))
                .post("/api/campaigns/inconnue/notebook/pages").then().statusCode(404);
    }

    @Test
    void deuxCampagnes_ontChacuneLeurCarnet() {
        String session = creer("Session 1");
        String premiere = campagne();
        Campagnes.creerUneNeuve();

        given().get(carnet()).then().body("pages", empty());
        given().get(carnet() + "/pages/" + session).then().statusCode(404);
        given().get(premiere + "/notebook").then().body("pages.id", equalTo(List.of(session)));
    }

    private static String creerSous(String title, String parentId) {
        return given().contentType(ContentType.JSON).body("{\"title\":\"" + title + "\",\"parentId\":\"" + parentId + "\"}")
                .post(carnet() + "/pages").then().statusCode(201).body("parentId", equalTo(parentId)).extract().path("id");
    }

    private static void deplacer(String pageId, String parentId, int status) {
        String body = parentId == null ? "{\"parentId\":null}" : "{\"parentId\":\"" + parentId + "\"}";
        given().contentType(ContentType.JSON).body(body).put(carnet() + "/pages/" + pageId + "/parent").then().statusCode(status);
    }

    private static String parentDe(String pageId) {
        return given().get(carnet()).then().statusCode(200).extract().path("pages.find { it.id == '" + pageId + "' }.parentId");
    }

    @Test
    void creerSousUnePage_lArbreSeLitParLeParent() {
        String pnj = creer("PNJ");
        String bertrand = creerSous("Bertrand", pnj);

        given().get(carnet()).then().statusCode(200)
                .body("pages.title", equalTo(List.of("Bertrand", "PNJ")))
                .body("pages.parentId", equalTo(Arrays.asList(pnj, null)));
        given().get(carnet() + "/pages/" + bertrand).then().body("parentId", equalTo(pnj));
    }

    @Test
    void renommer_titreNettoye_contenuGarde() {
        String bertrand = creer("Bertrand");
        enregistrer(bertrand, ARRIVEE, 204);

        given().contentType(ContentType.JSON).body(Map.of("title", " Bertrand le Rouge "))
                .put(carnet() + "/pages/" + bertrand + "/title").then().statusCode(204);

        given().get(carnet()).then().body("pages.title", equalTo(List.of("Bertrand le Rouge")));
        given().get(carnet() + "/pages/" + bertrand).then().body("title", equalTo("Bertrand le Rouge"))
                .body("content.content[0].content[0].text", equalTo("Arrivée"));
    }

    @Test
    void renommerAvecUnTitreInvalide_400_titreGarde() {
        String bertrand = creer("Bertrand");
        given().contentType(ContentType.JSON).body("{}").put(carnet() + "/pages/" + bertrand + "/title").then().statusCode(400);
        given().contentType(ContentType.JSON).body(Map.of("title", "  ")).put(carnet() + "/pages/" + bertrand + "/title")
                .then().statusCode(400);
        given().contentType(ContentType.JSON).body(Map.of("title", "x".repeat(121)))
                .put(carnet() + "/pages/" + bertrand + "/title").then().statusCode(400);

        given().get(carnet()).then().body("pages.title", equalTo(List.of("Bertrand")));
    }

    @Test
    void deplacer_sesSousPagesSuivent_puisRetourALaRacine() {
        String lore = creer("Lore");
        String villes = creerSous("Villes", lore);
        String barovie = creerSous("Barovie", villes);
        String pnj = creer("PNJ");

        deplacer(villes, pnj, 204);
        assertEquals(pnj, parentDe(villes));
        assertEquals(villes, parentDe(barovie));

        deplacer(villes, null, 204);
        assertNull(parentDe(villes));
        assertEquals(villes, parentDe(barovie));
    }

    @Test
    void deplacerSousSoiOuSousUneSousPage_400_arbreInchange() {
        String lore = creer("Lore");
        String villes = creerSous("Villes", lore);
        String barovie = creerSous("Barovie", villes);

        deplacer(lore, lore, 400);
        deplacer(lore, villes, 400);
        deplacer(lore, barovie, 400);

        assertNull(parentDe(lore));
        assertEquals(lore, parentDe(villes));
    }

    @Test
    void parentOuPageInconnus_404() {
        String lore = creer("Lore");
        given().contentType(ContentType.JSON).body("{\"title\":\"Villes\",\"parentId\":\"inconnue\"}")
                .post(carnet() + "/pages").then().statusCode(404);
        deplacer(lore, "inconnue", 404);
        deplacer("inconnue", null, 404);
        given().contentType(ContentType.JSON).body(Map.of("title", "X")).put(carnet() + "/pages/inconnue/title")
                .then().statusCode(404);

        given().get(carnet()).then().body("pages.title", equalTo(List.of("Lore")));
    }
}
