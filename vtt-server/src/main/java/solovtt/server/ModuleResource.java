package solovtt.server;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.util.List;

/** Les modules installés dans le serveur : le client ne propose que ceux qu'il a aussi. */
@Path("/api/modules")
public class ModuleResource {

    private final InstalledModules installed;

    public ModuleResource(InstalledModules installed) {
        this.installed = installed;
    }

    @GET
    @APIResponse(responseCode = "200", description = "Les ids des modules installés, triés")
    public List<String> installed() {
        return installed.ids();
    }
}
