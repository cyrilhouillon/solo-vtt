package dnd.engine.combat;

import dnd.engine.events.GameEvent;
import dnd.engine.events.GameEvent.ReactionRegained;
import dnd.engine.events.GameEvent.RoundStarted;
import dnd.engine.events.GameEvent.TurnOffered;
import dnd.engine.events.GameEvent.TurnStarted;
import dnd.engine.state.GameState;
import solovtt.engine.Decision;
import solovtt.engine.Engine;
import solovtt.engine.TurnOrder;

import java.util.List;
import java.util.Optional;

/** Ce que partagent les résolutions du combat : ouvrir le tour que la politique propose. */
final class Turns {

    static final InitiativePolicy POLICY = new InitiativePolicy();
    static final String WHO_PLAYS = "Qui joue ?";

    private Turns() {
    }

    /**
     * Ouvre le tour {@code next} : un nouveau round s'il en commence un, puis le tour du candidat unique,
     * ou la question « Qui joue ? » s'il y en a plusieurs.
     *
     * @return la décision à poser, s'il y a plusieurs candidats
     */
    static Optional<Decision> open(Engine<GameState, GameEvent> engine, TurnOrder next) {
        GameState state = engine.state();
        if (next.round() > state.combat().orElseThrow().round()) engine.emit(new RoundStarted(next.round()));
        if (next.activeId().isPresent()) {
            start(engine, next.activeId().get());
            return Optional.empty();
        }
        List<String> candidates = next.candidates();
        engine.emit(new TurnOffered(next.round(), candidates));
        List<String> names = candidates.stream().map(id -> state.get(id).name()).toList();
        return Optional.of(engine.ask(null, WHO_PLAYS, names, choice -> start(engine, candidates.get(names.indexOf(choice)))));
    }

    /** Le tour de la créature commence ; elle récupère sa réaction (SRD : au début de son tour). */
    private static void start(Engine<GameState, GameEvent> engine, String creatureId) {
        engine.emit(new TurnStarted(creatureId));
        if (!engine.state().get(creatureId).reactionAvailable()) engine.emit(new ReactionRegained(creatureId));
    }
}
