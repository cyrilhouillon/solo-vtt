package solovtt.server;

import dnd.engine.catalog.SrdCatalog;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.util.List;

@Path("/api/catalog")
public class CatalogResource {

    @GET
    public List<Dtos.CatalogEntryDto> entries() {
        return SrdCatalog.entries().stream().map(Dtos.CatalogEntryDto::of).toList();
    }
}
