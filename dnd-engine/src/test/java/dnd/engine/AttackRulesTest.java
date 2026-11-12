package dnd.engine;

import dnd.engine.attack.AttackAction;
import dnd.engine.attack.AttackContext;
import dnd.engine.attack.AttackPhase;
import dnd.engine.attack.CoreAttackRules.AttackRule;
import solovtt.engine.Decision;
import solovtt.engine.Dice;
import solovtt.engine.Engine;
import solovtt.engine.Resolution;
import solovtt.engine.RuleBook;
import solovtt.engine.StepResult;
import dnd.engine.events.GameEvent;
import dnd.engine.rules.Bless;
import dnd.engine.rules.DuelingStyle;
import dnd.engine.rules.ReactionWindow;
import dnd.engine.rules.ShieldReaction;
import dnd.engine.rules.StandardRules;
import dnd.engine.state.Condition;
import dnd.engine.state.Creature;
import dnd.engine.state.DamageType;
import dnd.engine.state.GameState;
import dnd.engine.state.Weapon;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Les dés sont scriptés : chaque test décrit exactement la situation de table qu'il vérifie.
 * Ordre des dés consommés : d20 (un ou deux), dés de bonus au jet, puis dés de dégâts.
 */
class AttackRulesTest {

    static final Weapon LONGSWORD = new Weapon("Épée longue", 1, 8, DamageType.SLASHING, 5, 3, true);

    static Creature.Builder fighter() {
        return Creature.builder("guerriere", "Guerrière").armorClass(18).hitPoints(28).weapon(LONGSWORD);
    }

    static Creature.Builder wizard() {
        return Creature.builder("magicienne", "Magicienne").armorClass(12).hitPoints(14)
                .knows(ShieldReaction.SPELL).spellSlots(1, 2);
    }

    static Engine<GameState, GameEvent> engine(GameState state, int... dice) {
        return new Engine<>(state, StandardRules.create(), Dice.scripted(dice));
    }

    static StepResult attack(Engine<GameState, GameEvent> e, String attacker, String target) {
        return e.submit(AttackAction.of(attacker, target, e.state()));
    }

    static <T extends GameEvent> List<T> eventsOf(Engine<GameState, GameEvent> e, Class<T> type) {
        return e.events().stream().filter(type::isInstance).map(type::cast).toList();
    }

    @Test
    void avantageEtDesavantageSAnnulent_unSeulD20() {
        var state = new GameState()
                .add(fighter().condition(Condition.POISONED).build())
                .add(wizard().condition(Condition.RESTRAINED).knows("rien").spellSlots(1, 0).build());
        var e = engine(state, 2, /* dégâts */ 4);

        assertInstanceOf(StepResult.Idle.class, attack(e, "guerriere", "magicienne"));

        var d20 = eventsOf(e, GameEvent.D20Rolled.class).getFirst();
        assertEquals(List.of(2), d20.rolls(), "un seul d20 quand avantage et désavantage se cumulent");
    }

    @Test
    void avantageSeul_garderLeMeilleur() {
        var state = new GameState()
                .add(fighter().build())
                .add(Creature.builder("cible", "Cible").armorClass(30).hitPoints(10).condition(Condition.RESTRAINED).build());
        var e = engine(state, 4, 17);

        attack(e, "guerriere", "cible");

        var d20 = eventsOf(e, GameEvent.D20Rolled.class).getFirst();
        assertEquals(List.of(4, 17), d20.rolls());
        assertEquals(17, d20.kept());
    }

    @Test
    void benedictionAjouteUnD4AuJet() {
        var state = new GameState()
                .add(fighter().effect(Bless.EFFECT).build())
                .add(Creature.builder("cible", "Cible").armorClass(30).hitPoints(10).build());
        var e = engine(state, 10, 3);

        attack(e, "guerriere", "cible");

        assertEquals(10 + 5 + 3, eventsOf(e, GameEvent.AttackResolved.class).getFirst().total());
    }

