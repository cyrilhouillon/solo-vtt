package solovtt.server;

import io.quarkus.smallrye.openapi.OpenApiFilter;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Schema;

/**
 * Donne à chaque réponse d'erreur (4xx) de la spécification le corps que rend {@link ErrorMapper} : un
 * {@code ErrorDto}. Les ressources ne déclarent que le code et sa raison, sans recopier ce corps.
 */
@OpenApiFilter
public class ErrorResponsesFilter implements OASFilter {

    static final String ERROR_SCHEMA = "ErrorDto";

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        if (openAPI.getComponents() == null) openAPI.setComponents(OASFactory.createComponents());
        openAPI.getComponents().addSchema(ERROR_SCHEMA, OASFactory.createSchema()
                .addType(Schema.SchemaType.OBJECT)
                .description("Une erreur, avec son message lisible")
                .addProperty("message", OASFactory.createSchema().addType(Schema.SchemaType.STRING)));
        if (openAPI.getPaths() == null) return;
        openAPI.getPaths().getPathItems().values().stream()
                .flatMap(path -> path.getOperations().values().stream())
                .filter(operation -> operation.getResponses() != null)
                .flatMap(operation -> operation.getResponses().getAPIResponses().entrySet().stream())
                .filter(response -> response.getKey().startsWith("4") && response.getValue().getContent() == null)
                .forEach(response -> response.getValue().setContent(OASFactory.createContent()
                        .addMediaType("application/json", OASFactory.createMediaType()
                                .schema(OASFactory.createSchema().ref(ERROR_SCHEMA)))));
    }
}
