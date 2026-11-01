package dnd.engine.catalog;

import dnd.engine.attack.AttackAction;
import dnd.engine.events.GameEvent;
import dnd.engine.rules.StandardRules;
import dnd.engine.state.DamageType;
import dnd.engine.state.GameState;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;
import solovtt.engine.Engine;
import solovtt.engine.StepResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SrdCatalogTest {

    @Test
    void leCatalogueProposeDeuxPersonnagesEtTroisMonstres() {
        var kinds = SrdCatalog.entries().stream().map(CatalogEntry::kind).toList();
        assertEquals(2, kinds.stream().filter(k -> k == CatalogEntry.Kind.CHARACTER).count());
        assertEquals(3, kinds.stream().filter(k -> k == CatalogEntry.Kind.MONSTER).count());
    }

    @Test
    void deuxInstancesDUneMemeEntreeSontDesCreaturesDistinctes() {
        var a = SrdCatalog.create("gobelin", "gobelin-1");
        var b = SrdCatalog.create("gobelin", "gobelin-2");
        assertNotSame(a, b);
        assertEquals("gobelin-1", a.id());
        assertEquals("gobelin-2", b.id());
    }

    @Test
    void leSqueletteSuitLeSrd() {
        var squelette = SrdCatalog.create("squelette", "s");
        assertEquals(13, squelette.armorClass());
        assertEquals(13, squelette.maxHitPoints());
        assertEquals(9, squelette.speed());
        assertTrue(squelette.isVulnerableTo(DamageType.BLUDGEONING));
    }

    @Test
    void leLoupCourtDouzeMetres() {
        assertEquals(12, SrdCatalog.create("loup", "l").speed());
    }

    @Test
    void chaqueEntreePeutAttaquer() {
        for (var entry : SrdCatalog.entries()) {
            var state = new GameState()
                    .add(entry.create("attaquant"))
                    .add(SrdCatalog.create("squelette", "cible"));
            assertNotNull(state.get("attaquant").weapon(), entry.id());
            var engine = new Engine<GameState, GameEvent>(state, StandardRules.create(), Dice.scripted(15, 1, 1, 1, 1));

            var result = engine.submit(AttackAction.of("attaquant", "cible", state));

            assertInstanceOf(StepResult.Idle.class, result, entry.id());
            assertTrue(engine.events().stream().anyMatch(GameEvent.AttackResolved.class::isInstance), entry.id());
        }
    }

    @Test
    void uneEntreeInconnueEstRefusee() {
        assertThrows(IllegalArgumentException.class, () -> SrdCatalog.create("dragon", "d"));
    }
}
