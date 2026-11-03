package solovtt.server;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Les images de fond des cartes : envoyées une fois, puis relues par leur empreinte. */
@Path("/api/images")
public class ImageResource {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private final ImageStore store;

    public ImageResource(ImageStore store) {
        this.store = store;
    }

    /** Garde une image, envoyée en octets bruts avec son type ; la même image envoyée deux fois garde la même empreinte. */
    @POST
    @Consumes({"image/png", "image/jpeg", "image/webp"})
    @ResponseStatus(201)
    @APIResponse(responseCode = "201", description = "L'empreinte de l'image gardée",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.ImageDto.class)))
    @APIResponse(responseCode = "400", description = "Les octets ne sont pas une image du type annoncé")
    @APIResponse(responseCode = "413", description = "Image plus lourde que la limite du serveur (quarkus.http.limits.max-body-size)")
    @APIResponse(responseCode = "415", description = "Type autre que PNG, JPEG ou WebP")
    public Dtos.ImageDto upload(@HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType, byte[] bytes) {
        String mediaType = contentType.split(";")[0].trim().toLowerCase();
        if (bytes == null || !matches(mediaType, bytes)) {
            throw new IllegalArgumentException("Image PNG, JPEG ou WebP attendue");
        }
        return new Dtos.ImageDto(store.put(bytes, mediaType));
    }

    @GET
    @Path("/{id}")
    @APIResponse(responseCode = "200", description = "L'image, avec son type ; elle ne change jamais sous la même empreinte")
    @APIResponse(responseCode = "404", description = "Image inconnue")
    public Response image(@PathParam("id") String id) {
        var image = store.find(id).orElseThrow(() -> new NotFoundException("Image inconnue : " + id));
        return Response.ok(image.bytes(), image.mediaType())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .build();
    }

    /** La signature des octets correspond au type annoncé. */
    private static boolean matches(String mediaType, byte[] bytes) {
        return switch (mediaType) {
            case "image/png" -> startsWith(bytes, PNG);
            case "image/jpeg" -> startsWith(bytes, JPEG);
            case "image/webp" -> bytes.length >= 12
                    && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                    && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
            default -> false;
        };
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        return bytes.length >= prefix.length && Arrays.equals(bytes, 0, prefix.length, prefix, 0, prefix.length);
    }
}
