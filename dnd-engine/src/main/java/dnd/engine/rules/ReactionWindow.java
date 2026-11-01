package dnd.engine.rules;

import dnd.engine.events.GameEvent;
import dnd.engine.events.GameEvent.ReactionUsed;
import dnd.engine.events.GameEvent.RuleApplied;
import dnd.engine.state.Creature;
import dnd.engine.state.GameState;
import solovtt.engine.Decision;
import solovtt.engine.Engine;
import solovtt.engine.Resolution;

import java.util.ArrayList;
import java.util.List;

/**
 * Le moment où un déclencheur permet de réagir. Empilée par le listener du déclencheur, elle pose une question
 * par créature qui a au moins une réaction possible : une option par réaction, plus {@link #DECLINE}.
 *
 * <p>Les créatures sont interrogées l'une après l'autre, et les offres de chacune sont évaluées à son tour :
 * la réaction choisie par une créature précédente est déjà prise en compte.</p>
 *
 * @param <T> ce qui déclenche les réactions
 */
public final class ReactionWindow<T> implements Resolution<GameState, GameEvent> {

    public static final String DECLINE = "Ne rien faire";

    private final String situation;
    private final T trigger;
    private final List<Reaction<T>> reactions;
    private final List<String> reactorIds;
    private int next;

    /**
     * @param situation  ce qui vient de se passer, en tête de la question (« Magicienne est touchée… »)
     * @param reactorIds les créatures à interroger, dans l'ordre
     */
    public ReactionWindow(String situation, T trigger, List<Reaction<T>> reactions, List<String> reactorIds) {
        this.situation = situation;
        this.trigger = trigger;
        this.reactions = List.copyOf(reactions);
        this.reactorIds = List.copyOf(reactorIds);
    }

    @Override
    public Advance advance(Engine<GameState, GameEvent> engine) {
        while (next < reactorIds.size()) {
            Creature reactor = engine.state().get(reactorIds.get(next++));
            if (!reactor.reactionAvailable()) continue;
            List<Reaction<T>> offered = reactions.stream()
                    .filter(r -> r.offeredTo(reactor, trigger, engine.state()))
                    .toList();
            if (!offered.isEmpty()) return new Waiting(ask(reactor.id(), offered, engine));
        }
        return new Completed();
    }

    private Decision ask(String reactorId, List<Reaction<T>> offered, Engine<GameState, GameEvent> engine) {
        List<String> options = new ArrayList<>();
        offered.forEach(r -> options.add(r.option()));
        options.add(DECLINE);
        return engine.ask(reactorId, situation + " Utiliser sa réaction ?", options, choice -> {
            Creature reactor = engine.state().get(reactorId);
            offered.stream().filter(r -> r.option().equals(choice)).findFirst().ifPresentOrElse(
                    r -> {
                        engine.emit(new ReactionUsed(reactorId, r.name()));
                        r.apply(reactor, trigger, engine);
                    },
                    () -> engine.emit(new RuleApplied(reactor.name(),
                            "renonce à " + String.join(" ou ", offered.stream().map(Reaction::name).toList()))));
        });
    }
}
