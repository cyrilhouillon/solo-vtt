package dnd.engine.combat;

import dnd.engine.events.GameEvent;
import dnd.engine.events.GameEvent.TurnEnded;
import dnd.engine.state.Combat;
import dnd.engine.state.GameState;
import solovtt.engine.Decision;
import solovtt.engine.Engine;
import solovtt.engine.TurnOrder;

import java.util.Optional;

/** Termine le tour de la créature active, puis ouvre le suivant (nouveau round après le dernier groupe). */
public final class EndTurn extends CombatStep {

    private final TurnOrder ended;

    private EndTurn(TurnOrder ended) {
        this.ended = ended;
    }

    /** @throws IllegalStateException hors combat, ou si personne ne joue encore ; rien n'est émis */
    public static EndTurn of(GameState state) {
        Combat combat = state.combat().orElseThrow(() -> new IllegalStateException("Aucun combat en cours"));
        TurnOrder turn = combat.turn().filter(t -> !t.awaitingChoice())
                .orElseThrow(() -> new IllegalStateException("Aucune créature ne joue : choisir d'abord qui joue"));
        return new EndTurn(turn);
    }

    @Override
    protected Optional<Decision> resolve(Engine<GameState, GameEvent> engine) {
        engine.emit(new TurnEnded(ended.active()));
        return Turns.open(engine, Turns.POLICY.afterTurn(engine.state(), ended));
    }
}
