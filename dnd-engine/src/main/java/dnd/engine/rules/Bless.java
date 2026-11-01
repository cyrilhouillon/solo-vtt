package dnd.engine.rules;

import dnd.engine.attack.AttackContext;
import dnd.engine.attack.AttackPhase;
import dnd.engine.core.Decision;
import dnd.engine.core.Engine;
import dnd.engine.core.Listener;
import dnd.engine.state.GameState;

import java.util.Optional;

/** Bénédiction : +1d4 aux jets d'attaque (et de sauvegarde, non modélisés ici). */
public final class Bless implements Listener<AttackPhase, AttackContext> {

    public static final String EFFECT = "bless";

    public AttackPhase phase() { return AttackPhase.MODIFY_ROLL; }

    public boolean appliesTo(AttackContext ctx, GameState s) {
        return s.get(ctx.attackerId()).hasEffect(EFFECT);
    }

    public Optional<Decision> handle(AttackContext ctx, Engine e) {
        ctx.addAttackDie(new AttackContext.DiceTerm("Bénédiction", 1, 4));
        return Optional.empty();
    }
}
