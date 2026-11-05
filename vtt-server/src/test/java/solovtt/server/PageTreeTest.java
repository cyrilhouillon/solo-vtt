package solovtt.server;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageTreeTest {

    private static Page page(String id, String parentId) {
        return new Page(id, id, parentId, null);
    }

    private static final List<Page> LORE = List.of(page("lore", null), page("villes", "lore"), page("barovie", "villes"),
            page("pnj", null));

    @Test
    void unePage_estDansSaPropreBranche_etDansCellesDeSesAncetres() {
        assertTrue(PageTree.isWithin(LORE, "lore", "lore"));
        assertTrue(PageTree.isWithin(LORE, "barovie", "lore"));
        assertTrue(PageTree.isWithin(LORE, "barovie", "villes"));
    }

    @Test
    void uneSoeurOuUnAncetre_nEstPasDansLaBranche() {
        assertFalse(PageTree.isWithin(LORE, "pnj", "lore"));
        assertFalse(PageTree.isWithin(LORE, "lore", "villes"));
        assertFalse(PageTree.isWithin(LORE, "inconnue", "lore"));
    }

    @Test
    void uneBoucleSurLeDisque_nArretePasLaRemontee() {
        var boucle = List.of(page("a", "b"), page("b", "a"));
        assertFalse(PageTree.isWithin(boucle, "a", "c"));
    }

    @Test
    void unArbreSain_estRenduTelQuel() {
        assertEquals(LORE, PageTree.rooted(LORE));
    }

    @Test
    void parentInconnuOuBoucle_laPageRevientALaRacine_lesAutresGardentLeurParent() {
        var pages = List.of(page("orpheline", "illisible"), page("a", "b"), page("b", "a"), page("sous-a", "a"),
                page("seule", "seule"));

        assertEquals(List.of(page("orpheline", null), page("a", null), page("b", null), page("sous-a", "a"),
                page("seule", null)), PageTree.rooted(pages));
    }

    @Test
    void laBrancheDUnePage_laPageEtSesSousPagesATouteProfondeur() {
        assertEquals(Set.of("lore", "villes", "barovie"), PageTree.branch(LORE, "lore"));
        assertEquals(Set.of("pnj"), PageTree.branch(LORE, "pnj"));
    }

    @Test
    void laBranche_neBouclePasSurUneBoucleDuDisque() {
        var boucle = List.of(page("a", "b"), page("b", "a"), page("c", null));
        assertEquals(Set.of("a", "b"), PageTree.branch(boucle, "a"));
    }
}
