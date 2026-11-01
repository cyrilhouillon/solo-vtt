package dnd.engine.core;

import dnd.engine.events.GameEvent;
import dnd.engine.state.GameState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Le moteur : une pile de résolutions, un journal d'événements, et au plus une décision en attente.
 *
 * <p>Règle d'or : l'état du jeu ne change QUE via {@link #emit(GameEvent)}. Chaque événement
 * sait s'appliquer à l'état, donc rejouer le journal sur l'état initial redonne l'état final
 * (journal de combat, annulation, replay, synchro réseau, tests).</p>
 *
 * <p>Utilisation typique :</p>
 * <pre>{@code
 * StepResult r = engine.submit(AttackAction.of("guerrier", "magicienne", engine.state()));
 * while (r instanceof StepResult.AwaitingDecision(Decision d)) {
 *     r = engine.answer(d.id(), demanderAuJoueur(d));
 * }
 * }</pre>
 */
public final class Engine {

    private final GameState state;
    private final RuleBook rules;
    private final Dice dice;

    private final List<GameEvent> events = new ArrayList<>();
    private final List<Consumer<GameEvent>> observers = new ArrayList<>();
    private final Deque<Resolution> stack = new ArrayDeque<>();

    private Decision pending;
    private int nextDecisionId = 1;

    public Engine(GameState state, RuleBook rules, Dice dice) {
        this.state = state;
        this.rules = rules;
        this.dice = dice;
    }

    // --- Accès ---------------------------------------------------------------

    public GameState state() {
        return state;
    }

    public RuleBook rules() {
        return rules;
    }

    public List<GameEvent> events() {
        return Collections.unmodifiableList(events);
    }

    public Optional<Decision> pendingDecision() {
        return Optional.ofNullable(pending);
    }

    public void subscribe(Consumer<GameEvent> observer) {
        observers.add(observer);
    }

    // --- Outils pour les listeners ----------------------------------------------

    /** Seul point d'entrée des changements d'état. */
    public void emit(GameEvent event) {
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
    public void push(Resolution resolution) {
        stack.push(resolution);
    }

    Resolution top() {
        return stack.peek();
    }

    // --- API de pilotage (UI / IA / réseau) -----------------------------------

    public StepResult submit(Resolution resolution) {
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
        decision.handler().onChoice(option, this);
        return run();
    }

    private StepResult run() {
        while (!stack.isEmpty()) {
            Resolution top = stack.peek();
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
