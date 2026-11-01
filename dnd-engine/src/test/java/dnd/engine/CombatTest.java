package dnd.engine;

import dnd.engine.attack.AttackAction;
import dnd.engine.combat.EndTurn;
import dnd.engine.combat.JoinCombat;
import dnd.engine.combat.LeaveCombat;
import dnd.engine.combat.StartCombat;
import dnd.engine.events.GameEvent;
import dnd.engine.rules.StandardRules;
import dnd.engine.state.Combat;
import dnd.engine.state.Creature;
import dnd.engine.state.DamageType;
import dnd.engine.state.GameState;
import dnd.engine.state.Weapon;
import org.junit.jupiter.api.Test;
import solovtt.engine.Decision;
import solovtt.engine.Dice;
import solovtt.engine.Engine;
import solovtt.engine.StepResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guerrière (+2), Gobelin guerrier (+2) et Squelette (+3), posés dans cet ordre : les d20 d'initiative
 * sont consommés dans cet ordre.
 */
class CombatTest {

    static final Weapon SCIMITAR = new Weapon("Cimeterre", 1, 6, DamageType.SLASHING, 4, 2, true);

    static GameState table() {
        return new GameState()
                .add(Creature.builder("guerriere", "Guerrière").initiative(2).armorClass(18).hitPoints(12).build())
                .add(Creature.builder("gobelin", "Gobelin guerrier").initiative(2).armorClass(15).hitPoints(10).weapon(SCIMITAR).build())
                .add(Creature.builder("squelette", "Squelette").initiative(3).armorClass(14).hitPoints(13).build());
    }

    static Engine<GameState, GameEvent> engine(GameState state, int... dice) {
        return new Engine<>(state, StandardRules.create(), Dice.scripted(dice));
    }

    static Combat combat(Engine<GameState, GameEvent> e) {
        return e.state().combat().orElseThrow();
    }

    static String active(Engine<GameState, GameEvent> e) {
        return combat(e).activeId().orElseThrow();
    }

    static List<String> journal(Engine<GameState, GameEvent> e) {
        // Comme le serveur : chaque événement décrit au moment où il est émis.
        return e.events().stream().map(ev -> ev.describe(e.state())).toList();
    }

    @Test
    void lancerLeCombat_tireLInitiativeEtDonneLaMainAuPlusHaut() {
        var e = engine(table(), 10, 15, 4);

        assertInstanceOf(StepResult.Idle.class, e.submit(StartCombat.of(e.state())));

        assertEquals(List.of(
                "Le combat commence",
                "Initiative de Guerrière : d20 10 + 2 = 12",
                "Initiative de Gobelin guerrier : d20 15 + 2 = 17",
                "Initiative de Squelette : d20 4 + 3 = 7",
                "Ordre de jeu : Gobelin guerrier (17), Guerrière (12), Squelette (7)",
                "Round 1",
                "Tour de Gobelin guerrier"), journal(e));
        assertEquals(1, combat(e).round());
        assertEquals("gobelin", active(e));
    }

    @Test
    void finDuTour_passeAuSuivant_puisNouveauRoundApresLeDernier() {
        var e = engine(table(), 10, 15, 4);
        e.submit(StartCombat.of(e.state()));

        e.submit(EndTurn.of(e.state()));
        assertEquals("guerriere", active(e));
        e.submit(EndTurn.of(e.state()));
        assertEquals("squelette", active(e));
        int before = e.events().size();
        e.submit(EndTurn.of(e.state()));

        assertEquals("gobelin", active(e));
        assertEquals(2, combat(e).round());
        assertEquals(List.of("Fin du tour de Squelette", "Round 2", "Tour de Gobelin guerrier"),
                journal(e).subList(before, e.events().size()));
    }

    @Test
    void totalEgal_leModificateurLePlusHautJoueDAbord() {
        var e = engine(table(), 14, 5, 13);

        e.submit(StartCombat.of(e.state()));

        assertEquals("squelette", active(e));
        e.submit(EndTurn.of(e.state()));
        assertEquals("guerriere", active(e));
    }

    @Test
    void egaliteComplete_leJoueurChoisitQuiJoue_chacunUneFoisParRound() {
        var e = engine(table(), 10, 10, 1);

        var asked = assertInstanceOf(StepResult.AwaitingDecision.class, e.submit(StartCombat.of(e.state())));
        Decision who = asked.decision();
        assertEquals("Qui joue ?", who.prompt());
        assertEquals(List.of("Guerrière", "Gobelin guerrier"), who.options());
        assertThrows(IllegalStateException.class, () -> EndTurn.of(e.state()), "personne ne joue avant le choix");

        assertInstanceOf(StepResult.Idle.class, e.answer(who.id(), "Gobelin guerrier"));
        assertEquals("gobelin", active(e));

        e.submit(EndTurn.of(e.state()));
        assertEquals("guerriere", active(e), "seule restante du groupe : active d'office");
        e.submit(EndTurn.of(e.state()));
        assertEquals("squelette", active(e));

        var again = assertInstanceOf(StepResult.AwaitingDecision.class, e.submit(EndTurn.of(e.state())));
        assertEquals(2, combat(e).round());
        assertEquals(List.of("Guerrière", "Gobelin guerrier"), again.decision().options());
    }

