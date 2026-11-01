package solovtt.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registre des règles actives, rangées par type de pipeline (déduit de l'enum de phases).
 * Activer un module de règles = enregistrer ses listeners.
 *
 * @param <S> le type d'état du système de jeu
 * @param <E> le type d'événement du système de jeu
 */
public final class RuleBook<S, E extends Event<S>> {

    private final Map<Class<?>, List<Listener<S, E, ?, ?>>> byPhaseType = new HashMap<>();

    public <P extends Enum<P>, C> RuleBook<S, E> register(Listener<S, E, P, C> listener) {
        Class<P> phaseType = listener.phase().getDeclaringClass();
        byPhaseType.computeIfAbsent(phaseType, k -> new ArrayList<>()).add(listener);
        return this;
    }

    public <P extends Enum<P>, C> RuleBook<S, E> registerAll(List<? extends Listener<S, E, P, C>> listeners) {
        listeners.forEach(this::register);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <P extends Enum<P>, C> List<Listener<S, E, P, C>> listenersFor(Class<P> phaseType) {
        List<Listener<S, E, ?, ?>> list = byPhaseType.getOrDefault(phaseType, List.of());
        return (List<Listener<S, E, P, C>>) (List<?>) List.copyOf(list);
    }
}
