package dnd.engine.rules;

import dnd.engine.attack.CoreAttackRules;
import dnd.engine.core.RuleBook;

/** Assemble les règles disponibles. Chaque module (classe, sort, don) ne fait qu'ajouter des listeners. */
public final class StandardRules {

    private StandardRules() {
    }

    public static RuleBook create() {
        return new RuleBook()
                .registerAll(CoreAttackRules.all())
                .registerAll(ConditionRules.all())
                .register(new Bless())
                .register(new DuelingStyle())
                .register(new ShieldReaction());
    }
}
