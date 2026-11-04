package solovtt.server;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/** Où vit le Carnet d'une campagne : ses pages et la dernière page consultée. */
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

    /** Crée une page vide sous ce titre ; deux pages peuvent porter le même titre. */
    Page create(String title);

    /**
     * Remplace le contenu de la page.
     *
     * @throws IllegalArgumentException si aucune page n'a cet id
     */
    void saveContent(String id, String content);

    /** L'id de la dernière page consultée, vide si aucune. */
    Optional<String> lastPage();

    /**
     * Note la dernière page consultée.
     *
     * @throws IllegalArgumentException si aucune page n'a cet id
     */
    void setLastPage(String id);

    Comparator<Page> BY_TITLE = Comparator.comparing(Page::title, String.CASE_INSENSITIVE_ORDER).thenComparing(Page::title);

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
            public Page create(String title) {
                var page = new Page(UUID.randomUUID().toString(), title, null);
                pages.put(page.id(), page);
                return page;
            }

            @Override
            public void saveContent(String id, String content) {
                Page page = known(id);
                pages.put(id, new Page(id, page.title(), content));
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
