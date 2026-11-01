package dnd.engine.attack;

import solovtt.engine.Pipeline;
import dnd.engine.events.GameEvent;
import dnd.engine.state.GameState;

public final class AttackAction {

    private AttackAction() {
    }

    /** Une attaque avec l'arme équipée de l'attaquant. */
    public static Pipeline<GameState, GameEvent, AttackPhase, AttackContext> of(String attackerId, String targetId, GameState state) {
        var context = new AttackContext(attackerId, targetId, state.get(attackerId).weapon());
        return new Pipeline<>("Attaque", AttackPhase.class, context);
    }
}
