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
        var session = store.create("Session 1");
        var bertrand = store.create("bertrand");
        store.create("Session 1");

        assertNull(session.content());
        assertEquals(List.of("bertrand", "Session 1", "Session 1"), store.list().stream().map(Page::title).toList());
        assertEquals(Optional.of(bertrand), store.find(bertrand.id()));
        assertEquals(Optional.empty(), store.find("inconnue"));
    }

    @Test
    void enMemoire_contenuEtDernierePage() {
        var store = NotebookStore.inMemory();
        var session = store.create("Session 1");
        assertEquals(Optional.empty(), store.lastPage());

        store.saveContent(session.id(), ARRIVEE);
        store.setLastPage(session.id());

        assertEquals(Optional.of(new Page(session.id(), "Session 1", ARRIVEE)), store.find(session.id()));
        assertEquals(Optional.of(session.id()), store.lastPage());
    }

    @Test
    void enMemoire_unePageInconnue_estRefusee() {
        var store = NotebookStore.inMemory();
        assertThrows(IllegalArgumentException.class, () -> store.saveContent("inconnue", ARRIVEE));
        assertThrows(IllegalArgumentException.class, () -> store.setLastPage("inconnue"));
    }
}
