package solovtt.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import solovtt.server.NotebookStore.Tabs;
import solovtt.server.persistence.FileCampaignStore;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La preuve serveur de l'epic « tenir le carnet de sa campagne » : une session rangée sous « Sessions » lie deux PNJ ;
 * renommer l'un ne réécrit pas la session, qui reste citante ; la recherche la retrouve ; « PNJ » et ses sous-pages
 * partent à la corbeille puis en reviennent, liens compris ; un nouveau stockage sur le même dossier (redémarrage)
 * relit tout à l'identique.
 */
class CarnetDeBoutEnBoutTest {

    @TempDir
    Path dir;

    private static String document(String ismark, String ireena) {
        return "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":["
                + "{\"type\":\"text\",\"text\":\"À l'auberge du Sang de la Vigne, \"},"
                + "{\"type\":\"pageLink\",\"attrs\":{\"pageId\":\"" + ismark + "\",\"label\":null}},"
                + "{\"type\":\"text\",\"text\":\" nous présente \"},"
                + "{\"type\":\"pageLink\",\"attrs\":{\"pageId\":\"" + ireena + "\",\"label\":\"sa sœur\"}}"
                + "]}]}";
    }

    private static List<String> titres(List<Page> pages) {
        return pages.stream().map(Page::title).toList();
    }

    private static List<String> titresTrouves(NotebookStore carnet, String query) {
        return carnet.search(query).stream().map(h -> h.page().title()).toList();
    }

    @Test
    void uneSessionLieeDeuxPnj_renommeeCherchee_miseALaCorbeillePuisRestauree_seRelitALIdentique() {
        var campagnes = new FileCampaignStore(dir);
        Campaign barovie = campagnes.create("Barovie");
        NotebookStore carnet = campagnes.notebook(barovie.id());

        Page sessions = carnet.create("Sessions", null);
        Page pnj = carnet.create("PNJ", null);
        Page ismark = carnet.create("Ismark", pnj.id());
        Page ireena = carnet.create("Ireena", pnj.id());
        Page session5 = carnet.create("Session 5", sessions.id());
        String contenu = document(ismark.id(), ireena.id());
        carnet.saveContent(session5.id(), contenu);
        carnet.setTabs(new Tabs(List.of(session5.id(), ismark.id()), session5.id()));

        assertEquals(contenu, carnet.find(session5.id()).orElseThrow().content());
        assertEquals(sessions.id(), carnet.find(session5.id()).orElseThrow().parentId());
        assertEquals(pnj.id(), carnet.find(ismark.id()).orElseThrow().parentId());
        assertEquals(new Tabs(List.of(session5.id(), ismark.id()), session5.id()), carnet.tabs());

        // Renommer le PNJ ne réécrit pas la session, qui le cite toujours.
        carnet.rename(ismark.id(), "Ismark le Petit");
        assertEquals(contenu, carnet.find(session5.id()).orElseThrow().content());
        assertEquals(List.of("Session 5"), titres(PageLinks.citing(carnet.list(), ismark.id())));
        assertEquals(List.of("Session 5"), titres(PageLinks.citing(carnet.list(), ireena.id())));

        // La recherche retrouve la session par son texte, et le PNJ par son titre, en premier.
        assertEquals(List.of("Session 5"), titresTrouves(carnet, "auberge"));
        assertEquals("Ismark le Petit", titresTrouves(carnet, "ismark petit").getFirst());
        assertTrue(titresTrouves(carnet, "ismark petit").contains("Session 5"));

        // PNJ et ses sous-pages partent à la corbeille en un lot, puis en reviennent à leur place.
        carnet.delete(pnj.id());
        assertEquals(List.of("Session 5", "Sessions"), titres(carnet.list()));
        assertEquals(1, carnet.trash().size());
        assertEquals(pnj.id(), carnet.trash().getFirst().page().id());
        assertEquals(2, carnet.trash().getFirst().subpageCount());

        carnet.restore(pnj.id());
        assertEquals(List.of("Ireena", "Ismark le Petit", "PNJ", "Session 5", "Sessions"), titres(carnet.list()));
        assertEquals(pnj.id(), carnet.find(ismark.id()).orElseThrow().parentId());
        assertEquals(pnj.id(), carnet.find(ireena.id()).orElseThrow().parentId());
        assertTrue(carnet.trash().isEmpty());
        assertEquals(List.of("Session 5"), titres(PageLinks.citing(carnet.list(), ismark.id())));

        // Redémarrage : un nouveau stockage sur le même dossier relit tout à l'identique.
        var relues = new FileCampaignStore(dir);
        assertEquals(List.of(barovie), relues.list());
        NotebookStore relu = relues.notebook(barovie.id());
        assertEquals(carnet.list(), relu.list());
        assertEquals(carnet.tabs(), relu.tabs());
        assertTrue(relu.trash().isEmpty());
        assertEquals(List.of("Session 5"), titres(PageLinks.citing(relu.list(), ismark.id())));
        assertEquals(List.of("Session 5"), titresTrouves(relu, "auberge"));
        assertEquals(new NotebookResource(carnet).get(), new NotebookResource(relu).get());
    }

    @Test
    void unLotLaisseALaCorbeille_survitAuRedemarrage_etSonOngletEstOmis() {
        var campagnes = new FileCampaignStore(dir);
        Campaign barovie = campagnes.create("Barovie");
        NotebookStore carnet = campagnes.notebook(barovie.id());
        Page sessions = carnet.create("Sessions", null);
        Page pnj = carnet.create("PNJ", null);
        Page ismark = carnet.create("Ismark", pnj.id());
        Page ireena = carnet.create("Ireena", pnj.id());
        Page session5 = carnet.create("Session 5", sessions.id());
        carnet.saveContent(session5.id(), document(ismark.id(), ireena.id()));
        carnet.setTabs(new Tabs(List.of(session5.id(), ismark.id()), ismark.id()));

        carnet.delete(pnj.id());

        NotebookStore relu = new FileCampaignStore(dir).notebook(barovie.id());
        assertEquals(List.of("Session 5", "Sessions"), titres(relu.list()));
        assertEquals(1, relu.trash().size());
        assertEquals(2, relu.trash().getFirst().subpageCount());
        assertTrue(relu.find(ismark.id()).isEmpty());
        assertEquals(new Tabs(List.of(session5.id(), ismark.id()), ismark.id()), relu.tabs());

        Dtos.NotebookDto vu = new NotebookResource(relu).get();
        assertEquals(List.of(session5.id()), vu.openPageIds());
        assertEquals(null, vu.activePageId());

        relu.restore(pnj.id());
        assertEquals(List.of("Session 5"), titres(PageLinks.citing(relu.list(), ismark.id())));
        assertEquals(List.of(session5.id(), ismark.id()), new NotebookResource(relu).get().openPageIds());
    }
}
