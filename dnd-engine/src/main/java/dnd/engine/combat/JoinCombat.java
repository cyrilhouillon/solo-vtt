package dnd.engine.combat;

import dnd.engine.events.GameEvent;
import dnd.engine.events.GameEvent.InitiativeRolled;
import dnd.engine.events.GameEvent.JoinedCombat;
import dnd.engine.state.Combat;
import dnd.engine.state.Creature;
import dnd.engine.state.GameState;
import dnd.engine.state.InitiativeGroup;
import solovtt.engine.Decision;
import solovtt.engine.Engine;

import java.util.Optional;

/**
 * Une créature arrive en plein combat : elle tire son initiative et prend son rang. Si ce rang vient avant le groupe
 * qui joue, il est déjà passé : elle jouera au round suivant ; sinon (même groupe ou après), dès ce round.
 */
public final class JoinCombat extends CombatStep {

    private final String creatureId;

    private JoinCombat(String creatureId) {
        this.creatureId = creatureId;
    }

    /** @throws IllegalStateException hors combat, ou si la créature y est déjà ; rien n'est émis */
    public static JoinCombat of(GameState state, String creatureId) {
        Combat combat = state.combat().orElseThrow(() -> new IllegalStateException("Aucun combat en cours"));
        state.get(creatureId);
        if (combat.includes(creatureId)) throw new IllegalStateException("Déjà dans le combat : " + creatureId);
        return new JoinCombat(creatureId);
    }

    @Override
    protected Optional<Decision> resolve(Engine<GameState, GameEvent> engine) {
        Creature creature = engine.state().get(creatureId);
        int roll = engine.roll(20);
        engine.emit(new InitiativeRolled(creatureId, roll, creature.initiative()));
        int total = roll + creature.initiative();
        Combat combat = engine.state().combat().orElseThrow();
        // Le groupe qui joue est celui des candidats du tour en cours.
        boolean passed = combat.turn()
                .flatMap(t -> combat.groupOf(t.candidates().getFirst()))
                .map(current -> ranksAhead(total, creature.initiative(), current))
                .orElse(false);
        engine.emit(new JoinedCombat(creatureId, total, creature.initiative(), passed));
        return Optional.empty();
    }

    /** La créature arrivée (total, modificateur) passe avant le groupe {@code current}. */
    private static boolean ranksAhead(int total, int modifier, InitiativeGroup current) {
        return !current.ranksBefore(total, modifier) && !current.sameRank(total, modifier);
    }
}
