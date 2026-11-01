package dnd.engine.rules;

import dnd.engine.attack.AttackContext;
import dnd.engine.attack.AttackPhase;
import dnd.engine.core.Decision;
import dnd.engine.core.Engine;
import dnd.engine.core.Listener;
import dnd.engine.state.Condition;
import dnd.engine.state.GameState;

import java.util.List;
import java.util.Optional;

/** Effets des conditions sur les jets d'attaque. Ils ne font qu'accumuler des sources. */
public final class ConditionRules {

    private ConditionRules() {
    }

    public static List<Listener<AttackPhase, AttackContext>> all() {
        return List.of(new PoisonedAttacker(), new RestrainedTarget());
    }

    /** Empoisonné : désavantage aux jets d'attaque. */
    static final class PoisonedAttacker implements Listener<AttackPhase, AttackContext> {
        public AttackPhase phase() { return AttackPhase.MODIFY_ROLL; }

        public boolean appliesTo(AttackContext ctx, GameState s) {
            return s.get(ctx.attackerId()).has(Condition.POISONED);
        }

        public Optional<Decision> handle(AttackContext ctx, Engine e) {
            ctx.addDisadvantage("attaquant empoisonné");
            return Optional.empty();
        }
    }

    /** Entravé : les jets d'attaque contre la créature ont l'avantage. */
    static final class RestrainedTarget implements Listener<AttackPhase, AttackContext> {
        public AttackPhase phase() { return AttackPhase.MODIFY_ROLL; }

        public boolean appliesTo(AttackContext ctx, GameState s) {
            return s.get(ctx.targetId()).has(Condition.RESTRAINED);
        }

        public Optional<Decision> handle(AttackContext ctx, Engine e) {
            ctx.addAdvantage("cible entravée");
            return Optional.empty();
        }
    }
}