    @Test
    void arriveeApresLeGroupeEnCours_joueCeRound_avant_auRoundSuivant() {
        var state = table();
        var e = engine(state, 10, 15, 4, /* Loup */ 3, /* Loup 2 */ 19);
        e.submit(StartCombat.of(state));
        e.submit(EndTurn.of(state)); // Guerrière (12) joue

        state.add(Creature.builder("loup", "Loup").initiative(2).build());
        e.submit(JoinCombat.of(state, "loup")); // 5 : après la Guerrière
        state.add(Creature.builder("loup2", "Loup 2").initiative(2).build());
        e.submit(JoinCombat.of(state, "loup2")); // 21 : avant, rang passé

        assertFalse(combat(e).hasPlayed("loup"));
        assertTrue(combat(e).hasPlayed("loup2"));
        e.submit(EndTurn.of(state));
        assertEquals("squelette", active(e));
        e.submit(EndTurn.of(state));
        assertEquals("loup", active(e));
        e.submit(EndTurn.of(state));
        assertEquals(2, combat(e).round());
        assertEquals("loup2", active(e));
    }

    @Test
    void laCreatureActiveQuitteLeCombat_leTourPasseALaSuivante() {
        var state = table();
        var e = engine(state, 10, 15, 4);
        e.submit(StartCombat.of(state));

        e.submit(LeaveCombat.of(state, "gobelin"));

        assertFalse(combat(e).includes("gobelin"));
        assertEquals("guerriere", active(e));
        assertTrue(journal(e).contains("Gobelin guerrier quitte le combat"));
    }

    @Test
    void leDernierQuiQuitteLeCombat_yMetFin() {
        var state = new GameState().add(Creature.builder("seul", "Seul").build());
        var e = engine(state, 5);
        e.submit(StartCombat.of(state));

        e.submit(LeaveCombat.of(state, "seul"));

        assertTrue(state.combat().isEmpty());
    }

    @Test
    void enCombat_seuleLaCreatureActivePeutAttaquer() {
        var state = table();
        var e = engine(state, 10, 15, 4);
        e.submit(StartCombat.of(state));
        e.submit(EndTurn.of(state)); // Guerrière active
        int before = e.events().size();

        var refused = assertThrows(IllegalArgumentException.class, () -> AttackAction.of("gobelin", "guerriere", state));

        assertEquals("Ce n'est pas le tour de Gobelin guerrier", refused.getMessage());
        assertEquals(before, e.events().size());
    }

    @Test
    void auDebutDeSonTour_laCreatureRecupereSaReaction_bouclierReste() {
        var state = new GameState()
                .add(Creature.builder("magicienne", "Magicienne").initiative(2).armorClass(12).build())
                .add(Creature.builder("gobelin", "Gobelin guerrier").initiative(2).build());
        var e = engine(state, 15, 10);
        e.emit(new GameEvent.ReactionUsed("magicienne", "Bouclier"));
        e.emit(new GameEvent.ArmorClassBonusApplied("magicienne", "Bouclier", 5));

        e.submit(StartCombat.of(state));

        assertTrue(state.get("magicienne").reactionAvailable());
        assertEquals(17, state.get("magicienne").armorClass(), "Bouclier n'expire pas encore");
        assertTrue(journal(e).contains("Magicienne récupère sa réaction"));
    }

    @Test
    void refus_sansCreature_dejaLance_ouHorsCombat_sansEvenement() {
        var empty = new GameState();
        assertThrows(IllegalStateException.class, () -> StartCombat.of(empty));
        assertThrows(IllegalStateException.class, () -> EndTurn.of(empty));

        var e = engine(table(), 10, 15, 4);
        e.submit(StartCombat.of(e.state()));
        int before = e.events().size();
        assertThrows(IllegalStateException.class, () -> StartCombat.of(e.state()));
        assertEquals(before, e.events().size());
    }

    @Test
    void rejouerLeJournalDuCombatRedonneLEtatFinal() {
        var state = table();
        var initial = state.copy();
        var e = engine(state, 10, 10, 1);
        var who = ((StepResult.AwaitingDecision) e.submit(StartCombat.of(state))).decision();
        e.answer(who.id(), "Guerrière");
        e.submit(EndTurn.of(state));
        e.submit(EndTurn.of(state));
        e.submit(EndTurn.of(state));

        e.events().forEach(ev -> ev.apply(initial));

        assertEquals(e.state().signature(), initial.signature());
        assertTrue(initial.signature().contains("combat[round=2"));
    }
}
