package dnd.engine.rules;

import dnd.engine.attack.CoreAttackRules;
import solovtt.engine.RuleBook;
import dnd.engine.events.GameEvent;
import dnd.engine.state.GameState;

/** Assemble les règles disponibles. Chaque module (classe, sort, don) ne fait qu'ajouter des listeners. */
public final class StandardRules {

    private StandardRules() {
    }

    public static RuleBook<GameState, GameEvent> create() {
        return new RuleBook<GameState, GameEvent>()
                .registerAll(CoreAttackRules.all())
                .registerAll(ConditionRules.all())
                .register(new Bless())
                .register(new DuelingStyle())
                .register(new ShieldReaction());
    }
}
