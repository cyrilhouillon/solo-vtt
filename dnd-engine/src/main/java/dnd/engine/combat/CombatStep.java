package dnd.engine.combat;

import dnd.engine.events.GameEvent;
import dnd.engine.state.GameState;
import solovtt.engine.Decision;
import solovtt.engine.Engine;
import solovtt.engine.Resolution;

import java.util.Optional;

/**
 * Une étape du combat qui se résout d'un coup, ou s'arrête sur « Qui joue ? » : la réponse émet le début du tour,
 * puis l'étape se termine.
 */
abstract class CombatStep implements Resolution<GameState, GameEvent> {

    private boolean done;

    @Override
    public final Advance advance(Engine<GameState, GameEvent> engine) {
        if (done) return new Completed();
        done = true;
        return resolve(engine).<Advance>map(Waiting::new).orElseGet(Completed::new);
    }

    /** Émet les événements de l'étape ; rend la décision qui la suspend, s'il y en a une. */
    protected abstract Optional<Decision> resolve(Engine<GameState, GameEvent> engine);
}
