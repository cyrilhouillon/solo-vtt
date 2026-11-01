package dnd.engine;

import dnd.engine.attack.AttackAction;
import dnd.engine.attack.AttackContext;
import dnd.engine.attack.CoreAttackRules;
import dnd.engine.events.GameEvent;
import dnd.engine.rules.AttackHitReactions;
import dnd.engine.rules.Reaction;
import dnd.engine.rules.ReactionWindow;
import dnd.engine.rules.ShieldReaction;
import dnd.engine.state.Creature;
import dnd.engine.state.GameState;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;
import solovtt.engine.Engine;
import solovtt.engine.RuleBook;
import solovtt.engine.StepResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La Guerrière (+5) frappe la Magicienne (CA 12) : un d20 de 9 donne 14, que Bouclier ferait rater.
 * « Parade » est une réaction de test, proposée à toute créature qui la connaît : elle n'existe pas dans le SRD.
 */
class ReactionWindowTest {

    /** Réaction de test : +2 à la CA de qui la connaît (« parade »), quelle que soit la cible. */
    static final class Parry implements Reaction<AttackContext> {
        public String name() { return "Parade"; }
        public String option() { return "Parer"; }
        public boolean offeredTo(Creature reactor, AttackContext ctx, GameState s) {
            return reactor.knowsSpell("parade");
        }
        public void apply(Creature reactor, AttackContext ctx, Engine<GameState, GameEvent> e) {
            e.emit(new GameEvent.ArmorClassBonusApplied(reactor.id(), "Parade", 2));
        }
    }

    static Engine<GameState, GameEvent> engine(GameState state, int... dice) {
        var rules = new RuleBook<GameState, GameEvent>()
                .registerAll(CoreAttackRules.all())
                .register(new AttackHitReactions(List.of(new ShieldReaction(), new Parry())));
        return new Engine<>(state, rules, Dice.scripted(dice));
    }

    static GameState table(boolean wizardParries, boolean bystanderParries) {
        var wizard = AttackRulesTest.wizard();
        if (wizardParries) wizard.knows("parade");
        var bystander = Creature.builder("garde", "Garde").armorClass(14).hitPoints(10);
        if (bystanderParries) bystander.knows("parade");
        return new GameState().add(AttackRulesTest.fighter().build()).add(wizard.build()).add(bystander.build());
    }

    static StepResult.AwaitingDecision attack(Engine<GameState, GameEvent> e) {
        return assertInstanceOf(StepResult.AwaitingDecision.class,
                e.submit(AttackAction.of("guerriere", "magicienne", e.state())));
    }

    @Test
    void deuxReactionsPossibles_uneSeuleQuestionAvecLesDeux() {
        var e = engine(table(true, false), 9);

        var decision = attack(e).decision();

        assertEquals("magicienne", decision.chooserId());
        assertEquals("Coup au but sur Magicienne (14 contre CA 12). Utiliser sa réaction ?", decision.prompt());
        assertEquals(List.of(ShieldReaction.CAST, "Parer", ReactionWindow.DECLINE), decision.options());
    }

    @Test
    void choisirUneReaction_exclutLAutre() {
        var e = engine(table(true, false), 9, /* dégâts */ 5);

        var end = e.answer(attack(e).decision().id(), "Parer");

        assertInstanceOf(StepResult.Idle.class, end);
        var mage = e.state().get("magicienne");
        assertFalse(mage.reactionAvailable());
        assertEquals(2, mage.spellSlots(1), "Bouclier non lancé");
        assertEquals(14, mage.armorClass());
        assertEquals(14 - (5 + 3), mage.hitPoints(), "14 contre CA 14 : touché");
    }

    @Test
    void laCibleRenonce_puisUnTemoinEstInterroge() {
        var e = engine(table(false, true), 9, /* dégâts */ 5);

        var first = attack(e).decision();
        assertEquals(List.of(ShieldReaction.CAST, ReactionWindow.DECLINE), first.options());

        var second = assertInstanceOf(StepResult.AwaitingDecision.class, e.answer(first.id(), ReactionWindow.DECLINE)).decision();
        assertEquals("garde", second.chooserId());
        assertEquals(List.of("Parer", ReactionWindow.DECLINE), second.options());

        assertInstanceOf(StepResult.Idle.class, e.answer(second.id(), ReactionWindow.DECLINE));
        var texts = e.events().stream().filter(GameEvent.RuleApplied.class::isInstance)
                .map(ev -> ((GameEvent.RuleApplied) ev).text()).toList();
        assertTrue(texts.contains("renonce à Bouclier"), texts.toString());
        assertTrue(texts.contains("renonce à Parade"), texts.toString());
        assertEquals(14 - (5 + 3), e.state().get("magicienne").hitPoints());
    }

    @Test
    void sansReactionPossible_aucuneQuestion() {
        var e = engine(table(false, false), 15, 4); // 20 contre CA 12 : Bouclier ne suffit pas

        assertInstanceOf(StepResult.Idle.class, e.submit(AttackAction.of("guerriere", "magicienne", e.state())));
        assertTrue(e.state().get("magicienne").reactionAvailable());
    }
}
