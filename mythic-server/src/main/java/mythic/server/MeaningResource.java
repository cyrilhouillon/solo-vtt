package mythic.server;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import mythic.engine.Meaning;
import mythic.engine.MeaningTables;
import mythic.engine.MeaningTablesText;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import solovtt.engine.Dice;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Les tables de sens du joueur, communes à ses campagnes : les importer depuis son fichier, les lister, y tirer deux
 * mots. Les tables ne sont jamais renvoyées en entier.
 */
@Path("/api/mythic")
public class MeaningResource {

    static final String NOT_IMPORTED = "Tables de sens non importées : importer le fichier de vos tables.";

    private final MeaningStore store;
    private final Dice dice;

    public MeaningResource(MeaningStore store, Dice dice) {
        this.store = store;
        this.dice = dice;
    }

    @GET
    @Path("/meaning-tables")
    @APIResponse(responseCode = "200", description = "Les tables importées : Actions, Descriptions, puis les éléments dans l'ordre du fichier",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MythicDtos.MeaningTablesDto.class)))
    public MythicDtos.MeaningTablesDto tables() {
        return summary(store.find());
    }

    /** Remplace les tables par celles du fichier ; un fichier mal formé ne change rien. */
    @PUT
    @Path("/meaning-tables")
    @Consumes(MediaType.TEXT_PLAIN)
    @APIResponse(responseCode = "200", description = "Les tables importées",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MythicDtos.MeaningTablesDto.class)))
    @APIResponse(responseCode = "400", description = "Fichier mal formé : la raison et la ligne")
    public synchronized MythicDtos.MeaningTablesDto importTables(String text) {
        MeaningTables tables = MeaningTablesText.parse(text);
        store.save(tables);
        return summary(Optional.of(tables));
    }

    @POST
    @Path("/meanings")
    @APIResponse(responseCode = "200", description = "Deux mots tirés dans la table, avec leurs d100",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MythicDtos.MeaningDto.class)))
    @APIResponse(responseCode = "400", description = "Table absente ou inconnue, ou table d'éléments inconnue")
    @APIResponse(responseCode = "409", description = "Aucune table de sens importée")
    public MythicDtos.MeaningDto draw(MythicDtos.MeaningRequest request) {
        if (request == null || request.table() == null) throw new IllegalArgumentException("Table absente");
        MeaningTables tables = store.find()
                .orElseThrow(() -> new WebApplicationException(NOT_IMPORTED, Response.Status.CONFLICT));
        Meaning meaning = switch (request.table()) {
            case MythicDtos.ACTIONS -> tables.actions(dice);
            case MythicDtos.DESCRIPTIONS -> tables.descriptions(dice);
            case MythicDtos.ELEMENT -> {
                if (request.element() == null) throw new IllegalArgumentException("Table d'éléments absente");
                yield tables.element(request.element(), dice);
            }
            default -> throw new IllegalArgumentException("Table inconnue : " + request.table());
        };
        return MythicDtos.MeaningDto.of(meaning);
    }

    private static MythicDtos.MeaningTablesDto summary(Optional<MeaningTables> found) {
        if (found.isEmpty()) return new MythicDtos.MeaningTablesDto(false, List.of());
        List<MythicDtos.MeaningTableDto> tables = new ArrayList<>(List.of(
                new MythicDtos.MeaningTableDto(MythicDtos.ACTIONS, null, MeaningTables.ACTIONS),
                new MythicDtos.MeaningTableDto(MythicDtos.DESCRIPTIONS, null, MeaningTables.DESCRIPTIONS)));
        found.get().elementNames().forEach(name -> tables.add(new MythicDtos.MeaningTableDto(MythicDtos.ELEMENT, name, name)));
        return new MythicDtos.MeaningTablesDto(true, tables);
    }
}
