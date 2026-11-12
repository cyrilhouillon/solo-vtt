package solovtt.server.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import solovtt.server.NotebookStore;
import solovtt.server.NotebookStoreTest;
import solovtt.server.Page;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFileNotebookStoreTest {

    @TempDir
    Path dir;

    private static final String ARRIVEE = "{\"type\":\"doc\",\"content\":[{\"type\":\"heading\",\"attrs\":{\"level\":1},"
            + "\"content\":[{\"type\":\"text\",\"text\":\"Arrivée\"}]},{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\","
            + "\"marks\":[{\"type\":\"bold\"}],\"text\":\"Pluie\"}]}]}";

    @Test
    void sansDossier_carnetVide() {
        var store = new JsonFileNotebookStore(dir.resolve("carnet"));
        assertEquals(List.of(), store.list());
        assertEquals(NotebookStore.Tabs.NONE, store.tabs());
    }

    @Test
    void pagesContenusEtOnglets_seRelisentParUnNouvelAdaptateur() throws IOException {
        Path carnet = dir.resolve("carnet");
        var store = new JsonFileNotebookStore(carnet);
        var session = store.create("Session 1", null);
        var bertrand = store.create("Bertrand", null);
        store.saveContent(session.id(), ARRIVEE);
        var onglets = new NotebookStore.Tabs(List.of(session.id(), bertrand.id()), session.id());
        store.setTabs(onglets);

        var relu = new JsonFileNotebookStore(carnet);
        assertEquals(List.of(bertrand, new Page(session.id(), "Session 1", null, ARRIVEE)), relu.list());
        assertEquals(onglets, relu.tabs());
        assertTrue(Files.readString(carnet.resolve(session.id() + ".json")).contains("\"content\" : {"),
                "le contenu est un objet JSON imbriqué, pas une chaîne");
        try (Stream<Path> files = Files.list(carnet)) {
            assertEquals(Stream.of(bertrand.id() + ".json", "carnet.json", session.id() + ".json").sorted().toList(),
                    files.map(f -> f.getFileName().toString()).sorted().toList());
        }
    }

    @Test
    void uneFicheIllisibleOuIncoherente_estIgnoree_etJamaisEcrasee() throws IOException {
        Path carnet = dir.resolve("carnet");
        var store = new JsonFileNotebookStore(carnet);
        var valide = store.create("Session 1", null);
        Path illisible = carnet.resolve("abimee.json");
        Files.writeString(illisible, "{pas du json");
        Files.writeString(carnet.resolve("autre.json"), "{\"id\":\"pas-autre\",\"title\":\"X\",\"content\":null}");

        assertEquals(List.of(valide), store.list());
        assertThrows(IllegalArgumentException.class, () -> store.saveContent("abimee", ARRIVEE));
        assertEquals("{pas du json", Files.readString(illisible));
    }

    @Test
    void unIdQuiNestPasCeluiDUnePage_nOuvreAucunChemin() {
        Path carnet = dir.resolve("carnet");
        var store = new JsonFileNotebookStore(carnet);
        store.create("Session 1", null);

        assertThrows(IllegalArgumentException.class, () -> store.saveContent("../campagne", ARRIVEE));
        assertThrows(IllegalArgumentException.class,
                () -> store.setTabs(new NotebookStore.Tabs(List.of("../campagne"), "../campagne")));
        assertFalse(Files.exists(dir.resolve("campagne.json")));
    }

    @Test
    void uneFicheIllisibleNommeeParUnId_nEstPasTrouvee_etJamaisEcrasee() throws IOException {
        Path carnet = dir.resolve("carnet");
        var store = new JsonFileNotebookStore(carnet);
        store.create("Session 1", null);
        String id = UUID.randomUUID().toString();
        Path abimee = carnet.resolve(id + ".json");
        Files.writeString(abimee, "{pas du json");

        assertEquals(Optional.empty(), store.find(id));
        assertThrows(IllegalArgumentException.class, () -> store.saveContent(id, ARRIVEE));
        assertEquals("{pas du json", Files.readString(abimee));
    }

    @Test
    void desOngletsIllisibles_sontVides_etJamaisEcrases() throws IOException {
        Path carnet = dir.resolve("carnet");
        Files.createDirectories(carnet);
        Path fichier = carnet.resolve("carnet.json");
        Files.writeString(fichier, "{pas du json");
        assertEquals(NotebookStore.Tabs.NONE, new JsonFileNotebookStore(carnet).tabs());
        Files.writeString(fichier, "{\"lastPageId\":null,\"openPageIds\":[null]}");
        assertEquals(NotebookStore.Tabs.NONE, new JsonFileNotebookStore(carnet).tabs());
        assertEquals("{\"lastPageId\":null,\"openPageIds\":[null]}", Files.readString(fichier));
    }

    @Test
    void unCarnetDAvantLesOnglets_seLitEnUnSeulOngletActif() throws IOException {
        Path carnet = dir.resolve("carnet");
        var store = new JsonFileNotebookStore(carnet);
        var session = store.create("Session 1", null);
        Files.writeString(carnet.resolve("carnet.json"), "{\"lastPageId\":\"" + session.id() + "\"}");

        assertEquals(new NotebookStore.Tabs(List.of(session.id()), session.id()), store.tabs());
    }

    @Test
    void desOngletsEnDoubleOuUnActifHorsDesOnglets_sontRefuses_sansRienEcrire() {
        Path carnet = dir.resolve("carnet");
        var store = new JsonFileNotebookStore(carnet);
        var session = store.create("Session 1", null);
        var bertrand = store.create("Bertrand", null);

        assertThrows(IllegalArgumentException.class,
                () -> store.setTabs(new NotebookStore.Tabs(List.of(session.id(), session.id()), session.id())));
        assertThrows(IllegalArgumentException.class,
                () -> store.setTabs(new NotebookStore.Tabs(List.of(session.id()), bertrand.id())));
        assertFalse(Files.exists(carnet.resolve("carnet.json")));
    }

    @Test
    void unArbre_seRelitParUnNouvelAdaptateur_etUnDeplacementNEcritQueLaPageDeplacee() throws IOException {
        Path carnet = dir.resolve("carnet");
        var store = new JsonFileNotebookStore(carnet);
        var lore = store.create("Lore", null);
        var villes = store.create("Villes", lore.id());
        var barovie = store.create("Barovie", villes.id());
        var pnj = store.create("PNJ", null);
        store.saveContent(villes.id(), ARRIVEE);
        String ficheBarovie = Files.readString(carnet.resolve(barovie.id() + ".json"));

        store.rename(villes.id(), "Cités");
        store.move(villes.id(), pnj.id());

        var relu = new JsonFileNotebookStore(carnet);
        assertEquals(List.of(barovie, new Page(villes.id(), "Cités", pnj.id(), ARRIVEE), lore, pnj), relu.list());
        assertEquals(ficheBarovie, Files.readString(carnet.resolve(barovie.id() + ".json")));
    }

    @Test
    void unDeplacementQuiBoucle_neChangeAucuneFiche() throws IOException {
        Path carnet = dir.resolve("carnet");
        var store = new JsonFileNotebookStore(carnet);
        var lore = store.create("Lore", null);
        var villes = store.create("Villes", lore.id());
        String ficheLore = Files.readString(carnet.resolve(lore.id() + ".json"));

        assertThrows(IllegalArgumentException.class, () -> store.move(lore.id(), villes.id()));
        assertThrows(IllegalArgumentException.class, () -> store.create("Barovie", UUID.randomUUID().toString()));
        assertEquals(ficheLore, Files.readString(carnet.resolve(lore.id() + ".json")));
        assertEquals(List.of(lore, villes), store.list());
    }

    @Test
    void uneFicheEcriteAvantLArbre_seLitALaRacine() throws IOException {
        Path carnet = dir.resolve("carnet");
        Files.createDirectories(carnet);
        String id = UUID.randomUUID().toString();
        Files.writeString(carnet.resolve(id + ".json"), "{\"id\":\"" + id + "\",\"title\":\"Session 1\",\"content\":null}");

        assertEquals(List.of(new Page(id, "Session 1", null, null)), new JsonFileNotebookStore(carnet).list());
    }

    @Test
    void laCorbeille_suitLesReglesDuPort() {
        NotebookStoreTest.corbeille(new JsonFileNotebookStore(dir.resolve("a")));
        NotebookStoreTest.placeDisparueEtLotsSepares(new JsonFileNotebookStore(dir.resolve("b")));
    }

    @Test
    void laSectionDUnModule_suitLesReglesDuPort_etSeRelit() throws IOException {
        Path carnet = dir.resolve("carnet");
        NotebookStoreTest.sections(new JsonFileNotebookStore(carnet));

        var relu = new JsonFileNotebookStore(carnet).list();
        Page fils = relu.stream().filter(Page::isSectionRoot).findFirst().orElseThrow();
        assertEquals("essai", fils.section());
        assertEquals(1, relu.stream().filter(Page::isSectionRoot).count());
        Page aldric = relu.stream().filter(p -> p.title().equals("Aldric")).findFirst().orElseThrow();
        assertFalse(Files.readString(carnet.resolve(aldric.id() + ".json")).contains("section"),
                "une page ordinaire n'écrit pas de section");
    }

    @Test
    void lesEntreesDeCombat_suiventLesReglesDuPort_etSeRelisent() {
        Path carnet = dir.resolve("carnet");
        NotebookStoreTest.entreesDeCombat(new JsonFileNotebookStore(carnet));

        var relu = new JsonFileNotebookStore(carnet);
        assertEquals(2, relu.list().stream().filter(Page::isCombatEntry).count());
    }

    @Test
    void lesEntreesDeModule_suiventLesReglesDuPort_etSeRelisent() throws IOException {
        Path carnet = dir.resolve("carnet");
        NotebookStoreTest.entreesDeModule(new JsonFileNotebookStore(carnet));

        var relu = new JsonFileNotebookStore(carnet).list();
        assertEquals(2, relu.stream().filter(Page::isModuleEntry).count());
        assertEquals("essai", relu.stream().filter(p -> p.title().equals("Borin")).findFirst().orElseThrow().entryOf());
        Page lore = relu.stream().filter(p -> p.title().equals("Lore")).findFirst().orElseThrow();
        assertFalse(Files.readString(carnet.resolve(lore.id() + ".json")).contains("entryOf"),
                "une page ordinaire n'écrit pas d'entrée");
    }

    @Test
    void laCorbeille_seRelitParUnNouvelAdaptateur() {
        Path carnet = dir.resolve("carnet");
        var store = new JsonFileNotebookStore(carnet);
        var pnj = store.create("PNJ", null);
        var bertrand = store.create("Bertrand", pnj.id());
        store.create("Fils", bertrand.id());
        store.delete(bertrand.id());

        var relu = new JsonFileNotebookStore(carnet);
        assertEquals(List.of(pnj), relu.list());
        var entree = relu.trash().getFirst();
        assertEquals(bertrand, entree.page());
        assertEquals(1, entree.subpageCount());
        assertEquals(store.trash().getFirst().deletedAt(), entree.deletedAt());
    }

    @Test
    void uneEcritureQuiEchoueEnCoursDeLot_laisseLaSuppressionALaCorbeille_etLeGesteSeRejoue() throws IOException {
        Path carnet = dir.resolve("carnet");
        var store = new JsonFileNotebookStore(carnet);
        var bertrand = store.create("Bertrand", null);
        var fils = store.create("Fils", bertrand.id());
        // Un dossier à la place du fichier voisin : l'écriture de la fiche de Fils échoue.
        Path bloque = carnet.resolve(fils.id() + ".json.tmp");

        Files.createDirectory(bloque);
        assertThrows(RuntimeException.class, () -> store.delete(bertrand.id()));
        assertEquals(List.of(bertrand.id()), store.trash().stream().map(e -> e.page().id()).toList());
        Files.delete(bloque);
        store.restore(bertrand.id());
        assertEquals(List.of(bertrand, fils), store.list());

        store.delete(bertrand.id());
        Files.createDirectory(bloque);
        assertThrows(RuntimeException.class, () -> store.restore(bertrand.id()));
        assertEquals(List.of(bertrand.id()), store.trash().stream().map(e -> e.page().id()).toList());
        Files.delete(bloque);
        store.restore(bertrand.id());
        assertEquals(List.of(bertrand, fils), store.list());
        assertTrue(store.trash().isEmpty());
    }

    private static void fiche(Path carnet, String id, String title, String marque) throws IOException {
        Files.createDirectories(carnet);
        Files.writeString(carnet.resolve(id + ".json"),
                "{\"id\":\"" + id + "\",\"title\":\"" + title + "\",\"parentId\":null,\"content\":null" + marque + "}");
    }

    @Test
    void laCorbeille_laSuppressionLaPlusRecenteDAbord() throws IOException {
        Path carnet = dir.resolve("carnet");
        String ancienne = UUID.randomUUID().toString();
        String recente = UUID.randomUUID().toString();
        fiche(carnet, ancienne, "Auberge", ",\"deletedWith\":\"" + ancienne + "\",\"deletedAt\":\"2026-11-01T10:00:00Z\"");
        fiche(carnet, recente, "Bertrand", ",\"deletedWith\":\"" + recente + "\",\"deletedAt\":\"2026-11-05T10:00:00Z\"");

        assertEquals(List.of(recente, ancienne),
                new JsonFileNotebookStore(carnet).trash().stream().map(e -> e.page().id()).toList());
    }

    @Test
    void vider_neffaceQueLesFichesALaCorbeille_jamaisUneIllisible() throws IOException {
        Path carnet = dir.resolve("carnet");
        var store = new JsonFileNotebookStore(carnet);
        var vivante = store.create("Session 1", null);
        var supprimee = store.create("Auberge", null);
        store.delete(supprimee.id());
        Path illisible = carnet.resolve(UUID.randomUUID() + ".json");
        Files.writeString(illisible, "{pas du json");

        store.emptyTrash();

        assertFalse(Files.exists(carnet.resolve(supprimee.id() + ".json")));
        assertEquals("{pas du json", Files.readString(illisible));
        assertEquals(List.of(vivante), store.list());
        assertFalse(Files.readString(carnet.resolve(vivante.id() + ".json")).contains("deleted"),
                "une page vivante n'écrit aucune marque de corbeille");
    }
}