    @Test
    void bouclierTransformeUnCoupEnEchec() {
        var state = new GameState().add(fighter().build()).add(wizard().build());
        var e = engine(state, 9); // 9 + 5 = 14 : touche CA 12, rate CA 17

        var result = attack(e, "guerriere", "magicienne");

        var pending = assertInstanceOf(StepResult.AwaitingDecision.class, result).decision();
        assertEquals("magicienne", pending.chooserId());

        var end = e.answer(pending.id(), ShieldReaction.CAST);

        assertInstanceOf(StepResult.Idle.class, end);
        var mage = e.state().get("magicienne");
        assertEquals(14, mage.hitPoints());
        assertFalse(mage.reactionAvailable());
        assertEquals(1, mage.spellSlots(1));
        assertEquals(17, mage.armorClass());
        assertFalse(eventsOf(e, GameEvent.AttackResolved.class).getFirst().hit());
    }

    @Test
    void refuserBouclier_leCoupPorte() {
        var state = new GameState().add(fighter().build()).add(wizard().build());
        var e = engine(state, 9, /* dégâts */ 5);

        var pending = ((StepResult.AwaitingDecision) attack(e, "guerriere", "magicienne")).decision();
        e.answer(pending.id(), ReactionWindow.DECLINE);

        var mage = e.state().get("magicienne");
        assertEquals(14 - (5 + 3), mage.hitPoints());
        assertTrue(mage.reactionAvailable());
        assertEquals(2, mage.spellSlots(1));
    }

    @Test
    void bouclierPasProposeSiInutile() {
        var state = new GameState().add(fighter().build()).add(wizard().build());
        var e = engine(state, 15, 4); // 20 contre CA 12 : même +5 ne suffit pas

        assertInstanceOf(StepResult.Idle.class, attack(e, "guerriere", "magicienne"));
        assertTrue(e.state().get("magicienne").reactionAvailable());
    }

    @Test
    void bouclierPasProposeSur20Naturel_etDesDoubles() {
        var state = new GameState().add(fighter().build()).add(wizard().build());
        var e = engine(state, 20, 3, 4);

        assertInstanceOf(StepResult.Idle.class, attack(e, "guerriere", "magicienne"));
        var dmg = eventsOf(e, GameEvent.DiceRolled.class).getLast();
        assertEquals(List.of(3, 4), dmg.values(), "critique : dés de dégâts doublés");
        assertEquals(14 - (3 + 4 + 3), e.state().get("magicienne").hitPoints());
    }

    @Test
    void unNaturelRateToujours() {
        var state = new GameState().add(fighter().build())
                .add(Creature.builder("cible", "Cible").armorClass(1).hitPoints(10).build());
        var e = engine(state, 1);

        attack(e, "guerriere", "cible");

        assertFalse(eventsOf(e, GameEvent.AttackResolved.class).getFirst().hit());
    }

    @Test
    void ordreDesDegats_bonusAvantResistance() {
        var state = new GameState()
                .add(fighter().feature(DuelingStyle.FEATURE).build())
                .add(Creature.builder("cible", "Cible").armorClass(10).hitPoints(30)
                        .resistance(DamageType.SLASHING).build());
        var e = engine(state, 15, 4);

        attack(e, "guerriere", "cible");

        // (4 + 3 + 2) / 2 = 4, et pas 4/2 + 3 + 2 = 7
        assertEquals(30 - 4, e.state().get("cible").hitPoints());
    }

    @Test
    void ordreDesDegats_resistancePuisVulnerabilite() {
        var state = new GameState()
                .add(fighter().build())
                .add(Creature.builder("cible", "Cible").armorClass(10).hitPoints(30)
                        .resistance(DamageType.SLASHING).vulnerability(DamageType.SLASHING).build());
        var e = engine(state, 15, 4);

        attack(e, "guerriere", "cible");

        // 7 → 3 (résistance, arrondi inférieur) → 6 (vulnérabilité)
        assertEquals(30 - 6, e.state().get("cible").hitPoints());
    }

