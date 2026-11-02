package dnd.engine.combat;

import dnd.engine.events.GameEvent;
import dnd.engine.events.GameEvent.CombatEnded;
import dnd.engine.state.GameState;
import solovtt.engine.Decision;
import solovtt.engine.Engine;

import java.util.Optional;

/** Le joueur met fin au combat, à tout moment : plus de tours ; les créatures restent telles quelles. */
public final class EndCombat extends CombatStep {

    private EndCombat() {
    }

    /** @throws IllegalStateException hors combat ; rien n'est émis */
    public static EndCombat of(GameState state) {
        if (state.combat().isEmpty()) throw new IllegalStateException("Aucun combat en cours");
        return new EndCombat();
    }

    @Override
    protected Optional<Decision> resolve(Engine<GameState, GameEvent> engine) {
        engine.emit(new CombatEnded());
        return Optional.empty();
    }
}
