package dnd.engine.state;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class GameState {

    private final Map<String, Creature> creatures = new LinkedHashMap<>();
    /** Le combat en cours, ou {@code null} hors combat. */
    private Combat combat;

    public GameState add(Creature creature) {
        creatures.put(creature.id(), creature);
        return this;
    }

    public GameState remove(String id) {
        if (creatures.remove(id) == null) throw new IllegalArgumentException("Créature inconnue : " + id);
        return this;
    }

    public Creature get(String id) {
        Creature c = creatures.get(id);
        if (c == null) throw new IllegalArgumentException("Créature inconnue : " + id);
        return c;
    }

    public Collection<Creature> creatures() {
        return creatures.values();
    }

    public Optional<Combat> combat() {
        return Optional.ofNullable(combat);
    }

    // --- Mutations (réservées à GameEvent.apply) -------------------------------

    public void startCombat(List<InitiativeGroup> groups) {
        combat = new Combat(groups);
    }

    public void endCombat() {
        combat = null;
    }

    // --- Comparaison ------------------------------------------------------------

    public GameState copy() {
        GameState copy = new GameState();
        creatures.values().forEach(c -> copy.add(c.copy()));
        copy.combat = combat == null ? null : combat.copy();
        return copy;
    }

    public String signature() {
        String state = creatures.values().stream().map(Creature::signature).collect(Collectors.joining("\n"));
        return combat == null ? state : state + "\n" + combat.signature();
    }
}
