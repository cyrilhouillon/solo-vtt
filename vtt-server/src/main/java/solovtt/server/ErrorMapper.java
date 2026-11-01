package solovtt.server;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Toute erreur rend un code HTTP et un message lisible, en JSON. */
public class ErrorMapper {

    /** Les ids inconnus sont filtrés en amont (404) : il ne reste que les saisies invalides. */
    @ServerExceptionMapper
    public RestResponse<Dtos.ErrorDto> invalid(IllegalArgumentException e) {
        return RestResponse.status(Response.Status.BAD_REQUEST, new Dtos.ErrorDto(e.getMessage()));
    }

    /** Une action tentée pendant qu'une décision attend sa réponse. */
    @ServerExceptionMapper
    public RestResponse<Dtos.ErrorDto> decisionPending(DecisionPendingException e) {
        return RestResponse.status(Response.Status.CONFLICT, new Dtos.ErrorDto(e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<Dtos.ErrorDto> http(WebApplicationException e) {
        return RestResponse.status(Response.Status.fromStatusCode(e.getResponse().getStatus()), new Dtos.ErrorDto(e.getMessage()));
    }
}
