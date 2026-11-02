package solovtt.server.persistence;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import solovtt.server.GameStore;
import solovtt.server.SavedGame;
import solovtt.server.SavedGame.SavedCommand;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * La partie dans un fichier JSON. L'écriture passe par un fichier voisin puis un déplacement atomique :
 * le fichier n'est jamais à moitié écrit. Une partie mise de côté devient {@code <nom>.<horodatage>.illisible}.
 */
public final class JsonFileGameStore implements GameStore {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .addMixIn(SavedCommand.class, SavedCommandFormat.class);

    public JsonFileGameStore(Path file) {
        this.file = file.toAbsolutePath();
    }

    public Path file() {
        return file;
    }

    @Override
    public Optional<SavedGame> load() {
        if (!Files.exists(file)) return Optional.empty();
        try {
            return Optional.of(mapper.readValue(file.toFile(), SavedGame.class));
        } catch (IOException e) {
            throw new UncheckedIOException("Partie illisible : " + file, e);
        }
    }

    @Override
    public void save(SavedGame game) {
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            mapper.writeValue(temporary.toFile(), game);
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de sauvegarder la partie : " + file, e);
        }
    }

    @Override
    public void setAside() {
        if (!Files.exists(file)) return;
        Path aside = file.resolveSibling(file.getFileName() + "." + LocalDateTime.now().format(STAMP) + ".illisible");
        try {
            Files.move(file, aside);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de mettre la partie de côté : " + file, e);
        }
    }

    /** Le format JSON des commandes, hors de {@link SavedGame} : un champ {@code type} nomme la commande. */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = SavedCommand.Create.class, name = "create"),
            @JsonSubTypes.Type(value = SavedCommand.Place.class, name = "place"),
            @JsonSubTypes.Type(value = SavedCommand.Remove.class, name = "remove"),
            @JsonSubTypes.Type(value = SavedCommand.Move.class, name = "move"),
            @JsonSubTypes.Type(value = SavedCommand.Attack.class, name = "attack"),
            @JsonSubTypes.Type(value = SavedCommand.StartCombat.class, name = "startCombat"),
            @JsonSubTypes.Type(value = SavedCommand.EndTurn.class, name = "endTurn"),
            @JsonSubTypes.Type(value = SavedCommand.EndCombat.class, name = "endCombat"),
            @JsonSubTypes.Type(value = SavedCommand.Answer.class, name = "answer"),
    })
    private interface SavedCommandFormat {}
}
