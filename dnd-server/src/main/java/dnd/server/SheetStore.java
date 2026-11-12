package dnd.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dnd.engine.sheet.SheetEvent;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import solovtt.module.ModuleData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ce que le module D&D garde dans une campagne, par {@link ModuleData} ({@code modules/dnd.json}) : l'id de la page
 * « Personnages joueurs », et l'histoire de chaque fiche, sous l'id de sa page. Un document abîmé n'est jamais écrasé :
 * il est refusé (409) jusqu'à ce que le joueur le répare.
 */
final class SheetStore {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Le document du module dans une campagne. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record State(String playersPageId, Map<String, List<EventFile>> sheets) {
        static final State EMPTY = new State(null, Map.of());

        State {
            sheets = sheets == null ? Map.of() : sheets;
        }

        State withPlayersPage(String id) {
            return new State(id, sheets);
        }

        State withEvent(String pageId, SheetEvent event) {
            Map<String, List<EventFile>> next = new LinkedHashMap<>(sheets);
            List<EventFile> history = new ArrayList<>(next.getOrDefault(pageId, List.of()));
            // Une histoire commence par sa création, même si l'écriture de celle-ci a échoué.
            if (history.isEmpty() && !(event instanceof SheetEvent.SheetCreated)) {
                history.add(EventFile.of(new SheetEvent.SheetCreated()));
            }
            history.add(EventFile.of(event));
            next.put(pageId, history);
            return new State(playersPageId, next);
        }

        /** L'histoire de la fiche de cette page, vide si elle n'en a pas. */
        Optional<List<SheetEvent>> history(String pageId) {
            List<EventFile> events = sheets.get(pageId);
            if (events == null || events.isEmpty()) return Optional.empty();
            return Optional.of(events.stream().map(EventFile::event).toList());
        }
    }

    /** Un événement tel qu'il est écrit : {@code created}, ou {@code edited} avec les valeurs saisies. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record EventFile(String type, DndDtos.SheetValuesDto values) {
        static final String CREATED = "created";
        static final String EDITED = "edited";

        static EventFile of(SheetEvent event) {
            return switch (event) {
                case SheetEvent.SheetCreated created -> new EventFile(CREATED, null);
                case SheetEvent.SheetEdited edited -> new EventFile(EDITED, DndDtos.SheetValuesDto.of(edited.values()));
            };
        }

        /**
         * L'événement écrit.
         *
         * @throws IllegalStateException si son type est inconnu, ou si une saisie n'a pas de valeurs
         * @throws IllegalArgumentException si une valeur saisie est hors bornes
         */
        SheetEvent event() {
            return switch (type) {
                case CREATED -> new SheetEvent.SheetCreated();
                case EDITED -> {
                    if (values == null) throw new IllegalStateException("Saisie de fiche sans valeurs");
                    yield new SheetEvent.SheetEdited(values.toSheet());
                }
                case null, default -> throw new IllegalStateException("Événement de fiche inconnu : " + type);
            };
        }
    }

    private final ModuleData data;
    private final String moduleId;

    SheetStore(ModuleData data, String moduleId) {
        this.data = data;
        this.moduleId = moduleId;
    }

    /**
     * Le document du module dans la campagne, vide s'il n'a encore rien écrit.
     *
     * @throws ClientErrorException (409) si le document ne se lit pas
     */
    State read(String campaignId) {
        return data.read(campaignId, moduleId).map(json -> {
            try {
                State read = JSON.readValue(json, State.class);
                return read == null ? State.EMPTY : read;
            } catch (JsonProcessingException e) {
                throw unreadable();
            }
        }).orElse(State.EMPTY);
    }

    void write(String campaignId, State state) {
        try {
            data.write(campaignId, moduleId, JSON.writeValueAsString(state));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    static ClientErrorException unreadable() {
        return new ClientErrorException("Fiches de personnage illisibles : modules/dnd.json est abîmé", Response.Status.CONFLICT);
    }
}
