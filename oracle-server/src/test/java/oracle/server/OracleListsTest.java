package oracle.server;

import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Les listes de l'oracle générique : une page « Listes » dans la section de l'oracle, dont chaque sous-page est une liste,
 * et un tirage à chances égales parmi ses lignes écrites. Mots inventés.
 */
class OracleListsTest {

    private static final String STRAHD = OracleResourceTest.STRAHD;
    private static final String PHANDELVER = "phandelver";

    private final OracleResourceTest.Donnees donnees = new OracleResourceTest.Donnees();
    private final PagesDeTest pages = new PagesDeTest();

    private OracleResource oracle(int... des) {
        return new OracleResource(donnees, pages, Dice.scripted(des));
    }

    /** Une liste à puces de ces lignes ; une chaîne vide est un élément vide, un texte entre crochets un lien. */
    private static String liste(String... lignes) {
        StringBuilder items = new StringBuilder();
        for (String ligne : lignes) {
            if (!items.isEmpty()) items.append(',');
            String contenu = ligne.isEmpty() ? "" : ligne.startsWith("[")
                    ? ",\"content\":[{\"type\":\"pageLink\",\"attrs\":{\"pageId\":\"" + ligne.substring(1, ligne.length() - 1) + "\",\"label\":null}}]"
                    : ",\"content\":[{\"type\":\"text\",\"text\":\"" + ligne + "\"}]";
            items.append("{\"type\":\"listItem\",\"content\":[{\"type\":\"paragraph\"").append(contenu).append("}]}");
        }
        return "{\"type\":\"doc\",\"content\":[{\"type\":\"bulletList\",\"content\":[" + items + "]}]}";
    }

    @Test
    void lesListes_naissentUneSeuleFois_etSeRetrouventRenommees() {
        String listes = oracle().ensureLists(STRAHD).listsPageId();
        assertEquals(listes, oracle().ensureLists(STRAHD).listsPageId());
        assertEquals(1, pages.sectionRoots(STRAHD, "oracle").size());
        assertEquals(OracleResource.EMPTY_LIST, pages.sectionRootContent(STRAHD, "oracle", listes).orElseThrow());

        pages.renommer(listes, "Mes listes");
        assertEquals(listes, oracle().ensureLists(STRAHD).listsPageId());

        // Données perdues : la racine titrée Listes est reprise, sinon une page neuve.
        donnees.fichiers.clear();
        pages.renommer(listes, "Listes");
        assertEquals(listes, oracle().ensureLists(STRAHD).listsPageId());
    }

    @Test
    void lesListesSontLesSousPagesDeListes_parTitre_etPropresALaCampagne() {
        String listes = oracle().ensureLists(STRAHD).listsPageId();
        String lieux = pages.creer(STRAHD, "Lieux", listes, liste("Tour"));
        String humeurs = pages.creer(STRAHD, "humeurs", listes, liste("Calme"));
        pages.creer(STRAHD, "Sous-lieux", lieux, liste("Cave"));
        pages.creer(STRAHD, "Aldric", null, null);

        assertEquals(List.of(new OracleDtos.ListDto(humeurs, "humeurs"), new OracleDtos.ListDto(lieux, "Lieux")),
                oracle().lists(STRAHD));
        assertEquals(List.of(), oracle().lists(PHANDELVER));
        oracle().ensureLists(PHANDELVER);
        assertEquals(List.of(), oracle().lists(PHANDELVER));
        assertThrows(NotFoundException.class, () -> oracle(1).draw(PHANDELVER, lieux));
    }

    @Test
    void unTirage_choisitAChancesEgalesParmiLesLignesEcrites() {
        String listes = oracle().ensureLists(STRAHD).listsPageId();
        String lieux = pages.creer(STRAHD, "Lieux", listes, liste("Tour", "", "Port", "Forêt"));

        var port = oracle(2).draw(STRAHD, lieux);
        assertEquals("Lieux", port.list());
        assertEquals(2, port.roll());
        assertEquals(3, port.sides());
        assertEquals(3, port.line());
        assertEquals("Port", port.entryText());
        assertEquals("{\"type\":\"text\",\"text\":\"Port\"}", port.entry().getFirst().toString());

        assertEquals("Tour", oracle(1).draw(STRAHD, lieux).entryText());
        assertEquals(4, oracle(3).draw(STRAHD, lieux).line());
    }

    @Test
    void uneLigneLiee_resteUnLien_etSeLitParLeTitreDeSaCible() {
        String listes = oracle().ensureLists(STRAHD).listsPageId();
        String taverne = pages.creer(STRAHD, "Taverne du Gué", null, null);
        String lieux = pages.creer(STRAHD, "Lieux", listes, liste("[" + taverne + "]"));

        var tire = oracle(1).draw(STRAHD, lieux);
        assertEquals("Taverne du Gué", tire.entryText());
        assertEquals("pageLink", tire.entry().getFirst().path("type").asText());
        assertEquals(taverne, tire.entry().getFirst().path("attrs").path("pageId").asText());
    }

    @Test
    void uneListeSansLigneEcrite_400_sansDe() {
        String listes = oracle().ensureLists(STRAHD).listsPageId();
        String vide = pages.creer(STRAHD, "Vide", listes, liste("", "  "));
        String sansContenu = pages.creer(STRAHD, "Neuve", listes, null);

        var refus = assertThrows(IllegalArgumentException.class, () -> oracle().draw(STRAHD, vide));
        assertEquals("La liste Vide est vide : y écrire des lignes", refus.getMessage());
        assertThrows(IllegalArgumentException.class, () -> oracle().draw(STRAHD, sansContenu));
    }

    @Test
    void unePageQuiNestPasUneListe_404() {
        String listes = oracle().ensureLists(STRAHD).listsPageId();
        String lieux = pages.creer(STRAHD, "Lieux", listes, liste("Tour"));
        String cave = pages.creer(STRAHD, "Cave", lieux, liste("Rat"));
        String aldric = pages.creer(STRAHD, "Aldric", null, liste("Épée"));
        String perdue = pages.creer(STRAHD, "Perdue", listes, liste("Rien"));
        pages.supprimer(perdue);

        assertThrows(NotFoundException.class, () -> oracle(1).draw(STRAHD, cave));
        assertThrows(NotFoundException.class, () -> oracle(1).draw(STRAHD, aldric));
        assertThrows(NotFoundException.class, () -> oracle(1).draw(STRAHD, perdue));
        assertThrows(NotFoundException.class, () -> oracle(1).draw(STRAHD, listes));
        assertThrows(NotFoundException.class, () -> oracle(1).draw("inconnue", lieux));
    }
}
