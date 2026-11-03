package solovtt.server.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import solovtt.scene.DistanceUnit;
import solovtt.server.SavedGame;
import solovtt.server.SavedGame.SavedCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFileGameStoreTest {

    @TempDir
    Path dir;

    private static final SavedGame PARTIE = new SavedGame(List.of(
            new SavedCommand.Create(30, 20, DistanceUnit.METRE, "a".repeat(64), List.of()),
            new SavedCommand.Place("gobelin", 12, 1.5, List.of()),
            new SavedCommand.ShowGrid(false, List.of()),
            new SavedCommand.Move("gobelin-1", 10.5, 3, List.of()),
            new SavedCommand.StartCombat(List.of(14, 3)),
            new SavedCommand.EndTurn(List.of()),
            new SavedCommand.Attack("gobelin-1", "melee:Cimeterre", "magicienne-1", List.of(10)),
            new SavedCommand.Answer("d1", "Bouclier", List.of(4)),
            new SavedCommand.EndCombat(List.of()),
            new SavedCommand.Remove("gobelin-1", List.of())));

    @Test
    void unePartieSauveePuisChargee_estLaMeme() {
        var store = new JsonFileGameStore(dir.resolve("sous-dossier/partie.json"));
        store.save(PARTIE);
        assertEquals(Optional.of(PARTIE), store.load());
    }

    @Test
    void sansFichier_aucunePartie() {
        assertEquals(Optional.empty(), new JsonFileGameStore(dir.resolve("partie.json")).load());
    }

    @Test
    void sauvegarderRemplaceLaPartie_sansFichierTemporaireRestant() throws IOException {
        var store = new JsonFileGameStore(dir.resolve("partie.json"));
        store.save(PARTIE);
        var nouvelle = new SavedGame(List.of(new SavedCommand.Create(10, 10, DistanceUnit.FOOT, null, List.of())));
        store.save(nouvelle);
        assertEquals(Optional.of(nouvelle), store.load());
        try (Stream<Path> files = Files.list(dir)) {
            assertEquals(List.of(dir.resolve("partie.json")), files.toList());
        }
    }

    @Test
    void unFichierIllisible_leveUneErreur() throws IOException {
        Path file = dir.resolve("partie.json");
        Files.writeString(file, "{ pas du json");
        assertThrows(RuntimeException.class, () -> new JsonFileGameStore(file).load());
    }

    @Test
    void mettreDeCote_renommeEnIllisible_etPlusRienACharger() throws IOException {
        Path file = dir.resolve("partie.json");
        var store = new JsonFileGameStore(file);
        store.save(PARTIE);
        store.setAside();
        assertFalse(Files.exists(file));
        assertEquals(Optional.empty(), store.load());
        try (Stream<Path> files = Files.list(dir)) {
            var names = files.map(p -> p.getFileName().toString()).toList();
            assertEquals(1, names.size());
            assertTrue(names.getFirst().matches("partie\\.json\\.\\d{8}-\\d{6}\\.illisible"), names.getFirst());
        }
    }

    @Test
    void unePartieGardeeAvantLesImages_seLitSansImage() throws IOException {
        Path file = dir.resolve("partie.json");
        Files.writeString(file, """
                { "commands": [ { "type": "create", "width": 30, "height": 20, "unit": "METRE", "rolls": [] } ] }
                """);

        assertEquals(Optional.of(new SavedGame(List.of(new SavedCommand.Create(30, 20, DistanceUnit.METRE, null, List.of())))),
                new JsonFileGameStore(file).load());
    }
}
