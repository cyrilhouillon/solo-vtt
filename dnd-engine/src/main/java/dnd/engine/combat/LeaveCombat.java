package dnd.engine.combat;

import dnd.engine.events.GameEvent;
import dnd.engine.events.GameEvent.LeftCombat;
import dnd.engine.state.Combat;
import dnd.engine.state.GameState;
import solovtt.engine.Decision;
import solovtt.engine.Engine;
import solovtt.engine.TurnOrder;

import java.util.Optional;

/**
 * Une créature quitte le combat (retirée de la carte). Si elle jouait, son tour se termine et le suivant s'ouvre ;
 * s'il ne reste personne, le combat s'arrête.
 */
public final class LeaveCombat extends CombatStep {

    private final String creatureId;

    private LeaveCombat(String creatureId) {
        this.creatureId = creatureId;
    }

    /** @throws IllegalStateException si la créature n'est pas dans un combat ; rien n'est émis */
    public static LeaveCombat of(GameState state, String creatureId) {
        Combat combat = state.combat().orElseThrow(() -> new IllegalStateException("Aucun combat en cours"));
        if (!combat.includes(creatureId)) throw new IllegalStateException("Pas dans le combat : " + creatureId);
        return new LeaveCombat(creatureId);
    }

    @Override
    protected Optional<Decision> resolve(Engine<GameState, GameEvent> engine) {
        Optional<TurnOrder> turn = engine.state().combat().orElseThrow().turn();
        boolean wasActive = turn.flatMap(TurnOrder::activeId).filter(creatureId::equals).isPresent();
        engine.emit(new LeftCombat(creatureId));
        if (!wasActive || engine.state().combat().isEmpty()) return Optional.empty();
        return Turns.open(engine, Turns.POLICY.afterTurn(engine.state(), turn.get()));
    }
}
