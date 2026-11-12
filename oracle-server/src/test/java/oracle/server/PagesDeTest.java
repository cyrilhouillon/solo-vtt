package oracle.server;

import solovtt.module.NotebookPages;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Les Carnets des campagnes, en mémoire : chaque page vivante, son parent, sa section si elle en est une racine. */
final class PagesDeTest implements NotebookPages {

    record Page(String campaignId, String id, String title, String parentId, String section, String content) {}

    final List<Page> pages = new ArrayList<>();

    @Override
    public List<SectionPage> sectionRoots(String campaignId, String moduleId) {
        return pages.stream()
                .filter(p -> p.campaignId().equals(campaignId) && moduleId.equals(p.section()))
                .map(p -> new SectionPage(p.id(), p.title()))
                .toList();
    }

    @Override
    public SectionPage createSectionRoot(String campaignId, String moduleId, String title, String contentJson) {
        var page = new Page(campaignId, UUID.randomUUID().toString(), title, null, moduleId, contentJson);
        pages.add(page);
        return new SectionPage(page.id(), page.title());
    }

    @Override
    public Optional<String> sectionRootContent(String campaignId, String moduleId, String pageId) {
        return find(campaignId, pageId).filter(p -> moduleId.equals(p.section())).map(Page::content);
    }

    @Override
    public Optional<String> pageTitle(String campaignId, String pageId) {
        return find(campaignId, pageId).map(Page::title);
    }

    @Override
    public List<SectionPage> childPages(String campaignId, String moduleId, String pageId) {
        if (!inSection(campaignId, moduleId, pageId)) return List.of();
        return pages.stream()
                .filter(p -> p.campaignId().equals(campaignId) && pageId.equals(p.parentId()))
                .sorted(Comparator.comparing(Page::title, String.CASE_INSENSITIVE_ORDER))
                .map(p -> new SectionPage(p.id(), p.title()))
                .toList();
    }

    @Override
    public Optional<String> sectionPageContent(String campaignId, String moduleId, String pageId) {
        if (!inSection(campaignId, moduleId, pageId)) return Optional.empty();
        return find(campaignId, pageId).map(Page::content);
    }

    /** Une page ordinaire, ou une sous-page, que le joueur crée. */
    String creer(String campaignId, String title, String parentId, String content) {
        var page = new Page(campaignId, UUID.randomUUID().toString(), title, parentId, null, content);
        pages.add(page);
        return page.id();
    }

    /** Met la page à la corbeille : elle n'est plus vivante. */
    void supprimer(String id) {
        pages.removeIf(p -> p.id().equals(id));
    }

    /** Renomme une page, comme le joueur depuis l'arbre. */
    void renommer(String id, String title) {
        pages.replaceAll(p -> p.id().equals(id) ? new Page(p.campaignId(), id, title, p.parentId(), p.section(), p.content()) : p);
    }

    private Optional<Page> find(String campaignId, String pageId) {
        return pages.stream().filter(p -> p.campaignId().equals(campaignId) && p.id().equals(pageId)).findFirst();
    }

    private boolean inSection(String campaignId, String moduleId, String pageId) {
        Optional<Page> current = find(campaignId, pageId);
        for (int steps = 0; current.isPresent() && steps <= pages.size(); steps++) {
            Page page = current.get();
            if (page.parentId() == null) return moduleId.equals(page.section());
            current = find(campaignId, page.parentId());
        }
        return false;
    }

    @Override
    public SectionPage createEntry(String campaignId, String moduleId, String parentId, String title) {
        throw new UnsupportedOperationException("Ce module ne crée pas d'entrée");
    }

    @Override
    public boolean isEntry(String campaignId, String moduleId, String pageId) {
        return false;
    }
}
