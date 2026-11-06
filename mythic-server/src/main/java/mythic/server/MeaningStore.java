package mythic.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import mythic.engine.MeaningTables;
import solovtt.module.CommonModuleData;

import java.util.Optional;

/**
 * Les tables de sens du joueur, gardées dans les données communes de Mythic : importées une fois, elles servent à
 * toutes ses campagnes.
 */
@ApplicationScoped
public class MeaningStore {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Ce que Mythic garde pour le joueur. */
    record State(MeaningTables meaningTables) {}

    private final CommonModuleData data;

    public MeaningStore(CommonModuleData data) {
        this.data = data;
    }

    /** Les tables importées ; vide si aucune, ou si le document est abîmé (il sera remplacé au prochain import). */
    public Optional<MeaningTables> find() {
        return data.read(MythicResource.MODULE_ID).flatMap(json -> {
            try {
                return Optional.ofNullable(JSON.readValue(json, State.class).meaningTables());
            } catch (JsonProcessingException | IllegalArgumentException e) {
                return Optional.empty();
            }
        });
    }

    /** Remplace les tables importées. */
    public void save(MeaningTables tables) {
        try {
            data.write(MythicResource.MODULE_ID, JSON.writeValueAsString(new State(tables)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
