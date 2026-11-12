package solovtt.server;

import dnd.engine.catalog.CatalogEntry;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;
import solovtt.scene.DistanceUnit;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le rejeu pas à pas. Carte de 30 × 20 m : Guerrière (1,5 ; 1,5), Gobelin (12 ; 1,5), Magicienne au contact du Gobelin
 * (12 ; 3). Initiative : Guerrière 20, Gobelin 15, Magicienne 2. Le d20 de 10 du Gobelin touche la Magicienne, qui ne
 * réagit pas ; 3 au d6 : 5 dégâts.
 */
class CombatReplayTest {

    private final CombatStore combats = CombatStore.inMemory();
    private final List<String> lances = new ArrayList<>();

    private static CatalogEntry entree(String id) {
        return TestCharacters.entry(id);
    }

    /** Un combat terminé : lancement, déplacement de la Guerrière, repositionnement libre de la Magicienne, fin de tour, attaque et réponse, fin. */
    private String combatTermine() {
        var session = new SceneSession(Dice.scripted(20, 15, 2, 10, 3), GameStore.none(), MapStore.inMemory(), combats, lances::add);
        session.create(30, 20, DistanceUnit.METRE, null);
        session.place(entree("guerriere"), 1.5, 1.5);
        session.place(entree("gobelin"), 12, 1.5);
        session.place(entree("magicienne"), 12, 3);
        session.startCombat();
        session.move("guerriere-1", 6, 1.5);
        session.move("magicienne-1", 13.5, 3);
        session.endTurn();
        var attaque = session.attack("gobelin-1", "melee:Cimeterre", "magicienne-1");
        session.answer(attaque.decision().orElseThrow().id(), "Ne rien faire");
        session.endCombat();
        return lances.getFirst();
    }

    @Test
    void unPasParAction_lePas0EtantLEtatAuLancement() {
        String id = combatTermine();

        var pas = CombatReplay.of(combats.game(id).load().orElseThrow(), id);

        assertEquals(6, pas.size());
        var depart = pas.getFirst();
        assertEquals(List.of(), depart.entries());
        assertNull(depart.combat());
        assertEquals(3, depart.scene().tokens().size());
        assertEquals(3, depart.sheets().size());

        assertEquals(1, pas.get(1).combat().round());
        assertEquals("guerriere-1", pas.get(1).combat().activeId());
        assertEquals(6, pas.get(2).scene().tokens().getFirst().x());
        assertEquals(12, pas.get(2).scene().tokens().get(2).x(), "le repositionnement libre attend le pas suivant");
        assertEquals(13.5, pas.get(3).scene().tokens().get(2).x());
        assertEquals("gobelin-1", pas.get(3).combat().activeId());

        var attaque = pas.get(4);
        assertEquals("magicienne-1", attaque.targetId());
        assertEquals(2, attaque.targetHitPoints());
        assertTrue(attaque.entries().stream().anyMatch(e -> e.text().equals("Magicienne subit 5 dégâts tranchants (PV 2/7)")));
        assertTrue(attaque.entries().stream().anyMatch(e -> e.dice() != null && e.dice().sides() == 20));

        var fin = pas.get(5);
        assertNull(fin.combat());
        assertNull(fin.targetId());
        assertEquals(2, fin.sheets().get(2).hitPoints());
    }

    @Test
    void unePartieSansCeCombat_estRefusee() {
        String id = combatTermine();

        assertThrows(IllegalArgumentException.class,
                () -> CombatReplay.of(combats.game(id).load().orElseThrow(), "7c9e6679-7425-40de-944b-e07fc1f90ae7"));
    }
}
