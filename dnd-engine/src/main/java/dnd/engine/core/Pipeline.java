package dnd.engine.core;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Exécute les listeners d'une action phase par phase, avec un curseur explicite.
 *
 * <p>Comme tout l'état d'avancement est dans des champs (phase courante, index du listener),
 * le pipeline peut s'arrêter à n'importe quel moment et reprendre plus tard, sans thread
 * bloqué ni callback imbriqué. C'est ce qui rend les réactions (Bouclier, Contresort…) possibles.</p>
 */
public final class Pipeline<P extends Enum<P>, C> implements Resolution {

    private final String name;
    private final Class<P> phaseType;
    private final P[] phases;
    private final C context;

    private int phaseIndex = -1;
    private List<Listener<P, C>> currentPhase = List.of();
    private int stepIndex = 0;

    public Pipeline(String name, Class<P> phaseType, C context) {
        this.name = name;
        this.phaseType = phaseType;
        this.phases = phaseType.getEnumConstants();
        this.context = context;
    }

    public C context() {
        return context;
    }

    @Override
    public Advance advance(Engine engine) {
        while (true) {
            if (stepIndex >= currentPhase.size()) {
                if (++phaseIndex >= phases.length) {
                    return new Completed();
                }
                enterPhase(engine, phases[phaseIndex]);
                continue;
            }

            Listener<P, C> listener = currentPhase.get(stepIndex++);
            if (!listener.appliesTo(context, engine.state())) {
                continue;
            }

            Optional<Decision> decision = listener.handle(context, engine);
            if (decision.isPresent()) {
                return new Waiting(decision.get());
            }
            if (engine.top() != this) {
                // Un listener a empilé une nouvelle résolution : elle passe en premier.
                return new Yielded();
            }
        }
    }

    /**
     * Les listeners d'une phase sont récupérés à l'entrée de la phase (donc après les
     * phases précédentes), puis triés par priorité. Le tri de Stream est stable :
     * à priorité égale, l'ordre d'enregistrement est conservé.
     */
    private void enterPhase(Engine engine, P phase) {
        currentPhase = engine.rules().<P, C>listenersFor(phaseType).stream()
                .filter(l -> l.phase() == phase)
                .sorted(Comparator.comparingInt((Listener<P, C> l) -> l.priority()))
                .toList();
        stepIndex = 0;
    }

    @Override
    public String toString() {
        String phase = phaseIndex >= 0 && phaseIndex < phases.length ? phases[phaseIndex].name() : "-";
        return name + "[" + phase + "]";
    }
}
