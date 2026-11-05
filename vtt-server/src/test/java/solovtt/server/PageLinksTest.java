package solovtt.server;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageLinksTest {

    private static String lien(String pageId) {
        return "{\"type\":\"pageLink\",\"attrs\":{\"pageId\":\"" + pageId + "\",\"label\":null}}";
    }

    private static String paragraphe(String... noeuds) {
        return "{\"type\":\"paragraph\",\"content\":[" + String.join(",", noeuds) + "]}";
    }

    private static String document(String... blocs) {
        return "{\"type\":\"doc\",\"content\":[" + String.join(",", blocs) + "]}";
    }

    private static Page page(String id, String content) {
        return new Page(id, id, null, content);
    }

    private static List<String> citantes(List<Page> pages, String id) {
        return PageLinks.citing(pages, id).stream().map(Page::id).toList();
    }

    @Test
    void unLienDansUnParagraphe_citeLaPage() {
        var pages = List.of(page("bertrand", null), page("session-1", document(paragraphe(lien("bertrand")))));
        assertEquals(List.of("session-1"), citantes(pages, "bertrand"));
    }

    @Test
    void unLienDansUneListeImbriquee_citeLaPage() {
        String liste = "{\"type\":\"bulletList\",\"content\":[{\"type\":\"listItem\",\"content\":["
                + paragraphe("{\"type\":\"text\",\"text\":\"PNJ : \"}", lien("bertrand")) + "]}]}";
        var pages = List.of(page("session-5", document(liste)));
        assertEquals(List.of("session-5"), citantes(pages, "bertrand"));
    }

    @Test
    void deuxLiensDansUnePage_uneSeuleFois_dansLOrdreRecu() {
        var pages = List.of(page("session-1", document(paragraphe(lien("bertrand")))),
                page("session-5", document(paragraphe(lien("bertrand")), paragraphe(lien("bertrand")))));
        assertEquals(List.of("session-1", "session-5"), citantes(pages, "bertrand"));
    }

    @Test
    void unLienVersUneAutrePage_neCitePas() {
        var pages = List.of(page("lore", document(paragraphe(lien("barovie")))), page("vide", null));
        assertEquals(List.of(), citantes(pages, "bertrand"));
    }

    @Test
    void unePageQuiSeCite_nEstPasDansSaListe() {
        var pages = List.of(page("bertrand", document(paragraphe(lien("bertrand")))));
        assertEquals(List.of(), citantes(pages, "bertrand"));
    }

    @Test
    void unContenuIllisible_estSaute() {
        var pages = List.of(page("abime", "{pas du json"), page("session-1", document(paragraphe(lien("bertrand")))));
        assertEquals(List.of("session-1"), citantes(pages, "bertrand"));
    }

    @Test
    void lesPagesQuiCitentUnLot_horsDuLot_uneFoisChacune() {
        var pages = List.of(page("bertrand", document(paragraphe(lien("fils")))),
                page("fils", document(paragraphe(lien("bertrand")))),
                page("session-1", document(paragraphe(lien("bertrand"), lien("fils")))),
                page("auberge", document(paragraphe(lien("fils")))), page("vide", null));
        var citantes = PageLinks.citingAny(pages, Set.of("bertrand", "fils")).stream().map(Page::id).toList();
        assertEquals(List.of("session-1", "auberge"), citantes);
    }
}
