package dnd.engine;

import dnd.engine.combat.EndTurn;
import dnd.engine.combat.Move;
import dnd.engine.combat.StartCombat;
import dnd.engine.events.GameEvent;
import dnd.engine.state.Condition;
import dnd.engine.state.Creature;
import dnd.engine.state.DamageType;
import dnd.engine.state.GameState;
import org.junit.jupiter.api.Test;
import solovtt.engine.Engine;
import solovtt.scene.DistanceUnit;

import java.util.List;

import static dnd.engine.CombatTest.engine;
import static dnd.engine.CombatTest.journal;
import static dnd.engine.CombatTest.table;
import static org.junit.jupiter.api.Assertions.*;

/**
 * La table de {@link CombatTest} : d20 d'initiative 10, 15, 4, donc Gobelin guerrier (17, vitesse 9 m) joue d'abord,
 * puis Guerrière (12), puis Squelette (7).
 */
class MoveTest {

    private static final double DELTA = 1e-9;

    private static Engine<GameState, GameEvent> combatLance(GameState state) {
        var e = engine(state, 10, 15, 4);
        e.submit(StartCombat.of(state));
        return e;
    }

    private static double reste(GameState state) {
        return Move.remaining(state).orElseThrow();
    }

    @Test
    void dansSaVitesse_laCreatureActiveSeDeplace_etLeJournalDitCeQuiReste() {
        var state = table();
        var e = combatLance(state);
        int before = e.events().size();

        e.submit(Move.of("gobelin", 6, DistanceUnit.METRE, state));

        assertEquals(List.of("Gobelin guerrier se déplace de 6 m (reste 3 m)"), journal(e).subList(before, e.events().size()));
        assertEquals(3, reste(state), DELTA);
    }

    @Test
    void leDeplacementSeFractionne_jusquAEpuiserLaVitesse() {
        var state = table();
        var e = combatLance(state);

        e.submit(Move.of("gobelin", 4.5, DistanceUnit.METRE, state));
        e.submit(Move.of("gobelin", 4.5, DistanceUnit.METRE, state));

        assertEquals(0, reste(state), DELTA);
    }

    @Test
    void auDelaDeLaVitesseRestante_refusAvecLaRaison_sansEvenement() {
        var state = table();
        var e = combatLance(state);
        e.submit(Move.of("gobelin", 6.1, DistanceUnit.METRE, state));
        int before = e.events().size();

        var refused = assertThrows(IllegalArgumentException.class, () -> Move.of("gobelin", 3, DistanceUnit.METRE, state));

        assertEquals("Gobelin guerrier ne peut plus parcourir que 2,9 m ce tour-ci (déplacement de 3 m)", refused.getMessage());
        assertEquals(before, e.events().size());
    }

    @Test
    void pileLaVitesseRestante_estPermis_malgreLArrondi() {
        var state = table();
        var e = combatLance(state);
        e.submit(Move.of("gobelin", 0.1 + 0.2, DistanceUnit.METRE, state));

        assertDoesNotThrow(() -> Move.of("gobelin", 8.7, DistanceUnit.METRE, state));
    }

    @Test
    void surUneCarteEnPieds_trenteFtValentNeufMetres_etLeRefusParleEnPieds() {
        var state = table();
        var e = combatLance(state);

        e.submit(Move.of("gobelin", 25, DistanceUnit.FOOT, state));

        assertEquals("Gobelin guerrier se déplace de 25 ft (reste 5 ft)", journal(e).getLast());
        var refused = assertThrows(IllegalArgumentException.class, () -> Move.of("gobelin", 10, DistanceUnit.FOOT, state));
        assertEquals("Gobelin guerrier ne peut plus parcourir que 5 ft ce tour-ci (déplacement de 10 ft)", refused.getMessage());
    }

    @Test
    void aZeroPv_laVitesseEstDeZero() {
        var state = table();
        var e = combatLance(state);
        e.emit(new GameEvent.DamageDealt("gobelin", state.get("gobelin").hitPoints(), DamageType.SLASHING));
        int before = e.events().size();

        var refused = assertThrows(IllegalArgumentException.class, () -> Move.of("gobelin", 1, DistanceUnit.METRE, state));

        assertEquals("Gobelin guerrier est à terre : à 0 PV, sa vitesse est de 0", refused.getMessage());
        assertEquals(0, reste(state), DELTA);
        assertEquals(before, e.events().size());
    }

    @Test
    void entravee_laVitesseEstDeZero() {
        var state = new GameState()
                .add(Creature.builder("guerriere", "Guerrière").initiative(2).condition(Condition.RESTRAINED).build());
        var e = engine(state, 10);
        e.submit(StartCombat.of(state));

        var refused = assertThrows(IllegalArgumentException.class, () -> Move.of("guerriere", 1, DistanceUnit.METRE, state));

        assertEquals("Guerrière a l'état « Entravé » : sa vitesse est de 0", refused.getMessage());
    }

    @Test
    void auTourSuivantDeLaCreature_saVitesseEstPleine() {
        var state = table();
        var e = combatLance(state);
        e.submit(Move.of("gobelin", 9, DistanceUnit.METRE, state));

        e.submit(EndTurn.of(state));
        e.submit(EndTurn.of(state));
        e.submit(EndTurn.of(state));

        assertEquals("gobelin", state.combat().orElseThrow().activeId().orElseThrow());
        assertEquals(9, reste(state), DELTA);
    }

    @Test
    void horsCombat_ouUneCreatureNonActive_lesReglesNeLimitentRien() {
        var state = table();
        assertFalse(Move.limits("gobelin", state));
        assertTrue(Move.remaining(state).isEmpty());
        assertThrows(IllegalStateException.class, () -> Move.of("gobelin", 1, DistanceUnit.METRE, state));

        var e = combatLance(state);
        int before = e.events().size();

        assertTrue(Move.limits("gobelin", state));
        assertFalse(Move.limits("guerriere", state));
        var refused = assertThrows(IllegalStateException.class, () -> Move.of("guerriere", 1, DistanceUnit.METRE, state));
        assertEquals("Ce n'est pas le tour de Guerrière", refused.getMessage());
        assertEquals(before, e.events().size());
    }
}
