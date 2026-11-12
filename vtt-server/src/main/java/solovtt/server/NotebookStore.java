package solovtt.server;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Où vit le Carnet d'une campagne : ses pages, rangées en arbre par leur parent, ses onglets ouverts et sa corbeille. Un
 * déplacement ne touche que la page déplacée : ses sous-pages la suivent. Une page à la corbeille n'est vue que par
 * {@link #trash()} : ni {@link #list()} ni {@link #find} ne la rendent.
 */
public interface NotebookStore {

    /**
     * Les pages, triées par titre.
     *
     * @throws RuntimeException si les pages existent mais ne se lisent pas
     */
    List<Page> list();

    /** La page de cet id, vide si aucune. */
    default Optional<Page> find(String id) {
        return list().stream().filter(p -> p.id().equals(id)).findFirst();
    }

    /**
     * Crée une page vide sous ce titre, sous {@code parentId} ({@code null} : à la racine) ; deux pages peuvent porter le
     * même titre.
     *
     * @throws IllegalArgumentException si aucune page n'a l'id du parent, ou si le parent est une entrée de combat
     */
    Page create(String title, String parentId);

    /**
     * Crée, à la racine, une racine de la section de ce module, avec ce contenu ({@code null} : vide). Seul un module en
     * crée, par le port {@code NotebookPages} ; elle ne se déplace ni ne se supprime.
     *
     * @throws IllegalArgumentException si l'id de section n'est pas un id de module
     */
    Page createSectionRoot(String section, String title, String content);

    /** La section du Carnet que tient le cœur pour les entrées de combat. */
    String COMBATS_SECTION = "combats";

    /**
     * Crée l'entrée du combat {@code combatId} sous ce titre, dans la section des combats, dont la racine « Combats » est
     * créée si aucune n'est vivante. L'entrée a son propre id ; elle se renomme, se déplace et se supprime comme une page.
     */
    Page createCombatEntry(String combatId, String title);

    /**
     * Remplace le contenu de la page.
     *
     * @throws IllegalArgumentException si aucune page n'a cet id, ou si c'est une entrée de combat
     */
    void saveContent(String id, String content);

    /**
     * Renomme la page.
     *
     * @throws IllegalArgumentException si aucune page n'a cet id
     */
    void rename(String id, String title);

    /**
     * Range la page sous {@code parentId} ({@code null} : à la racine), avec ses sous-pages.
     *
     * @throws IllegalArgumentException si la page ou le parent est inconnu, si le parent est la page elle-même ou l'une
     *     de ses sous-pages, ou si la page est une racine de section
     */
    void move(String id, String parentId);

    /**
     * Met la page et ses sous-pages à la corbeille, en un seul lot.
     *
     * @throws IllegalArgumentException si aucune page vivante n'a cet id, ou si c'est une racine de section
     */
    void delete(String id);

    /** Les suppressions à la corbeille, une par page supprimée par le joueur, la plus récente d'abord. */
    List<TrashEntry> trash();

    /**
     * Sort de la corbeille la page supprimée sous cet id, avec les sous-pages parties avec elle. Elle reprend son parent
     * s'il est vivant, sinon la racine.
     *
     * @throws IllegalArgumentException si {@code id} n'est pas une suppression de la corbeille
     */
    void restore(String id);

    /** Efface pour de bon tout ce qui est à la corbeille. */
    void emptyTrash();

    /** Une suppression de la corbeille : la page supprimée par le joueur, quand, et combien de sous-pages sont parties avec elle. */
    record TrashEntry(Page page, Instant deletedAt, int subpageCount) {}

    /** Une page à la corbeille : {@code deletedWith} est la page dont la suppression l'a emportée (la sienne pour elle). */
    record Trashed(Page page, String deletedWith, Instant deletedAt) {}

    /** Les onglets ouverts, {@link Tabs#NONE} si aucun ; un onglet peut citer une page disparue depuis. */
    Tabs tabs();

    /**
     * Garde les onglets ouverts et l'actif.
     *
     * @throws IllegalArgumentException si une page est inconnue ou en double, ou si l'actif n'est pas l'un des onglets
     */
    void setTabs(Tabs tabs);

    /**
     * Les pages ouvertes en onglets, dans l'ordre de la barre, et celle de l'onglet actif : {@code null} si et seulement si
     * aucun onglet n'est ouvert.
     */
    record Tabs(List<String> pageIds, String activeId) {
        public static final Tabs NONE = new Tabs(List.of(), null);

        public Tabs {
            pageIds = List.copyOf(pageIds);
        }
    }

    /**
     * Les pages dont le titre ou le texte contient chaque mot de la requête, sans casse ni accents : celles dont le titre
     * en contient un d'abord. Chacune vient avec un extrait autour du premier mot trouvé dans son texte (son début si seul
     * le titre le contient), où les mots trouvés sont marqués. Boîte noire : l'implémentation par défaut relit toutes les
     * pages à chaque appel ({@link PageSearch}) ; un adaptateur peut la remplacer par un index ou une base.
     */
    default List<SearchHit> search(String query) {
        return PageSearch.search(list(), query);
    }

    /** Une page trouvée et son extrait, découpé en morceaux. */
    record SearchHit(Page page, List<Fragment> excerpt) {}

    /** Un morceau d'extrait ; {@code match} : un mot de la requête. */
    record Fragment(String text, boolean match) {}

    Comparator<Page> BY_TITLE = Comparator.comparing(Page::title, String.CASE_INSENSITIVE_ORDER).thenComparing(Page::title);

    /**
     * Refuse de déplacer une racine de section, ou de mettre la page sous elle-même : l'appelant tient le verrou du
     * Carnet.
     */
    static void checkMove(List<Page> pages, Page page, String parentId) {
        if (page.isSectionRoot()) throw new IllegalArgumentException("Une page de section ne se déplace pas");
        pages.stream().filter(p -> p.id().equals(parentId)).findFirst().ifPresent(NotebookStore::checkParent);
        if (parentId != null && PageTree.isWithin(pages, parentId, page.id())) {
            throw new IllegalArgumentException("Une page ne peut pas aller sous elle-même ni sous l'une de ses sous-pages");
        }
    }

    /** Refuse de ranger une page sous une entrée de combat, qui n'a pas de sous-page. */
    static void checkParent(Page parent) {
        if (parent.isCombatEntry()) throw new IllegalArgumentException("Une entrée de combat n'a pas de sous-page");
    }

    /** Refuse d'écrire le contenu d'une entrée de combat, qui n'a pas de texte. */
    static void checkContent(Page page) {
        if (page.isCombatEntry()) throw new IllegalArgumentException("Une entrée de combat n'a pas de texte");
    }

    /** La racine vivante de la section des combats, s'il y en a une. */
    static Optional<Page> combatsRoot(List<Page> pages) {
        return pages.stream().filter(p -> COMBATS_SECTION.equals(p.section())).findFirst();
    }

    /** Refuse de supprimer une racine de section : son module la tient. */
    static void checkDelete(Page page) {
        if (page.isSectionRoot()) throw new IllegalArgumentException("Une page de section ne se supprime pas");
    }

    /**
     * Refuse des onglets en double, ou dont l'actif n'est pas l'un d'eux ; chaque page doit être connue de l'appelant, qui
     * tient le verrou du Carnet.
     */
    static void checkTabs(Tabs tabs, Predicate<String> known) {
        if (tabs.pageIds().stream().distinct().count() != tabs.pageIds().size()) {
            throw new IllegalArgumentException("Une page ne s'ouvre que dans un seul onglet");
        }
        boolean activeOk = tabs.pageIds().isEmpty()
                ? tabs.activeId() == null
                : tabs.activeId() != null && tabs.pageIds().contains(tabs.activeId());
        if (!activeOk) throw new IllegalArgumentException("L'onglet actif doit être l'un des onglets ouverts");
        for (String id : tabs.pageIds()) {
            if (!known.test(id)) throw new IllegalArgumentException("Page inconnue : " + id);
        }
    }

    /** Les suppressions tirées des pages à la corbeille, la plus récente d'abord. */
    static List<TrashEntry> trashEntries(Collection<Trashed> trashed) {
        Map<String, Long> lots = trashed.stream().collect(Collectors.groupingBy(Trashed::deletedWith, Collectors.counting()));
        return trashed.stream()
                .filter(t -> t.page().id().equals(t.deletedWith()))
                .map(t -> new TrashEntry(t.page(), t.deletedAt(), (int) (lots.get(t.deletedWith()) - 1)))
                .sorted(Comparator.comparing(TrashEntry::deletedAt).reversed().thenComparing(TrashEntry::page, BY_TITLE))
                .toList();
    }

    /** Garde le Carnet en mémoire, le temps du processus. */
    static NotebookStore inMemory() {
        Map<String, Page> pages = new ConcurrentHashMap<>();
        Map<String, Trashed> trashed = new ConcurrentHashMap<>();
        AtomicReference<Tabs> tabs = new AtomicReference<>(Tabs.NONE);
        return new NotebookStore() {
            @Override
            public List<Page> list() {
                return pages.values().stream().sorted(BY_TITLE).toList();
            }

            @Override
            public synchronized Page create(String title, String parentId) {
                if (parentId != null) checkParent(known(parentId));
                var page = new Page(UUID.randomUUID().toString(), title, parentId, null);
                pages.put(page.id(), page);
                return page;
            }

            @Override
            public synchronized Page createSectionRoot(String section, String title, String content) {
                var page = new Page(UUID.randomUUID().toString(), title, null, CampaignStore.checkModuleId(section), content);
                pages.put(page.id(), page);
                return page;
            }

            @Override
            public synchronized Page createCombatEntry(String combatId, String title) {
                Page root = combatsRoot(list()).orElseGet(() -> createSectionRoot(COMBATS_SECTION, "Combats", null));
                var page = new Page(UUID.randomUUID().toString(), title, root.id(), null, null, CombatStore.checkId(combatId));
                pages.put(page.id(), page);
                return page;
            }

            @Override
            public synchronized void saveContent(String id, String content) {
                Page page = known(id);
                checkContent(page);
                pages.put(id, page.withContent(content));
            }

            @Override
            public synchronized void rename(String id, String title) {
                Page page = known(id);
                pages.put(id, page.withTitle(title));
            }

            @Override
            public synchronized void move(String id, String parentId) {
                Page page = known(id);
                if (parentId != null) known(parentId);
                checkMove(list(), page, parentId);
                pages.put(id, page.withParent(parentId));
            }

            @Override
            public synchronized void delete(String id) {
                checkDelete(known(id));
                Instant now = Instant.now();
                for (String member : PageTree.branch(list(), id)) {
                    trashed.put(member, new Trashed(pages.remove(member), id, now));
                }
            }

            @Override
            public List<TrashEntry> trash() {
                return trashEntries(trashed.values());
            }

            @Override
            public synchronized void restore(String id) {
                Trashed root = trashed.get(id);
                if (root == null || !id.equals(root.deletedWith())) throw new IllegalArgumentException("Aucune suppression : " + id);
                String parent = root.page().parentId();
                String parentId = parent != null && pages.containsKey(parent) ? parent : null;
                for (Trashed member : List.copyOf(trashed.values())) {
                    if (!id.equals(member.deletedWith())) continue;
                    Page page = member.page();
                    trashed.remove(page.id());
                    pages.put(page.id(), page.id().equals(id) ? page.withParent(parentId) : page);
                }
            }

            @Override
            public synchronized void emptyTrash() {
                trashed.clear();
            }

            @Override
            public Tabs tabs() {
                return tabs.get();
            }

            @Override
            public synchronized void setTabs(Tabs next) {
                checkTabs(next, pages::containsKey);
                tabs.set(next);
            }

            private Page known(String id) {
                Page page = pages.get(id);
                if (page == null) throw new IllegalArgumentException("Page inconnue : " + id);
                return page;
            }
        };
    }
}
