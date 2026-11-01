package dnd.engine.rules;

import dnd.engine.attack.AttackContext;
import dnd.engine.attack.AttackPhase;
import dnd.engine.attack.CoreAttackRules.AttackRule;
import solovtt.engine.Decision;
import solovtt.engine.Engine;
import dnd.engine.events.GameEvent;
import dnd.engine.events.GameEvent.RuleApplied;
import dnd.engine.state.Attack;
import dnd.engine.state.GameState;

import java.util.Optional;

/**
 * Style de combat Duel : +2 aux dégâts avec une arme de corps à corps tenue à une main,
 * pour une attaque au corps à corps (pas quand on la lance).
 * Placé en DAMAGE_ADJUST, il s'applique donc AVANT une éventuelle résistance.
 */
public final class DuelingStyle implements AttackRule {

    public static final String FEATURE = "dueling";

    public AttackPhase phase() { return AttackPhase.DAMAGE_ADJUST; }

    public boolean appliesTo(AttackContext ctx, GameState s) {
        return ctx.hit() && ctx.attack().mode() == Attack.Mode.MELEE && ctx.weapon().oneHandedMelee() && s.get(ctx.attackerId()).hasFeature(FEATURE);
    }

    public Optional<Decision> handle(AttackContext ctx, Engine<GameState, GameEvent> e) {
        ctx.setDamage(ctx.damage() + 2);
        e.emit(new RuleApplied("Style Duel", "+2 → %d".formatted(ctx.damage())));
        return Optional.empty();
    }
}
