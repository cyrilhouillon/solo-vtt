package solovtt.server;

import jakarta.enterprise.context.ApplicationScoped;
import solovtt.module.ModuleData;

import java.util.Optional;

/** Les données des modules, gardées par la campagne : un module les reçoit par injection, sans connaître le stockage. */
@ApplicationScoped
public class ModuleDataAdapter implements ModuleData {

    private final CampaignStore campaigns;

    public ModuleDataAdapter(CampaignStore campaigns) {
        this.campaigns = campaigns;
    }

    @Override
    public boolean exists(String campaignId) {
        return campaigns.find(campaignId).isPresent();
    }

    @Override
    public Optional<String> read(String campaignId, String moduleId) {
        return campaigns.readModuleData(campaignId, moduleId);
    }

    @Override
    public void write(String campaignId, String moduleId, String json) {
        campaigns.writeModuleData(campaignId, moduleId, json);
    }
}
