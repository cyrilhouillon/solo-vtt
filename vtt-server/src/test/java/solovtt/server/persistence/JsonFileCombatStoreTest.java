package solovtt.server.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import solovtt.scene.DistanceUnit;
import solovtt.server.SavedGame;
import solovtt.server.SavedGame.SavedCommand;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFileCombatStoreTest {

    private static final String ID = "0f8fad5b-d9cb-469f-a165-70867728950e";

    @TempDir
    Path dir;

    @Test
    void unCombatEcrit_seRelitDansSonFichier_etSeListe() {
        var combats = new JsonFileCombatStore(dir.resolve("combats"));
        var combat = new SavedGame(List.of(
                new SavedCommand.Create(30, 20, DistanceUnit.METRE, null, null, List.of()),
                new SavedCommand.StartCombat(ID, List.of())));

        assertEquals(List.of(), combats.ids());
        assertEquals(Optional.empty(), combats.game(ID).load());
        combats.game(ID).save(combat);

        assertTrue(Files.exists(dir.resolve("combats").resolve(ID + ".json")));
        assertEquals(List.of(ID), combats.ids());
        assertEquals(combat, new JsonFileCombatStore(dir.resolve("combats")).game(ID).load().orElseThrow());
    }

    @Test
    void unIdQuiNEstPasUnUuid_neConstruitAucunChemin() {
        var combats = new JsonFileCombatStore(dir);

        assertThrows(IllegalArgumentException.class, () -> combats.game("../partie"));
    }

    @Test
    void unLancementDAvant_sansIdDeCombat_seRelitSansId() throws Exception {
        Path file = dir.resolve("partie.json");
        Files.writeString(file, """
                {"commands":[{"type":"startCombat","rolls":[14,3]}]}""");

        var lu = new JsonFileGameStore(file).load().orElseThrow();

        assertEquals(new SavedCommand.StartCombat(null, List.of(14, 3)), lu.commands().getFirst());
    }
}
