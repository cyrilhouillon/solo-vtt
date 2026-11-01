package dnd.engine.combat;

import dnd.engine.events.GameEvent;
import dnd.engine.events.GameEvent.CombatStarted;
import dnd.engine.events.GameEvent.InitiativeOrderSet;
import dnd.engine.events.GameEvent.InitiativeRolled;
import dnd.engine.events.GameEvent.RoundStarted;
import dnd.engine.state.Creature;
import dnd.engine.state.GameState;
import dnd.engine.state.InitiativeGroup;
import solovtt.engine.Decision;
import solovtt.engine.Engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Lance le combat (SRD 5.2.1, p. 13) : chaque créature tire d20 + son modificateur d'initiative, dans l'ordre
 * d'arrivée. L'ordre de jeu va du total le plus haut au plus bas ; à total égal, le modificateur le plus haut joue
 * d'abord ; à égalité complète, les créatures forment un groupe et le joueur choisit qui joue.
 */
public final class StartCombat extends CombatStep {

    private StartCombat() {
    }

    /** @throws IllegalStateException sans créature, ou si un combat est déjà en cours ; rien n'est émis */
    public static StartCombat of(GameState state) {
        if (state.combat().isPresent()) throw new IllegalStateException("Le combat est déjà lancé");
        if (state.creatures().isEmpty()) throw new IllegalStateException("Aucune créature : en poser avant de lancer le combat");
        return new StartCombat();
    }

    @Override
    protected Optional<Decision> resolve(Engine<GameState, GameEvent> engine) {
        engine.emit(new CombatStarted());
        record Rolled(String id, int total, int modifier) {}
        List<Rolled> rolls = new ArrayList<>();
        for (Creature c : List.copyOf(engine.state().creatures())) {
            int roll = engine.roll(20);
            engine.emit(new InitiativeRolled(c.id(), roll, c.initiative()));
            rolls.add(new Rolled(c.id(), roll + c.initiative(), c.initiative()));
        }
        List<InitiativeGroup> groups = new ArrayList<>();
        rolls.stream()
                .sorted(Comparator.comparingInt(Rolled::total).thenComparingInt(Rolled::modifier).reversed())
                .forEach(r -> {
                    InitiativeGroup last = groups.isEmpty() ? null : groups.getLast();
                    if (last != null && last.sameRank(r.total(), r.modifier())) {
                        List<String> members = new ArrayList<>(last.members());
                        members.add(r.id());
                        groups.set(groups.size() - 1, new InitiativeGroup(r.total(), r.modifier(), members));
                    } else {
                        groups.add(new InitiativeGroup(r.total(), r.modifier(), List.of(r.id())));
                    }
                });
        engine.emit(new InitiativeOrderSet(groups));
        engine.emit(new RoundStarted(1));
        return Turns.open(engine, Turns.POLICY.first(engine.state()));
    }
}
