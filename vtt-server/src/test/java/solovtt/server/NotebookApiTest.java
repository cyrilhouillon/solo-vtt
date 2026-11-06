package solovtt.server;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solovtt.module.NotebookPages;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static solovtt.server.Campagnes.campagne;

/** Le Carnet d'une campagne neuve : « Session 1 », puis « Bertrand ». */
@QuarkusTest
class NotebookApiTest {

    private static final String ARRIVEE = """
            {"type":"doc","content":[{"type":"heading","attrs":{"level":1},"content":[{"type":"text","text":"Arrivée"}]}]}""";

    @Inject
    NotebookPages sections;

    private String campagneId;

    @BeforeEach
    void campagneNeuve() {
        campagneId = Campagnes.creerUneNeuve();
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
        given().get(carnet()).then().statusCode(200).body("pages", empty())
                .body("openPageIds", empty()).body("activePageId", nullValue());
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

    private static void envoyerOnglets(Object body, int status) {
        given().contentType(ContentType.JSON).body(body).put(carnet() + "/tabs").then().statusCode(status);
    }

    private static Map<String, Object> corpsOnglets(List<String> pageIds, String activeId) {
        var body = new HashMap<String, Object>();
        body.put("pageIds", pageIds);
        body.put("activeId", activeId);
        return body;
    }

    @Test
    void lesOngletsEtLActif_sontGardes_dansLOrdre() {
        String session = creer("Session 1");
        String bertrand = creer("Bertrand");
        envoyerOnglets(corpsOnglets(List.of(session, bertrand), bertrand), 204);

        given().get(carnet()).then().body("openPageIds", equalTo(List.of(session, bertrand)))
                .body("activePageId", equalTo(bertrand));

        envoyerOnglets(corpsOnglets(List.of(), null), 204);
        given().get(carnet()).then().body("openPageIds", empty()).body("activePageId", nullValue());
    }

    @Test
    void desOngletsRefuses_laissentLesOngletsGardes() {
        String session = creer("Session 1");
        String bertrand = creer("Bertrand");
        envoyerOnglets(corpsOnglets(List.of(session), session), 204);

        envoyerOnglets(corpsOnglets(List.of(session, "inconnue"), session), 404);
        envoyerOnglets(corpsOnglets(List.of(session, session), session), 400);
        envoyerOnglets(corpsOnglets(List.of(session), bertrand), 400);
        envoyerOnglets(corpsOnglets(List.of(session), null), 400);
        envoyerOnglets("{}", 400);
        envoyerOnglets("{\"pageIds\":[null],\"activeId\":null}", 400);

        given().get(carnet()).then().body("openPageIds", equalTo(List.of(session))).body("activePageId", equalTo(session));
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

    private static String citant(String... pageIds) {
        StringBuilder liens = new StringBuilder();
        for (String pageId : pageIds) {
            if (!liens.isEmpty()) liens.append(',');
            liens.append("{\"type\":\"pageLink\",\"attrs\":{\"pageId\":\"").append(pageId).append("\",\"label\":null}}");
        }
        return "{\"type\":\"doc\",\"content\":[{\"type\":\"bulletList\",\"content\":[{\"type\":\"listItem\",\"content\":["
                + "{\"type\":\"paragraph\",\"content\":[" + liens + "]}]}]}]}";
    }

    private static String citantes(String pageId) {
        return carnet() + "/pages/" + pageId + "/citing";
    }

    @Test
    void lesPagesQuiCitent_unePageChacune_parTitre_sansLaPageElleMeme() {
        String bertrand = creer("Bertrand");
        String pnj = creer("PNJ");
        String session5 = creerSous("Session 5", pnj);
        String session1 = creer("Session 1");
        String lore = creer("Lore");
        enregistrer(session5, citant(bertrand, bertrand), 204);
        enregistrer(session1, citant(bertrand), 204);
        enregistrer(lore, citant(session1), 204);
        enregistrer(bertrand, citant(bertrand), 204);

        given().get(citantes(bertrand)).then().statusCode(200)
                .body("title", equalTo(List.of("Session 1", "Session 5")))
                .body("id", equalTo(List.of(session1, session5)))
                .body("parentId", equalTo(Arrays.asList(null, pnj)));

        enregistrer(session1, ARRIVEE, 204);
        given().get(citantes(bertrand)).then().statusCode(200).body("title", equalTo(List.of("Session 5")));
    }

    @Test
    void unePageQueRienNeCite_listeVide_etPageInconnue_404() {
        String bertrand = creer("Bertrand");
        String session = creer("Session 1");
        enregistrer(session, ARRIVEE, 204);

        given().get(citantes(bertrand)).then().statusCode(200).body("$", empty());
        given().get(citantes(session)).then().statusCode(200).body("$", empty());
        given().get(citantes("inconnue")).then().statusCode(404);
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

    private static String recherche() {
        return carnet() + "/search";
    }

    private static String paragraphe(String... noeuds) {
        return "{\"type\":\"paragraph\",\"content\":[" + String.join(",", noeuds) + "]}";
    }

    private static String texte(String text) {
        return "{\"type\":\"text\",\"text\":\"" + text + "\"}";
    }

    private static String lienVers(String pageId) {
        return "{\"type\":\"pageLink\",\"attrs\":{\"pageId\":\"" + pageId + "\",\"label\":null}}";
    }

    private static String document(String... blocs) {
        return "{\"type\":\"doc\",\"content\":[" + String.join(",", blocs) + "]}";
    }

    @Test
    void chercher_sansCasseNiAccents_titresDAbord_extraitMarque() {
        String eveque = creer("Évêque Aldric");
        String pnj = creer("PNJ");
        String session = creerSous("Session 1", pnj);
        creer("Lore");
        enregistrer(eveque, document(paragraphe(texte("Il garde la crypte"))), 204);
        enregistrer(session, "{\"type\":\"doc\",\"content\":[{\"type\":\"bulletList\",\"content\":[{\"type\":\"listItem\",\"content\":["
                + paragraphe(texte("Nous rencontrons l'évêque à l'auberge du Gué")) + "]}]}]}", 204);

        given().queryParam("q", "eveque").get(recherche()).then().statusCode(200)
                .body("title", equalTo(List.of("Évêque Aldric", "Session 1")))
                .body("id", equalTo(List.of(eveque, session)))
                .body("parentId", equalTo(Arrays.asList(null, pnj)))
                .body("[0].excerpt.text", equalTo(List.of("Il garde la crypte")))
                .body("[1].excerpt.text", equalTo(List.of("Nous rencontrons l'", "évêque", " à l'auberge du Gué")))
                .body("[1].excerpt.match", equalTo(List.of(false, true, false)));

        given().queryParam("q", "EVEQUE auberge").get(recherche()).then().statusCode(200)
                .body("title", equalTo(List.of("Session 1")))
                .body("[0].excerpt.findAll { it.match }.text", equalTo(List.of("évêque", "auberge")));

        given().queryParam("q", "crypte").get(recherche()).then().statusCode(200)
                .body("title", equalTo(List.of("Évêque Aldric")))
                .body("[0].excerpt.text", equalTo(List.of("Il garde la ", "crypte")));

        given().queryParam("q", "lore").get(recherche()).then().statusCode(200)
                .body("title", equalTo(List.of("Lore"))).body("[0].excerpt", empty());
    }

    @Test
    void chercher_unLienSeTrouveSousLeTitreDeSaCible() {
        String eveque = creer("Évêque Aldric");
        String session = creer("Session 1");
        enregistrer(session, document(paragraphe(texte("Visite chez "), lienVers(eveque))), 204);

        given().queryParam("q", "aldric").get(recherche()).then().statusCode(200)
                .body("title", equalTo(List.of("Évêque Aldric", "Session 1")))
                .body("[1].excerpt.text", equalTo(List.of("Visite chez Évêque ", "Aldric")));
    }

    @Test
    void uneImage_seRelitALIdentique_etLaRechercheVoitLeTexteAutour() {
        String bertrand = creer("Bertrand");
        String image = "{\"type\":\"pageImage\",\"attrs\":{\"imageId\":\"abc123\",\"alt\":\"bertrand.png\"}}";
        enregistrer(bertrand, document(paragraphe(texte("Le forgeron")), image, paragraphe(texte("du village"))), 204);

        given().get(carnet() + "/pages/" + bertrand).then().statusCode(200)
                .body("content.content[1].type", equalTo("pageImage"))
                .body("content.content[1].attrs.imageId", equalTo("abc123"))
                .body("content.content[1].attrs.alt", equalTo("bertrand.png"));
        given().queryParam("q", "forgeron village").get(recherche()).then().statusCode(200)
                .body("title", equalTo(List.of("Bertrand")))
                .body("[0].excerpt.text", equalTo(List.of("Le ", "forgeron", " du ", "village")));
    }

    @Test
    void chercher_rienATrouver_listeVide_requeteVide_400() {
        creer("Session 1");
        given().queryParam("q", "dragon").get(recherche()).then().statusCode(200).body("$", empty());
        given().get(recherche()).then().statusCode(400);
        given().queryParam("q", "   ").get(recherche()).then().statusCode(400);
    }

    @Test
    void chercher_neVoitQueLaCampagneOuverte() {
        creer("Évêque Aldric");
        Campagnes.creerUneNeuve();
        creer("Session 1");
        given().queryParam("q", "eveque").get(recherche()).then().statusCode(200).body("$", empty());
    }

    private static String corbeille() {
        return carnet() + "/trash";
    }

    private static void supprimer(String pageId, int status) {
        given().delete(carnet() + "/pages/" + pageId).then().statusCode(status);
    }

    private static void restaurer(String pageId, int status) {
        given().post(corbeille() + "/" + pageId + "/restore").then().statusCode(status);
    }

    @Test
    void avantDeSupprimer_leCarnetDitSesSousPagesEtLesPagesQuiYRenvoient() {
        String pnj = creer("PNJ");
        String bertrand = creerSous("Bertrand", pnj);
        String fils = creerSous("Fils", bertrand);
        String session = creer("Session 1");
        String auberge = creer("Auberge");
        enregistrer(session, citant(bertrand, fils), 204);
        enregistrer(auberge, citant(fils), 204);
        enregistrer(fils, citant(bertrand), 204);

        given().get(carnet() + "/pages/" + bertrand + "/deletion").then().statusCode(200)
                .body("subpageCount", equalTo(1)).body("citingCount", equalTo(2));
        given().get(carnet() + "/pages/" + auberge + "/deletion").then().statusCode(200)
                .body("subpageCount", equalTo(0)).body("citingCount", equalTo(0));
        given().get(carnet() + "/pages/inconnue/deletion").then().statusCode(404);
    }

    @Test
    void supprimer_laPageEtSesSousPagesVontALaCorbeille_puisReviennentALeurPlace() {
        String pnj = creer("PNJ");
        String bertrand = creerSous("Bertrand", pnj);
        String fils = creerSous("Fils", bertrand);
        String session = creer("Session 1");
        enregistrer(session, citant(fils), 204);
        enregistrer(fils, ARRIVEE, 204);
        envoyerOnglets(corpsOnglets(List.of(bertrand, session), bertrand), 204);

        supprimer(bertrand, 204);

        given().get(carnet()).then().body("pages.title", equalTo(List.of("PNJ", "Session 1")))
                .body("openPageIds", equalTo(List.of(session))).body("activePageId", nullValue());
        given().get(corbeille()).then().statusCode(200).body("id", equalTo(List.of(bertrand)))
                .body("title", equalTo(List.of("Bertrand"))).body("subpageCount", equalTo(List.of(1)))
                .body("deletedAt[0]", notNullValue());
        given().get(citantes(fils)).then().statusCode(404);
        given().queryParam("q", "Fils").get(recherche()).then().body("$", empty());
        enregistrer(fils, ARRIVEE, 404);
        given().contentType(ContentType.JSON).body(Map.of("title", "X")).put(carnet() + "/pages/" + bertrand + "/title")
                .then().statusCode(404);
        deplacer(bertrand, null, 404);
        deplacer(session, fils, 404);
        envoyerOnglets(corpsOnglets(List.of(fils), fils), 404);
        supprimer(bertrand, 404);
        restaurer(fils, 404);
        restaurer(session, 404);

        restaurer(bertrand, 204);

        given().get(carnet()).then().body("pages.title", equalTo(List.of("Bertrand", "Fils", "PNJ", "Session 1")))
                .body("pages.parentId", equalTo(Arrays.asList(pnj, bertrand, null, null)));
        given().get(carnet() + "/pages/" + fils).then().body("content.content[0].content[0].text", equalTo("Arrivée"));
        given().get(citantes(fils)).then().body("id", equalTo(List.of(session)));
        given().get(corbeille()).then().body("$", empty());
    }

    @Test
    void restaurerSansSaPlace_revientALaRacine_puisViderEffaceTout() {
        String pnj = creer("PNJ");
        String bertrand = creerSous("Bertrand", pnj);
        supprimer(bertrand, 204);
        supprimer(pnj, 204);

        restaurer(bertrand, 204);
        given().get(carnet()).then().body("pages.title", equalTo(List.of("Bertrand")))
                .body("pages[0].parentId", nullValue());

        given().delete(corbeille()).then().statusCode(204);
        given().get(corbeille()).then().body("$", empty());
        restaurer(pnj, 404);
        supprimer("inconnue", 404);
    }

    @Test
    void uneRacineDeSection_creeeParLePortDUnModule_seMontreMaisNeSeDeplaceNiNeSeSupprime() {
        String fils = sections.createSectionRoot(campagneId, "essai", "Fils", null).id();
        String aldric = creer("Aldric");
        assertEquals(List.of(new NotebookPages.SectionPage(fils, "Fils")), sections.sectionRoots(campagneId, "essai"));
        assertEquals(List.of(), sections.sectionRoots(campagneId, "autre"));

        given().get(carnet()).then().statusCode(200)
                .body("pages.title", equalTo(List.of("Aldric", "Fils")))
                .body("pages.section", equalTo(Arrays.asList(null, "essai")));
        given().get(carnet() + "/pages/" + fils).then().statusCode(200).body("section", equalTo("essai"));

        given().contentType(ContentType.JSON).body(Map.of("parentId", aldric))
                .put(carnet() + "/pages/" + fils + "/parent").then().statusCode(400)
                .body("message", equalTo("Une page de section ne se déplace pas"));
        given().delete(carnet() + "/pages/" + fils).then().statusCode(400)
                .body("message", equalTo("Une page de section ne se supprime pas"));
        given().contentType(ContentType.JSON).body(Map.of("parentId", fils))
                .put(carnet() + "/pages/" + aldric + "/parent").then().statusCode(204);
        given().delete(carnet() + "/pages/" + aldric).then().statusCode(204);
        given().get(carnet()).then().body("pages.title", equalTo(List.of("Fils")));
    }

    @Test
    void lePortDesSections_refuseUneCampagneInconnue() {
        assertThrows(IllegalArgumentException.class, () -> sections.sectionRoots("inconnue", "essai"));
        assertThrows(IllegalArgumentException.class, () -> sections.createSectionRoot("inconnue", "essai", "Fils", null));
    }
}
