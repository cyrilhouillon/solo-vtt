package mythic.server;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListLinesTest {

    private static final Map<String, String> TITRES = Map.of("vessa", "Vessa");

    private static List<ListLines.Line> lignes(String json) {
        return ListLines.of(json, id -> Optional.ofNullable(TITRES.get(id)));
    }

    private static List<String> textes(List<ListLines.Line> lignes) {
        return lignes.stream().map(ListLines.Line::text).toList();
    }

    private static List<Boolean> ecrites(List<ListLines.Line> lignes) {
        return lignes.stream().map(ListLines.Line::written).toList();
    }

    @Test
    void chaqueElementDeListe_estUneLigne_videComprise_etUnLienSeLitParSonTitre() {
        var lignes = lignes("""
                {"type":"doc","content":[{"type":"bulletList","content":[
                  {"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"Garde ","marks":[{"type":"bold"}]},{"type":"text","text":"Ylo"}]}]},
                  {"type":"listItem","content":[{"type":"paragraph"}]},
                  {"type":"listItem","content":[{"type":"paragraph","content":[{"type":"pageLink","attrs":{"pageId":"vessa","label":null}}]}]},
                  {"type":"listItem","content":[{"type":"paragraph","content":[{"type":"pageLink","attrs":{"pageId":"vessa","label":"la sorcière"}}]}]},
                  {"type":"listItem","content":[{"type":"paragraph","content":[{"type":"pageLink","attrs":{"pageId":"perdue","label":null}}]}]}
                ]}]}""");

        assertEquals(List.of("Garde Ylo", "", "Vessa", "la sorcière", ListLines.MISSING_PAGE), textes(lignes));
        assertEquals(List.of(true, false, true, true, true), ecrites(lignes));
        assertEquals("pageLink", lignes.get(2).inline().get(0).path("type").asText());
        assertEquals("bold", lignes.get(0).inline().get(0).path("marks").path(0).path("type").asText());
    }

    @Test
    void leTexteHorsDesListes_etLesSousListes_neSontPasDesLignes() {
        var lignes = lignes("""
                {"type":"doc","content":[
                  {"type":"paragraph","content":[{"type":"text","text":"Mes fils"}]},
                  {"type":"bulletList","content":[
                    {"type":"listItem","content":[
                      {"type":"paragraph","content":[{"type":"text","text":"Trouver la tour"}]},
                      {"type":"bulletList","content":[{"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"note"}]}]}]}
                    ]}
                  ]},
                  {"type":"blockquote","content":[{"type":"paragraph","content":[{"type":"text","text":"cité"}]}]},
                  {"type":"orderedList","content":[{"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"  Sauver   Ireena "}]}]}]}
                ]}""");

        assertEquals(List.of("Trouver la tour", "Sauver Ireena"), textes(lignes));
    }

    @Test
    void desEspacesSeuls_fontUneLigneVide() {
        var lignes = lignes("""
                {"type":"doc","content":[{"type":"bulletList","content":[
                  {"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"   "}]}]}
                ]}]}""");

        assertEquals(List.of(false), ecrites(lignes));
    }

    @Test
    void unContenuIllisibleOuAbsent_naAucuneLigne() {
        assertEquals(List.of(), lignes("{pas du json"));
        assertEquals(List.of(), lignes(null));
        assertEquals(List.of(), lignes("{\"type\":\"doc\"}"));
    }
}
