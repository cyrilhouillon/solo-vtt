package solovtt.server;

import jakarta.enterprise.context.ApplicationScoped;
import solovtt.module.NotebookPages;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Les sections des modules dans le Carnet d'une campagne : un module les reçoit par injection, sans connaître le stockage. */
@ApplicationScoped
public class NotebookPagesAdapter implements NotebookPages {

    private final CampaignStore campaigns;

    public NotebookPagesAdapter(CampaignStore campaigns) {
        this.campaigns = campaigns;
    }

    @Override
    public List<SectionPage> sectionRoots(String campaignId, String moduleId) {
        checkSection(moduleId);
        return notebook(campaignId).list().stream()
                .filter(page -> moduleId.equals(page.section()))
                .map(page -> new SectionPage(page.id(), page.title()))
                .toList();
    }

    @Override
    public SectionPage createSectionRoot(String campaignId, String moduleId, String title, String contentJson) {
        Page page = notebook(campaignId).createSectionRoot(moduleId, title, contentJson);
        return new SectionPage(page.id(), page.title());
    }

    @Override
    public Optional<String> sectionRootContent(String campaignId, String moduleId, String pageId) {
        checkSection(moduleId);
        return notebook(campaignId).find(pageId)
                .filter(page -> moduleId.equals(page.section()))
                .map(Page::content);
    }

    @Override
    public Optional<String> pageTitle(String campaignId, String pageId) {
        return notebook(campaignId).find(pageId).map(Page::title);
    }

    @Override
    public List<SectionPage> childPages(String campaignId, String moduleId, String pageId) {
        checkSection(moduleId);
        List<Page> pages = notebook(campaignId).list();
        if (!inSection(pages, moduleId, pageId)) return List.of();
        return pages.stream()
                .filter(page -> pageId.equals(page.parentId()))
                .sorted(Comparator.comparing(Page::title, String.CASE_INSENSITIVE_ORDER).thenComparing(Page::title))
                .map(page -> new SectionPage(page.id(), page.title()))
                .toList();
    }

    @Override
    public Optional<String> sectionPageContent(String campaignId, String moduleId, String pageId) {
        checkSection(moduleId);
        List<Page> pages = notebook(campaignId).list();
        if (!inSection(pages, moduleId, pageId)) return Optional.empty();
        return pages.stream().filter(page -> page.id().equals(pageId)).findFirst().map(Page::content);
    }

    @Override
    public SectionPage createEntry(String campaignId, String moduleId, String parentId, String title) {
        checkSection(moduleId);
        Page page = notebook(campaignId).createEntry(moduleId, parentId, title);
        return new SectionPage(page.id(), page.title());
    }

    @Override
    public boolean isEntry(String campaignId, String moduleId, String pageId) {
        checkSection(moduleId);
        return notebook(campaignId).find(pageId).filter(page -> moduleId.equals(page.entryOf())).isPresent();
    }

    private static boolean inSection(List<Page> pages, String moduleId, String pageId) {
        return NotebookStore.inSection(pages, moduleId, pageId);
    }

    private NotebookStore notebook(String campaignId) {
        if (campaigns.find(campaignId).isEmpty()) throw new IllegalArgumentException("Campagne inconnue : " + campaignId);
        return campaigns.notebook(campaignId);
    }

    /** La section d'un module : jamais celle des combats, que tient le cœur. */
    private static void checkSection(String moduleId) {
        if (NotebookStore.COMBATS_SECTION.equals(CampaignStore.checkModuleId(moduleId))) {
            throw new IllegalArgumentException("Section réservée par le cœur : " + moduleId);
        }
    }
}
