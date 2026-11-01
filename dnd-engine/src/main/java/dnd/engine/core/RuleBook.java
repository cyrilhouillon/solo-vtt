package dnd.engine.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registre des règles actives, rangées par type de pipeline (déduit de l'enum de phases).
 * Activer un module de règles = enregistrer ses listeners.
 */
public final class RuleBook {

    private final Map<Class<?>, List<Listener<?, ?>>> byPhaseType = new HashMap<>();

    public <P extends Enum<P>, C> RuleBook register(Listener<P, C> listener) {
        Class<P> phaseType = listener.phase().getDeclaringClass();
        byPhaseType.computeIfAbsent(phaseType, k -> new ArrayList<>()).add(listener);
        return this;
    }

    public <P extends Enum<P>, C> RuleBook registerAll(List<? extends Listener<P, C>> listeners) {
        listeners.forEach(this::register);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <P extends Enum<P>, C> List<Listener<P, C>> listenersFor(Class<P> phaseType) {
        List<Listener<?, ?>> list = byPhaseType.getOrDefault(phaseType, List.of());
        return (List<Listener<P, C>>) (List<?>) List.copyOf(list);
    }
}
