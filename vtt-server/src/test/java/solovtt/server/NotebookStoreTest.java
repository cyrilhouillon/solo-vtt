package solovtt.server;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotebookStoreTest {

    private static final String ARRIVEE = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}";

    @Test
    void enMemoire_creeUnePageVide_listeeParTitre_memeTitrePermis() {
        var store = NotebookStore.inMemory();
        var session = store.create("Session 1", null);
        var bertrand = store.create("bertrand", null);
        store.create("Session 1", null);

        assertNull(session.content());
        assertEquals(List.of("bertrand", "Session 1", "Session 1"), store.list().stream().map(Page::title).toList());
        assertEquals(Optional.of(bertrand), store.find(bertrand.id()));
        assertEquals(Optional.empty(), store.find("inconnue"));
    }

    @Test
    void enMemoire_contenuEtDernierePage() {
        var store = NotebookStore.inMemory();
        var session = store.create("Session 1", null);
        assertEquals(Optional.empty(), store.lastPage());

        store.saveContent(session.id(), ARRIVEE);
        store.setLastPage(session.id());

        assertEquals(Optional.of(new Page(session.id(), "Session 1", null, ARRIVEE)), store.find(session.id()));
        assertEquals(Optional.of(session.id()), store.lastPage());
    }

    @Test
    void enMemoire_unePageInconnue_estRefusee() {
        var store = NotebookStore.inMemory();
        assertThrows(IllegalArgumentException.class, () -> store.saveContent("inconnue", ARRIVEE));
        assertThrows(IllegalArgumentException.class, () -> store.setLastPage("inconnue"));
    }

    @Test
    void enMemoire_creerSousUnePage_renommer_deplacerAvecSesSousPages() {
        var store = NotebookStore.inMemory();
        var lore = store.create("Lore", null);
        var villes = store.create("Villes", lore.id());
        var barovie = store.create("Barovie", villes.id());
        var pnj = store.create("PNJ", null);
        store.saveContent(villes.id(), ARRIVEE);

        store.rename(villes.id(), "Cités");
        store.move(villes.id(), pnj.id());

        assertEquals(Optional.of(new Page(villes.id(), "Cités", pnj.id(), ARRIVEE)), store.find(villes.id()));
        assertEquals(villes.id(), store.find(barovie.id()).orElseThrow().parentId());
        store.move(villes.id(), null);
        assertNull(store.find(villes.id()).orElseThrow().parentId());
    }

    @Test
    void enMemoire_unePageNeVaNiSousElleMemeNiSousUneDeSesSousPages() {
        var store = NotebookStore.inMemory();
        var lore = store.create("Lore", null);
        var villes = store.create("Villes", lore.id());
        var barovie = store.create("Barovie", villes.id());

        assertThrows(IllegalArgumentException.class, () -> store.move(lore.id(), lore.id()));
        assertThrows(IllegalArgumentException.class, () -> store.move(lore.id(), barovie.id()));
        assertNull(store.find(lore.id()).orElseThrow().parentId());
    }

    @Test
    void enMemoire_unParentInconnu_estRefuse() {
        var store = NotebookStore.inMemory();
        var lore = store.create("Lore", null);

        assertThrows(IllegalArgumentException.class, () -> store.create("Villes", "inconnue"));
        assertThrows(IllegalArgumentException.class, () -> store.move(lore.id(), "inconnue"));
        assertThrows(IllegalArgumentException.class, () -> store.move("inconnue", null));
        assertThrows(IllegalArgumentException.class, () -> store.rename("inconnue", "X"));
        assertEquals(List.of(lore), store.list());
    }
}
