package mythic.server;

import jakarta.enterprise.context.ApplicationScoped;
import solovtt.module.InstalledModule;

/** Mythic se déclare au cœur, qui le rend activable par campagne. */
@ApplicationScoped
public class MythicModule implements InstalledModule {

    @Override
    public String id() {
        return MythicResource.MODULE_ID;
    }
}
