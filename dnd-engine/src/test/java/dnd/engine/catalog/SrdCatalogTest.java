package dnd.engine.catalog;

import dnd.engine.attack.AttackAction;
import dnd.engine.events.GameEvent;
import dnd.engine.rules.StandardRules;
import dnd.engine.state.Attack;
import dnd.engine.state.DamageType;
import dnd.engine.state.GameState;
import dnd.engine.state.Weapon;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;
import solovtt.engine.Engine;
import solovtt.engine.StepResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void chaqueCreaturePorteSonModificateurDInitiativeDuSrd() {
        assertEquals(2, SrdCatalog.create("guerriere", "g").initiative());
        assertEquals(2, SrdCatalog.create("magicienne", "m").initiative());
        assertEquals(2, SrdCatalog.create("gobelin", "go").initiative());
        assertEquals(3, SrdCatalog.create("squelette", "s").initiative());
        assertEquals(2, SrdCatalog.create("loup", "l").initiative());
    }

    @Test
    void leSqueletteSuitLeSrd() {
        var squelette = SrdCatalog.create("squelette", "s");
        assertEquals(14, squelette.armorClass());
        assertEquals(13, squelette.maxHitPoints());
        assertEquals(9, squelette.speed());
        assertTrue(squelette.isVulnerableTo(DamageType.BLUDGEONING));
    }

    @Test
    void leLoupCourtDouzeMetres() {
        var loup = SrdCatalog.create("loup", "l");
        assertEquals(12, loup.speed());
        assertEquals(12, loup.armorClass());
        assertEquals("1d6", loup.weapons().getFirst().diceNotation());
    }

    @Test
    void lesAttaquesSuiventLeSrd() {
        assertEquals(List.of("Dague", "Dague (lancer)"), labels("magicienne"));
        assertEquals(List.of("Cimeterre", "Arc court"), labels("gobelin"));
        assertEquals(List.of("Épée courte", "Arc court"), labels("squelette"));
        assertEquals(List.of("Épée longue"), labels("guerriere"));
        assertEquals(List.of("Morsure"), labels("loup"));

        var dague = SrdCatalog.create("magicienne", "m").weapons().getFirst();
        assertEquals(1.5, dague.reach());
        assertEquals(new Weapon.Range(6, 18), dague.range());
        var arc = SrdCatalog.create("squelette", "s").weapons().get(1);
        assertNull(arc.reach());
        assertEquals(new Weapon.Range(24, 96), arc.range());
        assertEquals(5, arc.attackBonus());
    }

    private static List<String> labels(String entryId) {
        return SrdCatalog.create(entryId, "x").attacks().stream().map(Attack::label).toList();
    }

    @Test
    void chaqueEntreePeutAttaquer() {
        for (var entry : SrdCatalog.entries()) {
            var state = new GameState()
                    .add(entry.create("attaquant"))
                    .add(SrdCatalog.create("squelette", "cible"));
            assertFalse(state.get("attaquant").attacks().isEmpty(), entry.id());
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
