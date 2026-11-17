package solovtt.server.persistence;

import dnd.engine.state.CreatureProfile;
import dnd.engine.state.DamageType;
import dnd.engine.state.Weapon;
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
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFileGameStoreTest {

    @TempDir
    Path dir;

    private static final SavedGame PARTIE = new SavedGame(List.of(
            new SavedCommand.Create(30, 20, DistanceUnit.METRE, "a".repeat(64), "Crypte", List.of()),
            new SavedCommand.Place("gobelin", 12, 1.5, List.of()),
            new SavedCommand.Place("page-aria", "Aria", new CreatureProfile(18, 12, 5, 9, 2,
                    List.of(new Weapon("Javeline", 1, 6, DamageType.PIERCING, 5, 3, false).thrown(9, 36),
                            Weapon.ranged("Arc court", 1, 6, DamageType.PIERCING, 4, 2, 24, 96),
                            new Weapon("Javeline", 1, 8, DamageType.FIRE, 6, 0, false, 1.5, null, "autre:Javeline")),
                    Set.of("dueling"), Set.of("shield"), List.of(2)), 3, 4.5, List.of()),
            new SavedCommand.ShowGrid(false, List.of()),
            new SavedCommand.AlignGrid(33, 22, 0.25, 0.5, List.of()),
            new SavedCommand.Move("gobelin-1", 10.5, 3, List.of()),
            new SavedCommand.StartCombat("0f8fad5b-d9cb-469f-a165-70867728950e", List.of(14, 3)),
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
        var nouvelle = new SavedGame(List.of(new SavedCommand.Create(10, 10, DistanceUnit.FOOT, null, null, List.of())));
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

        assertEquals(Optional.of(new SavedGame(List.of(new SavedCommand.Create(30, 20, DistanceUnit.METRE, null, null, List.of())))),
                new JsonFileGameStore(file).load());
    }

    @Test
    void uneArmeGardeeAvantSonId_sIdentifieParSonNom() throws IOException {
        Path file = dir.resolve("partie.json");
        Files.writeString(file, """
                { "commands": [ { "type": "place", "catalogId": "page-aria", "name": "Aria", "x": 3, "y": 4.5, "rolls": [],
                  "profile": { "armorClass": 18, "hitPointMax": 12, "hitPoints": 5, "speed": 9, "initiative": 2,
                    "weapons": [ { "name": "Dague", "diceCount": 1, "dieSides": 4, "damageType": "PIERCING", "attackBonus": 5,
                      "damageBonus": 3, "oneHandedMelee": false, "reach": 1.5, "range": null } ],
                    "features": [], "spellsKnown": [], "spellSlots": [] } } ] }
                """);

        var place = (SavedCommand.Place) new JsonFileGameStore(file).load().orElseThrow().commands().getFirst();
        var dague = place.profile().weapons().getFirst();
        assertEquals(null, dague.id());
        assertEquals("Dague", dague.key());
    }

    @Test
    void unPersonnagePose_gardeSesSortsEtSesEmplacements() {
        var store = new JsonFileGameStore(dir.resolve("partie.json"));
        var profil = new dnd.engine.state.CreatureProfile(11, 7, 7, 9, 1, List.of(), java.util.Set.of(),
                java.util.Set.of("shield", "light"), List.of(1));
        var partie = new SavedGame(List.of(new SavedCommand.Place("page-elminster", "Elminster", profil, 3, 4.5, List.of())));

        store.save(partie);

        var place = (SavedCommand.Place) store.load().orElseThrow().commands().getFirst();
        assertEquals(java.util.Set.of("shield", "light"), place.profile().spellsKnown());
        assertEquals(List.of(1), place.profile().spellSlots());
    }
}
