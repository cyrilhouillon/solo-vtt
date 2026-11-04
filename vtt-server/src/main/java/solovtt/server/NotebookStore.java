package solovtt.server;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Où vit le Carnet d'une campagne : ses pages, rangées en arbre par leur parent, et la dernière page consultée. Un
 * déplacement ne touche que la page déplacée : ses sous-pages la suivent.
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
     * @throws IllegalArgumentException si aucune page n'a l'id du parent
     */
    Page create(String title, String parentId);

    /**
     * Remplace le contenu de la page.
     *
     * @throws IllegalArgumentException si aucune page n'a cet id
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
     * @throws IllegalArgumentException si la page ou le parent est inconnu, ou si le parent est la page elle-même ou
     *     l'une de ses sous-pages
     */
    void move(String id, String parentId);

    /** L'id de la dernière page consultée, vide si aucune. */
    Optional<String> lastPage();

    /**
     * Note la dernière page consultée.
     *
     * @throws IllegalArgumentException si aucune page n'a cet id
     */
    void setLastPage(String id);

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

    /** Refuse un parent qui mettrait la page sous elle-même : l'appelant tient le verrou du Carnet. */
    static void checkMove(List<Page> pages, String id, String parentId) {
        if (parentId != null && PageTree.isWithin(pages, parentId, id)) {
            throw new IllegalArgumentException("Une page ne peut pas aller sous elle-même ni sous l'une de ses sous-pages");
        }
    }

    /** Garde le Carnet en mémoire, le temps du processus. */
    static NotebookStore inMemory() {
        Map<String, Page> pages = new ConcurrentHashMap<>();
        AtomicReference<String> lastPage = new AtomicReference<>();
        return new NotebookStore() {
            @Override
            public List<Page> list() {
                return pages.values().stream().sorted(BY_TITLE).toList();
            }

            @Override
            public synchronized Page create(String title, String parentId) {
                if (parentId != null) known(parentId);
                var page = new Page(UUID.randomUUID().toString(), title, parentId, null);
                pages.put(page.id(), page);
                return page;
            }

            @Override
            public synchronized void saveContent(String id, String content) {
                Page page = known(id);
                pages.put(id, new Page(id, page.title(), page.parentId(), content));
            }

            @Override
            public synchronized void rename(String id, String title) {
                Page page = known(id);
                pages.put(id, new Page(id, title, page.parentId(), page.content()));
            }

            @Override
            public synchronized void move(String id, String parentId) {
                Page page = known(id);
                if (parentId != null) known(parentId);
                checkMove(list(), id, parentId);
                pages.put(id, new Page(id, page.title(), parentId, page.content()));
            }

            @Override
            public Optional<String> lastPage() {
                return Optional.ofNullable(lastPage.get());
            }

            @Override
            public void setLastPage(String id) {
                lastPage.set(known(id).id());
            }

            private Page known(String id) {
                Page page = pages.get(id);
                if (page == null) throw new IllegalArgumentException("Page inconnue : " + id);
                return page;
            }
        };
    }
}
