package oracle.server;

import jakarta.enterprise.context.ApplicationScoped;
import solovtt.module.InstalledModule;

/** L'oracle générique se déclare au cœur, qui le rend activable par campagne. */
@ApplicationScoped
public class OracleModule implements InstalledModule {

    @Override
    public String id() {
        return OracleResource.MODULE_ID;
    }
}
