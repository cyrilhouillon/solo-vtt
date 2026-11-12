package solovtt.server;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NotebookStoreTest {

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
    void enMemoire_contenuEtOnglets() {
        var store = NotebookStore.inMemory();
        var session = store.create("Session 1", null);
        var bertrand = store.create("Bertrand", null);
        assertEquals(NotebookStore.Tabs.NONE, store.tabs());

        store.saveContent(session.id(), ARRIVEE);
        var tabs = new NotebookStore.Tabs(List.of(session.id(), bertrand.id()), bertrand.id());
        store.setTabs(tabs);

        assertEquals(Optional.of(new Page(session.id(), "Session 1", null, ARRIVEE)), store.find(session.id()));
        assertEquals(tabs, store.tabs());
        store.setTabs(NotebookStore.Tabs.NONE);
        assertEquals(NotebookStore.Tabs.NONE, store.tabs());
    }

    @Test
    void enMemoire_unePageInconnue_estRefusee() {
        var store = NotebookStore.inMemory();
        assertThrows(IllegalArgumentException.class, () -> store.saveContent("inconnue", ARRIVEE));
        assertThrows(IllegalArgumentException.class,
                () -> store.setTabs(new NotebookStore.Tabs(List.of("inconnue"), "inconnue")));
    }

    @Test
    void enMemoire_desOngletsEnDoubleOuUnActifHorsDesOnglets_sontRefuses() {
        var store = NotebookStore.inMemory();
        var session = store.create("Session 1", null);
        var bertrand = store.create("Bertrand", null);
        var gardes = new NotebookStore.Tabs(List.of(session.id()), session.id());
        store.setTabs(gardes);

        assertThrows(IllegalArgumentException.class,
                () -> store.setTabs(new NotebookStore.Tabs(List.of(session.id(), session.id()), session.id())));
        assertThrows(IllegalArgumentException.class,
                () -> store.setTabs(new NotebookStore.Tabs(List.of(session.id()), bertrand.id())));
        assertThrows(IllegalArgumentException.class, () -> store.setTabs(new NotebookStore.Tabs(List.of(session.id()), null)));
        assertThrows(IllegalArgumentException.class, () -> store.setTabs(new NotebookStore.Tabs(List.of(), session.id())));
        assertEquals(gardes, store.tabs());
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

    @Test
    void enMemoire_supprimer_puisRestaurer_laPageEtSesSousPages() {
        corbeille(NotebookStore.inMemory());
    }

    @Test
    void enMemoire_restaurerSansSaPlace_ouUnLotAPart_puisVider() {
        placeDisparueEtLotsSepares(NotebookStore.inMemory());
    }

    /** Les règles de la corbeille, communes aux adaptateurs. */
    public static void corbeille(NotebookStore store) {
        var pnj = store.create("PNJ", null);
        var bertrand = store.create("Bertrand", pnj.id());
        var fils = store.create("Fils", bertrand.id());
        var session = store.create("Session 1", null);
        store.saveContent(fils.id(), ARRIVEE);

        store.delete(bertrand.id());

        assertEquals(List.of(pnj, session), store.list());
        assertEquals(Optional.empty(), store.find(fils.id()));
        assertThrows(IllegalArgumentException.class, () -> store.saveContent(bertrand.id(), ARRIVEE));
        assertThrows(IllegalArgumentException.class, () -> store.create("Petit-fils", fils.id()));
        assertThrows(IllegalArgumentException.class, () -> store.delete(bertrand.id()));
        var entrees = store.trash();
        assertEquals(1, entrees.size());
        assertEquals(bertrand, entrees.getFirst().page());
        assertEquals(1, entrees.getFirst().subpageCount());
        assertThrows(IllegalArgumentException.class, () -> store.restore(fils.id()));
        assertThrows(IllegalArgumentException.class, () -> store.restore(session.id()));

        store.restore(bertrand.id());

        assertEquals(List.of(bertrand, new Page(fils.id(), "Fils", bertrand.id(), ARRIVEE), pnj, session), store.list());
        assertEquals(List.of(), store.trash());
    }

    public static void placeDisparueEtLotsSepares(NotebookStore store) {
        var pnj = store.create("PNJ", null);
        var bertrand = store.create("Bertrand", pnj.id());
        var fils = store.create("Fils", bertrand.id());

        store.delete(fils.id());
        store.delete(bertrand.id());
        store.delete(pnj.id());
        assertEquals(3, store.trash().size());
        store.restore(bertrand.id());

        assertEquals(List.of(new Page(bertrand.id(), "Bertrand", null, null)), store.list());
        // Fils, supprimé à part avant Bertrand, reste son propre lot.
        assertEquals(Set.of(pnj.id(), fils.id()), store.trash().stream().map(e -> e.page().id()).collect(Collectors.toSet()));

        store.emptyTrash();
        assertEquals(List.of(), store.trash());
        assertThrows(IllegalArgumentException.class, () -> store.restore(pnj.id()));
        assertEquals(List.of(new Page(bertrand.id(), "Bertrand", null, null)), store.list());
    }

    @Test
    void enMemoire_laSectionDUnModule_suitLesReglesDuPort() {
        sections(NotebookStore.inMemory());
    }

    /**
     * Une racine de section garde sa section et son contenu, se renomme, mais ne se déplace ni ne se supprime ; ses
     * sous-pages vont et viennent comme toute page.
     */
    public static void sections(NotebookStore store) {
        var fils = store.createSectionRoot("essai", "Fils", ARRIVEE);
        var aldric = store.create("Aldric", null);
        assertEquals(new Page(fils.id(), "Fils", null, "essai", ARRIVEE), fils);
        assertEquals(List.of(aldric, fils), store.list());

        store.rename(fils.id(), "Intrigues");
        var sousPage = store.create("La tour", fils.id());
        store.move(aldric.id(), fils.id());
        store.move(sousPage.id(), null);
        store.move(sousPage.id(), fils.id());
        assertEquals(Optional.of(new Page(aldric.id(), "Aldric", fils.id(), null)), store.find(aldric.id()));
        store.delete(sousPage.id());
        store.restore(sousPage.id());
        assertEquals(Optional.of(new Page(sousPage.id(), "La tour", fils.id(), null)), store.find(sousPage.id()));

        var avant = store.list();
        var deplacer = assertThrows(IllegalArgumentException.class, () -> store.move(fils.id(), aldric.id()));
        assertEquals("Une page de section ne se déplace pas", deplacer.getMessage());
        assertThrows(IllegalArgumentException.class, () -> store.move(fils.id(), null));
        var supprimer = assertThrows(IllegalArgumentException.class, () -> store.delete(fils.id()));
        assertEquals("Une page de section ne se supprime pas", supprimer.getMessage());
        assertEquals(avant, store.list());
        assertEquals(List.of(), store.trash());
        assertEquals("Intrigues", store.find(fils.id()).orElseThrow().title());
        assertEquals("essai", store.find(fils.id()).orElseThrow().section());

        assertThrows(IllegalArgumentException.class, () -> store.createSectionRoot("../x", "Fils", null));
        assertThrows(IllegalArgumentException.class, () -> store.createSectionRoot("A", "Fils", null));
    }

    @Test
    void enMemoire_lesEntreesDeCombat_suiventLesReglesDuPort() {
        entreesDeCombat(NotebookStore.inMemory());
    }

    private static final String COMBAT = "0f8fad5b-d9cb-469f-a165-70867728950e";
    private static final String COMBAT_2 = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

    /**
     * Une entrée de combat va dans la section des combats, créée au premier ; elle se renomme, se déplace, se supprime et
     * se restaure comme une page, mais n'a ni texte ni sous-page.
     */
    public static void entreesDeCombat(NotebookStore store) {
        var premier = store.createCombatEntry(COMBAT, "Combat 1");
        var second = store.createCombatEntry(COMBAT_2, "Combat 2");
        var racine = store.find(premier.parentId()).orElseThrow();
        assertEquals("Combats", racine.title());
        assertEquals(NotebookStore.COMBATS_SECTION, racine.section());
        assertEquals(racine.id(), second.parentId());
        assertEquals(new Page(premier.id(), "Combat 1", racine.id(), null, null, COMBAT), premier);
        assertNotEquals(COMBAT, premier.id());

        store.rename(premier.id(), "Embuscade");
        var session = store.create("Session 1", null);
        store.move(premier.id(), session.id());
        assertEquals(Optional.of(new Page(premier.id(), "Embuscade", session.id(), null, null, COMBAT)), store.find(premier.id()));
        store.delete(premier.id());
        assertEquals(Optional.empty(), store.find(premier.id()));
        store.restore(premier.id());
        assertEquals(COMBAT, store.find(premier.id()).orElseThrow().combatId());

        var texte = assertThrows(IllegalArgumentException.class, () -> store.saveContent(second.id(), ARRIVEE));
        assertEquals("Une entrée de combat n'a pas de texte", texte.getMessage());
        assertThrows(IllegalArgumentException.class, () -> store.create("Notes", second.id()));
        assertThrows(IllegalArgumentException.class, () -> store.move(session.id(), second.id()));
        assertNull(store.find(second.id()).orElseThrow().content());
        assertEquals(1, store.list().stream().filter(p -> NotebookStore.COMBATS_SECTION.equals(p.section())).count());
        assertThrows(IllegalArgumentException.class, () -> store.createCombatEntry("../x", "Combat 3"));
    }

    @Test
    void enMemoire_lesEntreesDeModule_suiventLesReglesDuPort() {
        entreesDeModule(NotebookStore.inMemory());
    }

    /**
     * Une entrée de module se range dans la section de son module ; elle se renomme, se déplace, se supprime, se restaure
     * et se trouve par son titre comme une page, mais n'a ni texte ni sous-page.
     */
    public static void entreesDeModule(NotebookStore store) {
        var joueurs = store.createSectionRoot("essai", "Personnages joueurs", null);
        var lore = store.create("Lore", joueurs.id());
        var aria = store.createEntry("essai", joueurs.id(), "Aria");
        var borin = store.createEntry("essai", lore.id(), "Borin");

        assertEquals(new Page(aria.id(), "Aria", joueurs.id(), null, null, null, "essai"), aria);
        assertEquals(Optional.of(borin), store.find(borin.id()));
        assertEquals("Borin", store.search("borin").getFirst().page().title());

        store.rename(aria.id(), "Aria la Rouge");
        var session = store.create("Session 1", null);
        store.move(aria.id(), session.id());
        assertEquals(Optional.of(new Page(aria.id(), "Aria la Rouge", session.id(), null, null, null, "essai")),
                store.find(aria.id()));
        store.delete(aria.id());
        assertEquals(Optional.empty(), store.find(aria.id()));
        store.restore(aria.id());
        assertEquals("essai", store.find(aria.id()).orElseThrow().entryOf());

        var texte = assertThrows(IllegalArgumentException.class, () -> store.saveContent(borin.id(), ARRIVEE));
        assertEquals("Une entrée de module n'a pas de texte", texte.getMessage());
        var sousPage = assertThrows(IllegalArgumentException.class, () -> store.create("Notes", borin.id()));
        assertEquals("Une entrée de module n'a pas de sous-page", sousPage.getMessage());
        assertThrows(IllegalArgumentException.class, () -> store.move(session.id(), borin.id()));
        assertThrows(IllegalArgumentException.class, () -> store.createEntry("essai", borin.id(), "Cael"));
        assertNull(store.find(borin.id()).orElseThrow().content());

        var horsSection = assertThrows(IllegalArgumentException.class, () -> store.createEntry("essai", session.id(), "Cael"));
        assertEquals("Une entrée se range dans la section de son module", horsSection.getMessage());
        assertThrows(IllegalArgumentException.class, () -> store.createEntry("essai", null, "Cael"));
        assertThrows(IllegalArgumentException.class, () -> store.createEntry("autre", joueurs.id(), "Cael"));
        assertThrows(IllegalArgumentException.class, () -> store.createEntry("../x", joueurs.id(), "Cael"));
        assertThrows(IllegalArgumentException.class, () -> store.createEntry(NotebookStore.COMBATS_SECTION, joueurs.id(), "Cael"));
        assertEquals(2, store.list().stream().filter(Page::isModuleEntry).count());
    }
}
