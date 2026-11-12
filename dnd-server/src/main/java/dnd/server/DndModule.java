package dnd.server;

import jakarta.enterprise.context.ApplicationScoped;
import solovtt.module.InstalledModule;

/** Le module D&D 2024 : les fiches des personnages joueurs, rangées dans la section « Personnages joueurs » du Carnet. */
@ApplicationScoped
public class DndModule implements InstalledModule {

    @Override
    public String id() {
        return SheetResource.MODULE_ID;
    }
}
