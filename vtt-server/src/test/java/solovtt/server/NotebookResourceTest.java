package solovtt.server;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NotebookResourceTest {

    /** Le port refuse d'écrire un onglet sur une page inconnue : seul un fichier abîmé ou une page disparue en laisse un. */
    private static NotebookStore avecOnglets(NotebookStore pages, NotebookStore.Tabs onglets) {
        return new NotebookStore() {
            @Override
            public List<Page> list() {
                return pages.list();
            }

            @Override
            public Page create(String title, String parentId) {
                return pages.create(title, parentId);
            }

            @Override
            public void saveContent(String id, String content) {
                pages.saveContent(id, content);
            }

            @Override
            public void rename(String id, String title) {
                pages.rename(id, title);
            }

            @Override
            public void move(String id, String parentId) {
                pages.move(id, parentId);
            }

            @Override
            public void delete(String id) {
                pages.delete(id);
            }

            @Override
            public List<TrashEntry> trash() {
                return pages.trash();
            }

            @Override
            public void restore(String id) {
                pages.restore(id);
            }

            @Override
            public void emptyTrash() {
                pages.emptyTrash();
            }

            @Override
            public Tabs tabs() {
                return onglets;
            }

            @Override
            public void setTabs(Tabs tabs) {
                pages.setTabs(tabs);
            }
        };
    }

    @Test
    void unOngletSurUnePageDisparue_estOmis_etNActiveRien() {
        var pages = NotebookStore.inMemory();
        var session = pages.create("Session 1", null);
        var carnet = new NotebookResource(avecOnglets(pages,
                new NotebookStore.Tabs(List.of("disparue", session.id()), "disparue")));

        var lu = carnet.get();

        assertEquals(List.of(session.id()), lu.openPageIds());
        assertNull(lu.activePageId());
    }

    @Test
    void unOngletActifVivant_resteActif() {
        var pages = NotebookStore.inMemory();
        var session = pages.create("Session 1", null);
        var carnet = new NotebookResource(avecOnglets(pages,
                new NotebookStore.Tabs(List.of(session.id(), "disparue"), session.id())));

        assertEquals(session.id(), carnet.get().activePageId());
    }
}
