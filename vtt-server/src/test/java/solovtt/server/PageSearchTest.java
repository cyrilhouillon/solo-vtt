package solovtt.server;

import org.junit.jupiter.api.Test;
import solovtt.server.NotebookStore.Fragment;
import solovtt.server.NotebookStore.SearchHit;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageSearchTest {

    private static String texte(String text) {
        return "{\"type\":\"text\",\"text\":\"" + text + "\"}";
    }

    private static String lien(String pageId, String label) {
        return "{\"type\":\"pageLink\",\"attrs\":{\"pageId\":\"" + pageId + "\",\"label\":"
                + (label == null ? "null" : "\"" + label + "\"") + "}}";
    }

    private static String paragraphe(String... noeuds) {
        return "{\"type\":\"paragraph\",\"content\":[" + String.join(",", noeuds) + "]}";
    }

    private static String document(String... blocs) {
        return "{\"type\":\"doc\",\"content\":[" + String.join(",", blocs) + "]}";
    }

    private static String liste(String... paragraphes) {
        StringBuilder items = new StringBuilder();
        for (String p : paragraphes) {
            if (!items.isEmpty()) items.append(',');
            items.append("{\"type\":\"listItem\",\"content\":[").append(p).append("]}");
        }
        return "{\"type\":\"bulletList\",\"content\":[" + items + "]}";
    }

    private static Page page(String id, String title, String content) {
        return new Page(id, title, null, content);
    }

    private static List<String> titres(List<SearchHit> hits) {
        return hits.stream().map(h -> h.page().title()).toList();
    }

    /** L'extrait lu à plat, les mots trouvés entre crochets. */
    private static String extrait(SearchHit hit) {
        return hit.excerpt().stream().map(f -> f.match() ? "[" + f.text() + "]" : f.text()).collect(Collectors.joining());
    }

    private static final Page EVEQUE = page("eveque", "Évêque Aldric", document(paragraphe(texte("Il garde la crypte"))));
    private static final Page SESSION = page("session-1", "Session 1",
            document(liste(paragraphe(texte("Nous rencontrons l'évêque à l'auberge du Gué")))));
    private static final Page LORE = page("lore", "Lore", null);
    private static final List<Page> CARNET = List.of(EVEQUE, LORE, SESSION);

    @Test
    void sansCasseNiAccents_unCaractereParCaractere() {
        assertEquals("eveque", PageSearch.fold("Évêque"));
        assertEquals("œuvre", PageSearch.fold("Œuvre"));
        assertEquals(PageSearch.fold("L'Évêque, À GUÉ").length(), "L'Évêque, À GUÉ".length());
    }

    @Test
    void lesTitresDAbord_puisLesTextes_motMarqueDansLExtrait() {
        var hits = PageSearch.search(CARNET, "eveque");
        assertEquals(List.of("Évêque Aldric", "Session 1"), titres(hits));
        assertEquals("Il garde la crypte", extrait(hits.get(0)));
        assertEquals("Nous rencontrons l'[évêque] à l'auberge du Gué", extrait(hits.get(1)));
    }

    @Test
    void chaqueMotEstExige_tousMarques() {
        var hits = PageSearch.search(CARNET, "  AUBERGE   EVEQUE ");
        assertEquals(List.of("Session 1"), titres(hits));
        assertEquals("Nous rencontrons l'[évêque] à l'[auberge] du Gué", extrait(hits.get(0)));
        assertEquals(List.of(), PageSearch.search(CARNET, "eveque dragon"));
    }

    @Test
    void unMotDuTexteSeul() {
        var hits = PageSearch.search(CARNET, "crypte");
        assertEquals(List.of("Évêque Aldric"), titres(hits));
        assertEquals("Il garde la [crypte]", extrait(hits.get(0)));
    }

    @Test
    void aucunePage_etRequeteVide() {
        assertEquals(List.of(), PageSearch.search(CARNET, "dragon"));
        assertEquals(List.of(), PageSearch.search(CARNET, "   "));
    }

    @Test
    void titreSeul_pageSansContenu_extraitVide() {
        var hits = PageSearch.search(CARNET, "lore");
        assertEquals(List.of("Lore"), titres(hits));
        assertEquals(List.of(), hits.get(0).excerpt());
    }

    @Test
    void unLienSeTrouveSousLeTexteAffiche() {
        var sansTexte = page("s2", "Session 2", document(paragraphe(texte("Visite chez "), lien("eveque", null))));
        var avecTexte = page("s3", "Session 3", document(paragraphe(texte("Visite chez "), lien("eveque", "le prélat"))));
        var pages = List.of(EVEQUE, sansTexte, avecTexte);

        assertEquals(List.of("Évêque Aldric", "Session 2"), titres(PageSearch.search(pages, "aldric")));
        var hits = PageSearch.search(pages, "prelat");
        assertEquals(List.of("Session 3"), titres(hits));
        assertEquals("Visite chez le [prélat]", extrait(hits.get(0)));
    }

    @Test
    void deuxBlocsSontSeparesParUnEspace() {
        var pages = List.of(page("p", "Page", document(paragraphe(texte("fin")), paragraphe(texte("debut")))));
        assertEquals(List.of(), PageSearch.search(pages, "findebut"));
        assertEquals("[fin] debut", extrait(PageSearch.search(pages, "fin").get(0)));
    }

    @Test
    void unTexteLong_extraitCoupeAuxEspaces_pointsDeSuspension() {
        String avant = "mot ".repeat(30);
        String apres = " autre".repeat(40);
        var pages = List.of(page("p", "Page", document(paragraphe(texte(avant + "gargouille" + apres)))));
        var hit = PageSearch.search(pages, "gargouille").get(0);
        List<Fragment> fragments = hit.excerpt();

        assertEquals("…", fragments.get(0).text());
        assertEquals("…", fragments.get(fragments.size() - 1).text());
        String lu = extrait(hit);
        assertTrue(lu.startsWith("…mot "), lu);
        assertTrue(lu.endsWith(" autre…"), lu);
        assertTrue(lu.contains("[gargouille]"), lu);
        assertTrue(lu.length() <= PageSearch.BEFORE + PageSearch.AFTER + "gargouille".length() + 4, lu);
    }

    @Test
    void titreSeul_extraitDuDebutDuTexte() {
        String texte = "phrase ".repeat(40);
        var pages = List.of(page("p", "Gargouille", document(paragraphe(texte(texte)))));
        String lu = extrait(PageSearch.search(pages, "gargouille").get(0));
        assertTrue(lu.startsWith("phrase phrase"), lu);
        assertTrue(lu.endsWith("phrase…"), lu);
        assertTrue(lu.length() <= PageSearch.START + 1, lu);
    }

    @Test
    void unContenuIllisible_chercheSurLeTitreSeul() {
        var illisible = page("x", "Crypte oubliée", "{pas du json");
        var pages = List.of(illisible, EVEQUE);
        assertEquals(List.of("Crypte oubliée", "Évêque Aldric"), titres(PageSearch.search(pages, "crypte")));
        assertEquals(List.of(), PageSearch.search(List.of(illisible), "json"));
    }
}
