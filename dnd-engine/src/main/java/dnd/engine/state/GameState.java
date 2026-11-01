package dnd.engine.state;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class GameState {

    private final Map<String, Creature> creatures = new LinkedHashMap<>();

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

    public GameState copy() {
        GameState copy = new GameState();
        creatures.values().forEach(c -> copy.add(c.copy()));
        return copy;
    }

    public String signature() {
        return creatures.values().stream().map(Creature::signature).collect(Collectors.joining("\n"));
    }
}
