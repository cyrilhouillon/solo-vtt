package solovtt.server;

import dnd.engine.rules.ShieldReaction;
import dnd.engine.state.Combat;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;
import solovtt.scene.DistanceUnit;
import solovtt.scene.Token;
import solovtt.server.SceneSession.JournalEntry;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La preuve de l'epic « jouer un combat D&D 2024 en local » : deux personnages contre trois adversaires, joués de bout
 * en bout par le seul joueur ; chaque action du moteur entre au journal, et la partie reprend à l'identique.
 *
 * <p>Carte de 30 × 20 m. Guerrière (1,5 ; 3), Magicienne (3 ; 6), Gobelin (4,5 ; 3), Squelette (9 ; 6), Loup (4,5 ; 6).
 * Initiative, dans l'ordre de pose : Guerrière 20 (22), Magicienne 12 (14), Gobelin 15 (17), Squelette 8 (11), Loup 3 (5).
 * Ordre de jeu : Guerrière, Gobelin, Magicienne, Squelette, Loup.</p>
 */
class CombatDeBoutEnBoutTest {

    private final InMemoryGameStore store = new InMemoryGameStore();

    @Test
    void deuxPersonnagesContreTroisAdversaires_jouesDeBoutEnBout_puisRepris() {
        var session = new SceneSession(Dice.scripted(
                20, 12, 15, 8, 3,   // initiative
                15, 7,              // Guerrière → Gobelin : 15 + 5 = 20 contre CA 15, 1d8 = 7
                2,                  // Magicienne → Loup : 2 + 4 = 6 contre CA 12
                10,                 // Squelette → Magicienne : 10 + 5 = 15 contre CA 12, Bouclier la fait rater
                18, 4), store);     // Loup → Magicienne : 18 + 4 = 22 contre CA 17, 1d6 = 4

        // Préparation : pose et déplacement libre restent hors journal.
        sansEntree(session, () -> session.create(30, 20, DistanceUnit.METRE));
        sansEntree(session, () -> session.place(entree("guerriere"), 1.5, 3));
        sansEntree(session, () -> session.place(entree("magicienne"), 3, 6));
        sansEntree(session, () -> session.place(entree("gobelin"), 4.5, 3));
        sansEntree(session, () -> session.place(entree("squelette"), 12, 6));
        sansEntree(session, () -> session.place(entree("loup"), 4.5, 6));
        sansEntree(session, () -> session.move("squelette-1", 9, 6));

        // Round 1
        entre(session, session::startCombat, "Le combat commence", "Ordre de jeu", "Round 1", "Tour de Guerrière");
        sansEntree(session, () -> session.move("loup-1", 4.5, 6));
        entre(session, () -> session.move("guerriere-1", 3, 3), "Guerrière se déplace de 1,5 m");
        entre(session, () -> session.attack("guerriere-1", "melee:Épée longue", "gobelin-1"),
                "Guerrière attaque Gobelin guerrier", "Gobelin guerrier subit");
        assertEquals(0, session.creature("gobelin-1").orElseThrow().hitPoints());
        entre(session, session::endTurn, "Fin du tour de Guerrière", "Tour de Gobelin guerrier");

        assertTrue(session.attackRefusal("gobelin-1").isPresent());
        int avantRefus = session.journal().size();
        assertThrows(IllegalArgumentException.class, () -> session.attack("gobelin-1", "melee:Cimeterre", "guerriere-1"));
        assertEquals(avantRefus, session.journal().size());
        entre(session, session::endTurn, "Fin du tour de Gobelin guerrier", "Tour de Magicienne");

        entre(session, () -> session.attack("magicienne-1", "melee:Dague", "loup-1"), "Magicienne attaque Loup", "RATÉ");
        entre(session, session::endTurn, "Fin du tour de Magicienne", "Tour de Squelette");

        entre(session, () -> session.move("squelette-1", 4.5, 7.5), "Squelette se déplace");
        var bouclier = session.attack("squelette-1", "melee:Épée courte", "magicienne-1");
        assertTrue(bouclier.entries().stream().anyMatch(e -> e.text().equals("Squelette attaque Magicienne (Épée courte)")));
        String decision = bouclier.decision().orElseThrow().id();
        entre(session, () -> session.answer(decision, ShieldReaction.CAST).orElseThrow(),
                "Magicienne utilise sa réaction : Bouclier", "RATÉ");
        entre(session, session::endTurn, "Fin du tour de Squelette", "Tour de Loup");

        entre(session, () -> session.attack("loup-1", "melee:Morsure", "magicienne-1"),
                "Loup attaque Magicienne", "Magicienne subit 6 dégâts perforants (PV 1/7)");
        entre(session, session::endTurn, "Fin du tour de Loup", "Round 2", "Tour de Guerrière");

        entre(session, session::endCombat, "Le combat se termine");
        assertTrue(session.combat().isEmpty());
        assertEquals(1, session.creature("magicienne-1").orElseThrow().hitPoints());

        // Fermer puis rouvrir
        var reprise = new SceneSession(Dice.scripted(), store);

        assertEquals(session.journal(), reprise.journal());
        assertEquals(positions(session), positions(reprise));
        assertTrue(reprise.combat().isEmpty());
        for (Token token : session.scene().orElseThrow().tokens()) {
            assertEquals(session.creature(token.entityId()).orElseThrow().signature(),
                    reprise.creature(token.entityId()).orElseThrow().signature());
        }
        assertEquals(session.combat().map(Combat::signature), reprise.combat().map(Combat::signature));
        assertEquals(0, store.setAsides);
    }

    private static dnd.engine.catalog.CatalogEntry entree(String id) {
        return SceneSession.catalogEntry(id).orElseThrow();
    }

    private static List<String> positions(SceneSession session) {
        return session.scene().orElseThrow().tokens().stream().map(t -> t.id() + "@" + t.position()).toList();
    }

    private static void sansEntree(SceneSession session, Supplier<?> commande) {
        int avant = session.journal().size();
        commande.get();
        assertEquals(avant, session.journal().size());
    }

    /** La commande ajoute des entrées au journal, et chacun des fragments se lit dans l'une d'elles. */
    private static void entre(SceneSession session, Supplier<?> commande, String... fragments) {
        int avant = session.journal().size();
        commande.get();
        List<JournalEntry> ajout = session.journal().subList(avant, session.journal().size());
        assertTrue(!ajout.isEmpty(), "aucune entrée au journal");
        for (String fragment : fragments) {
            assertTrue(ajout.stream().anyMatch(e -> e.text().contains(fragment)),
                    () -> "« " + fragment + " » absent de " + ajout.stream().map(JournalEntry::text).toList());
        }
    }
}
