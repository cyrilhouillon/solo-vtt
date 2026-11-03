package solovtt.server;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@QuarkusTest
class ImageApiTest {

    /** Le début d'une PNG : la signature suffit au serveur, qui ne décode pas l'image. */
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};
    static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII);

    static String envoyer(byte[] bytes, String type) {
        return given().contentType(type).body(bytes).post("/api/images").then()
                .statusCode(201).extract().path("id");
    }

    @Test
    void unePngEnvoyeeSeRelitAvecSonType_sousSonEmpreinte() {
        String id = envoyer(PNG, "image/png");
        org.hamcrest.MatcherAssert.assertThat(id, matchesPattern("[0-9a-f]{64}"));
        byte[] relue = given().get("/api/images/" + id).then().statusCode(200)
                .header("Content-Type", equalTo("image/png"))
                .header("Cache-Control", equalTo("public, max-age=31536000, immutable"))
                .extract().asByteArray();
        assertArrayEquals(PNG, relue);
    }

    @Test
    void laMemeImageEnvoyeeDeuxFois_gardeLaMemeEmpreinte() {
        org.junit.jupiter.api.Assertions.assertEquals(envoyer(PNG, "image/png"), envoyer(PNG, "image/png"));
    }

    @Test
    void jpegEtWebp_seRelisentAvecLeurType() {
        given().get("/api/images/" + envoyer(JPEG, "image/jpeg")).then().statusCode(200).header("Content-Type", equalTo("image/jpeg"));
        given().get("/api/images/" + envoyer(WEBP, "image/webp")).then().statusCode(200).header("Content-Type", equalTo("image/webp"));
    }

    @Test
    void unAutreType_estRefuse415() {
        given().contentType("image/gif").body("GIF89a".getBytes(StandardCharsets.US_ASCII)).post("/api/images").then().statusCode(415);
        given().contentType("text/plain").body("bonjour").post("/api/images").then().statusCode(415);
    }

    @Test
    void desOctetsQuiNeSontPasLImageAnnoncee_sontRefuses400() {
        given().contentType("image/png").body(JPEG).post("/api/images").then().statusCode(400)
                .body("message", equalTo("Image PNG, JPEG ou WebP attendue"));
        given().contentType("image/webp").body(Arrays.copyOf(PNG, 4)).post("/api/images").then().statusCode(400);
        given().contentType("image/png").body(new byte[0]).post("/api/images").then().statusCode(400);
    }

    @Test
    void uneImageInconnue_rend404() {
        given().get("/api/images/" + "0".repeat(64)).then().statusCode(404);
        given().get("/api/images/inconnue").then().statusCode(404);
    }
}