    @Test
    void unMalusAuxDegats_peutFaireZero_maisNeSoignePas() {
        var dague = new Weapon("Dague", 1, 4, DamageType.PIERCING, 5, -3, false);
        var state = new GameState()
                .add(Creature.builder("chetif", "Chétif").weapon(dague).build())
                .add(Creature.builder("cible", "Cible").armorClass(10).hitPoints(30).currentHitPoints(20).build());
        var e = engine(state, 15, 1);

        attack(e, "chetif", "cible");

        // 1 − 3 = −2 : 0 dégât, et la cible ne regagne aucun PV.
        assertEquals(20, e.state().get("cible").hitPoints());
    }

    @Test
    void prioriteDansUnePhase_puisOrdreDEnregistrement() {
        List<String> order = new ArrayList<>();
        record Probe(String name, int priority, List<String> out) implements AttackRule {
            public AttackPhase phase() { return AttackPhase.AFTER; }
            public int priority() { return priority; }
            public Optional<Decision> handle(AttackContext ctx, Engine<GameState, GameEvent> e) {
                out.add(name);
                return Optional.empty();
            }
        }
        var rules = StandardRules.create()
                .register(new Probe("B", 10, order))
                .register(new Probe("C", 10, order))
                .register(new Probe("A", -5, order));
        var state = new GameState().add(fighter().build())
                .add(Creature.builder("cible", "Cible").armorClass(30).hitPoints(10).build());

        new Engine<>(state, rules, Dice.scripted(2)).submit(AttackAction.of("guerriere", "cible", state));

        assertEquals(List.of("A", "B", "C"), order);
    }

    @Test
    void resolutionEmpilee_seResoutAvantLaSuite() {
        RuleBook<GameState, GameEvent> rules = StandardRules.create()
                .register(new AttackRule() {
                    public AttackPhase phase() { return AttackPhase.AFTER; }
                    public int priority() { return 0; }
                    public Optional<Decision> handle(AttackContext ctx, Engine<GameState, GameEvent> e) {
                        // ex. un effet déclenché qui a sa propre résolution (et pourrait poser ses propres questions)
                        e.push(engine -> {
                            engine.emit(new GameEvent.RuleApplied("Test", "sous-résolution"));
                            return new Resolution.Completed();
                        });
                        return Optional.empty();
                    }
                })
                .register(new AttackRule() {
                    public AttackPhase phase() { return AttackPhase.AFTER; }
                    public int priority() { return 1; }
                    public Optional<Decision> handle(AttackContext ctx, Engine<GameState, GameEvent> e) {
                        e.emit(new GameEvent.RuleApplied("Test", "suite de l'attaque"));
                        return Optional.empty();
                    }
                });
        var state = new GameState().add(fighter().build())
                .add(Creature.builder("cible", "Cible").armorClass(30).hitPoints(10).build());
        var e = new Engine<>(state, rules, Dice.scripted(2));

        e.submit(AttackAction.of("guerriere", "cible", state));

        var texts = eventsOf(e, GameEvent.RuleApplied.class).stream()
                .filter(r -> r.source().equals("Test")).map(GameEvent.RuleApplied::text).toList();
        assertEquals(List.of("sous-résolution", "suite de l'attaque"), texts);
    }

    @Test
    void rejouerLeJournalRedonneLEtatFinal() {
        var state = new GameState().add(fighter().effect(Bless.EFFECT).build()).add(wizard().build());
        var initial = state.copy();
        var e = engine(state, 8, 1);

        var pending = ((StepResult.AwaitingDecision) attack(e, "guerriere", "magicienne")).decision();
        e.answer(pending.id(), ShieldReaction.CAST);

        e.events().forEach(ev -> ev.apply(initial));
        assertEquals(e.state().signature(), initial.signature());
    }

    @Test
    void reponseInvalideRefusee() {
        var state = new GameState().add(fighter().build()).add(wizard().build());
        var e = engine(state, 9);
        var pending = ((StepResult.AwaitingDecision) attack(e, "guerriere", "magicienne")).decision();

        assertThrows(IllegalArgumentException.class, () -> e.answer(pending.id(), "Boule de feu"));
        assertThrows(IllegalStateException.class, () -> e.submit(AttackAction.of("guerriere", "magicienne", state)));
    }
}
