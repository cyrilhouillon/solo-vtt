package solovtt.engine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Les axiomes du moteur, vérifiés sans aucun système de jeu : un état jouet (un compteur),
 * deux événements et un pipeline à deux phases.
 */
class EngineTest {

    // --- Système de jeu jouet ----------------------------------------------------

    static final class Counter {
        int value;

        Counter copy() {
            Counter c = new Counter();
            c.value = value;
            return c;
        }
    }

    sealed interface ToyEvent extends Event<Counter> {
    }

    record Added(int amount) implements ToyEvent {
        public void apply(Counter state) {
            state.value += amount;
        }
    }

    record Noted(String text) implements ToyEvent {
    }

    enum ToyPhase { FIRST, SECOND }

    /** Le contexte partagé par les listeners : la trace des étapes exécutées. */
    record Trace(List<String> steps) {
        Trace() {
            this(new ArrayList<>());
        }
    }

    record Step(String name, ToyPhase phase, int priority,
                BiFunction<Trace, Engine<Counter, ToyEvent>, Optional<Decision>> body)
            implements Listener<Counter, ToyEvent, ToyPhase, Trace> {

        static Step of(String name, ToyPhase phase, int priority) {
            return new Step(name, phase, priority, (t, e) -> Optional.empty());
        }

        public Optional<Decision> handle(Trace trace, Engine<Counter, ToyEvent> engine) {
            trace.steps().add(name);
            return body.apply(trace, engine);
        }
    }

    static Engine<Counter, ToyEvent> engine(Counter state, RuleBook<Counter, ToyEvent> rules, int... dice) {
        return new Engine<>(state, rules, Dice.scripted(dice));
    }

    static Pipeline<Counter, ToyEvent, ToyPhase, Trace> action(Trace trace) {
        return new Pipeline<>("Action", ToyPhase.class, trace);
    }

    // --- Axiomes -------------------------------------------------------------------

    @Test
    void phasesSExecutentDansLOrdreDeLEnum() {
        var rules = new RuleBook<Counter, ToyEvent>()
                .register(Step.of("second", ToyPhase.SECOND, 0))
                .register(Step.of("premier", ToyPhase.FIRST, 0));
        var trace = new Trace();

        engine(new Counter(), rules).submit(action(trace));

        assertEquals(List.of("premier", "second"), trace.steps());
    }

    @Test
    void prioriteDansUnePhase_puisOrdreDEnregistrement() {
        var rules = new RuleBook<Counter, ToyEvent>()
                .register(Step.of("B", ToyPhase.FIRST, 10))
                .register(Step.of("C", ToyPhase.FIRST, 10))
                .register(Step.of("A", ToyPhase.FIRST, -5));
        var trace = new Trace();

        engine(new Counter(), rules).submit(action(trace));

        assertEquals(List.of("A", "B", "C"), trace.steps());
    }

    @Test
    void decisionSuspendLeMoteur_laReponseLeRelance() {
        var rules = new RuleBook<Counter, ToyEvent>()
                .register(new Step("question", ToyPhase.FIRST, 0, (t, e) ->
                        Optional.of(e.ask("joueur", "Ajouter 3 ?", List.of("oui", "non"), choice -> {
                            if (choice.equals("oui")) e.emit(new Added(3));
                        }))))
                .register(Step.of("suite", ToyPhase.SECOND, 0));
        var trace = new Trace();
        var e = engine(new Counter(), rules);

        var result = e.submit(action(trace));

        var pending = assertInstanceOf(StepResult.AwaitingDecision.class, result).decision();
        assertEquals(List.of("question"), trace.steps());
        assertEquals(Optional.of(pending), e.pendingDecision());

        assertInstanceOf(StepResult.Idle.class, e.answer(pending.id(), "oui"));
        assertEquals(List.of("question", "suite"), trace.steps());
        assertEquals(3, e.state().value);
        assertTrue(e.pendingDecision().isEmpty());
    }

    @Test
    void resolutionEmpilee_seResoutAvantLaSuite() {
        var rules = new RuleBook<Counter, ToyEvent>()
                .register(new Step("empile", ToyPhase.FIRST, 0, (t, e) -> {
                    e.push(engine -> {
                        engine.emit(new Noted("sous-résolution"));
                        return new Resolution.Completed();
                    });
                    return Optional.empty();
                }))
                .register(new Step("suite", ToyPhase.FIRST, 1, (t, e) -> {
                    e.emit(new Noted("suite"));
                    return Optional.empty();
                }));
        var e = engine(new Counter(), rules);

        e.submit(action(new Trace()));

        assertEquals(List.of(new Noted("sous-résolution"), new Noted("suite")), e.events());
    }

    @Test
    void soumettreAvecUneDecisionEnAttente_estRefuse() {
        var rules = new RuleBook<Counter, ToyEvent>()
                .register(new Step("question", ToyPhase.FIRST, 0, (t, e) ->
                        Optional.of(e.ask("joueur", "?", List.of("ok"), choice -> {
                        }))));
        var e = engine(new Counter(), rules);
        e.submit(action(new Trace()));

        assertThrows(IllegalStateException.class, () -> e.submit(action(new Trace())));
    }

    @Test
    void optionInvalide_estRefusee() {
        var rules = new RuleBook<Counter, ToyEvent>()
                .register(new Step("question", ToyPhase.FIRST, 0, (t, e) ->
                        Optional.of(e.ask("joueur", "?", List.of("ok"), choice -> {
                        }))));
        var e = engine(new Counter(), rules);
        var pending = ((StepResult.AwaitingDecision) e.submit(action(new Trace()))).decision();

        assertThrows(IllegalArgumentException.class, () -> e.answer(pending.id(), "autre"));
        assertEquals(Optional.of(pending), e.pendingDecision());
    }

    @Test
    void rejouerLeJournalRedonneLEtatFinal() {
        var rules = new RuleBook<Counter, ToyEvent>()
                .register(new Step("jet", ToyPhase.FIRST, 0, (t, e) -> {
                    e.emit(new Added(e.roll(6)));
                    e.emit(new Noted("jet appliqué"));
                    return Optional.empty();
                }))
                .register(new Step("second jet", ToyPhase.SECOND, 0, (t, e) -> {
                    e.emit(new Added(e.roll(6)));
                    return Optional.empty();
                }));
        var state = new Counter();
        var initial = state.copy();
        var e = engine(state, rules, 4, 6);

        e.submit(action(new Trace()));
        e.events().forEach(ev -> ev.apply(initial));

        assertEquals(10, e.state().value);
        assertEquals(e.state().value, initial.value);
    }
}
