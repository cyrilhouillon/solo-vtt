package dnd.engine.combat;

import dnd.engine.state.Combat;
import dnd.engine.state.GameState;
import dnd.engine.state.InitiativeGroup;
import solovtt.engine.TurnOrder;
import solovtt.engine.TurnPolicy;

import java.util.List;

/**
 * L'enchaînement des tours de D&D 2024 : les groupes d'initiative du plus haut au plus bas. Les membres d'un groupe
 * qui n'ont pas encore joué ce round sont candidats ; quand tous ont joué, vient le groupe suivant, et après le
 * dernier, un nouveau round qui repart du premier groupe.
 */
public final class InitiativePolicy implements TurnPolicy<GameState> {

    @Override
    public TurnOrder first(GameState state) {
        return next(combat(state));
    }

    @Override
    public TurnOrder afterTurn(GameState state, TurnOrder ended) {
        return next(combat(state));
    }

    private static TurnOrder next(Combat combat) {
        for (InitiativeGroup group : combat.groups()) {
            List<String> waiting = group.members().stream().filter(id -> !combat.hasPlayed(id)).toList();
            if (!waiting.isEmpty()) return TurnOrder.offer(combat.round(), waiting);
        }
        return TurnOrder.offer(combat.round() + 1, combat.groups().getFirst().members());
    }

    private static Combat combat(GameState state) {
        return state.combat().orElseThrow(() -> new IllegalStateException("Aucun combat en cours"));
    }
}
