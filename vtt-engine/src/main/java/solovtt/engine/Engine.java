package solovtt.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Le moteur : une pile de résolutions, un journal d'événements, et au plus une décision en attente.
 * Il ne connaît aucun système de jeu : l'état {@code S} et les événements {@code E} viennent du système.
 *
 * <p>Règle d'or : l'état du jeu ne change QUE via {@link #emit(Event)}. Chaque événement
 * sait s'appliquer à l'état, donc rejouer le journal sur l'état initial redonne l'état final
 * (journal de combat, annulation, replay, synchro réseau, tests).</p>
 *
 * <p>Utilisation typique :</p>
 * <pre>{@code
 * StepResult r = engine.submit(action);
 * while (r instanceof StepResult.AwaitingDecision(Decision d)) {
 *     r = engine.answer(d.id(), demanderAuJoueur(d));
 * }
 * }</pre>
 *
 * @param <S> le type d'état du système de jeu
 * @param <E> le type d'événement du système de jeu
 */
public final class Engine<S, E extends Event<S>> {

    private final S state;
    private final RuleBook<S, E> rules;
    private final Dice dice;

    private final List<E> events = new ArrayList<>();
    private final List<Consumer<E>> observers = new ArrayList<>();
    private final Deque<Resolution<S, E>> stack = new ArrayDeque<>();

    private Decision pending;
    private int nextDecisionId = 1;

    public Engine(S state, RuleBook<S, E> rules, Dice dice) {
        this.state = state;
        this.rules = rules;
        this.dice = dice;
    }

    // --- Accès ---------------------------------------------------------------

    public S state() {
        return state;
    }

    public RuleBook<S, E> rules() {
        return rules;
    }

    public List<E> events() {
        return Collections.unmodifiableList(events);
    }

    public Optional<Decision> pendingDecision() {
        return Optional.ofNullable(pending);
    }

    public void subscribe(Consumer<E> observer) {
        observers.add(observer);
    }

    // --- Outils pour les listeners ----------------------------------------------

    /** Seul point d'entrée des changements d'état. */
    public void emit(E event) {
        event.apply(state);
        events.add(event);
        observers.forEach(o -> o.accept(event));
    }

    public int roll(int sides) {
        return dice.roll(sides);
    }

    /** Crée une décision à retourner depuis {@link Listener#handle}. */
    public Decision ask(String chooserId, String prompt, List<String> options, Decision.DecisionHandler handler) {
        return new Decision("d" + nextDecisionId++, chooserId, prompt, options, handler);
    }

    /**
     * Empile une résolution depuis un listener ou un handler de décision (ex. Contresort
     * qui empile son propre sort au-dessus de celui qu'il vise). Elle sera résolue avant
     * que la résolution courante ne reprenne.
     */
    public void push(Resolution<S, E> resolution) {
        stack.push(resolution);
    }

    Resolution<S, E> top() {
        return stack.peek();
    }

    // --- API de pilotage (UI / IA / réseau) -----------------------------------

    public StepResult submit(Resolution<S, E> resolution) {
        if (pending != null) {
            throw new IllegalStateException("Une décision est en attente : " + pending.id());
        }
        stack.push(resolution);
        return run();
    }

    public StepResult answer(String decisionId, String option) {
        if (pending == null || !pending.id().equals(decisionId)) {
            throw new IllegalStateException("Aucune décision en attente avec l'id " + decisionId);
        }
        if (!pending.options().contains(option)) {
            throw new IllegalArgumentException("Option invalide '" + option + "', attendues : " + pending.options());
        }
        Decision decision = pending;
        pending = null;
        decision.handler().onChoice(option);
        return run();
    }

    private StepResult run() {
        while (!stack.isEmpty()) {
            Resolution<S, E> top = stack.peek();
            switch (top.advance(this)) {
                case Resolution.Completed c -> stack.pop();
                case Resolution.Waiting w -> {
                    pending = w.decision();
                    return new StepResult.AwaitingDecision(pending);
                }
                case Resolution.Yielded y -> {
                    // la boucle reprend avec la nouvelle résolution au sommet
                }
            }
        }
        return new StepResult.Idle();
    }
}
