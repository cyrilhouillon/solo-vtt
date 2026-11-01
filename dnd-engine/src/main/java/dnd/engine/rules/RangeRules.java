package dnd.engine.rules;

import dnd.engine.attack.AttackContext;
import dnd.engine.attack.AttackPhase;
import dnd.engine.attack.CoreAttackRules.AttackRule;
import dnd.engine.events.GameEvent;
import dnd.engine.state.GameState;
import solovtt.engine.Decision;
import solovtt.engine.Engine;

import java.util.List;
import java.util.Optional;

/**
 * Portée des attaques à distance (SRD 5.2.1, « Range »). Au-delà de la portée longue, l'attaque
 * est refusée avant d'être soumise ({@code AttackAction.of}) ; il ne reste ici que le désavantage.
 */
public final class RangeRules {

    public static final String LONG_RANGE = "portée longue";

    private RangeRules() {
    }

    public static List<AttackRule> all() {
        return List.of(new LongRange());
    }

    /** Au-delà de la portée normale : désavantage au jet d'attaque. */
    static final class LongRange implements AttackRule {
        public AttackPhase phase() { return AttackPhase.MODIFY_ROLL; }

        public boolean appliesTo(AttackContext ctx, GameState s) {
            return ctx.attack().beyondNormalRange(ctx.distance());
        }

        public Optional<Decision> handle(AttackContext ctx, Engine<GameState, GameEvent> e) {
            ctx.addDisadvantage(LONG_RANGE);
            return Optional.empty();
        }
    }
}
