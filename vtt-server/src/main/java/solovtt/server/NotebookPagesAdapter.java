package solovtt.server;

import jakarta.enterprise.context.ApplicationScoped;
import solovtt.module.NotebookPages;

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
        CampaignStore.checkModuleId(moduleId);
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
        CampaignStore.checkModuleId(moduleId);
        return notebook(campaignId).find(pageId)
                .filter(page -> moduleId.equals(page.section()))
                .map(Page::content);
    }

    @Override
    public Optional<String> pageTitle(String campaignId, String pageId) {
        return notebook(campaignId).find(pageId).map(Page::title);
    }

    private NotebookStore notebook(String campaignId) {
        if (campaigns.find(campaignId).isEmpty()) throw new IllegalArgumentException("Campagne inconnue : " + campaignId);
        return campaigns.notebook(campaignId);
    }
}
