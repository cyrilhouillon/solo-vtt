package solovtt.server;

import dnd.engine.catalog.CatalogEntry;
import dnd.engine.catalog.SrdCatalog;
import dnd.engine.state.Creature;
import dnd.engine.state.GameState;
import jakarta.enterprise.context.ApplicationScoped;
import solovtt.scene.DistanceUnit;
import solovtt.scene.Position;
import solovtt.scene.Scene;
import solovtt.scene.Token;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * La scène en préparation et l'état D&D qui l'accompagne. Une seule session, en mémoire :
 * le jeu est local et solo ; la persistance viendra avec la story « reprendre une partie ».
 *
 * <p>La préparation construit l'état initial : elle ne passe pas par le journal du moteur.</p>
 */
@ApplicationScoped
public class SceneSession {

    private Scene scene;
    private GameState state;
    private final Map<String, CatalogEntry.Kind> kinds = new HashMap<>();
    private final Map<String, Integer> instanceCounters = new HashMap<>();

    public synchronized Scene create(double width, double height, DistanceUnit unit) {
        Scene created = new Scene(width, height, unit);
        scene = created;
        state = new GameState();
        kinds.clear();
        instanceCounters.clear();
        return created;
    }

    public synchronized Optional<Scene> scene() {
        return Optional.ofNullable(scene);
    }

    /** Crée une créature depuis le catalogue et pose son jeton ; le jeton porte l'id de la créature. */
    public synchronized Token place(CatalogEntry entry, double x, double y) {
        if (scene == null) throw new IllegalStateException("aucune carte");
        int n = instanceCounters.getOrDefault(entry.id(), 0) + 1;
        String id = entry.id() + "-" + n;
        Token token = new Token(id, id, new Position(x, y));
        scene.place(token);
        instanceCounters.put(entry.id(), n);
        state.add(entry.create(id));
        kinds.put(id, entry.kind());
        return token;
    }

    public synchronized Optional<Creature> creature(String id) {
        if (!kinds.containsKey(id)) return Optional.empty();
        return Optional.of(state.get(id));
    }

    public synchronized CatalogEntry.Kind kindOf(String creatureId) {
        return kinds.get(creatureId);
    }

    public static Optional<CatalogEntry> catalogEntry(String id) {
        return SrdCatalog.entries().stream().filter(e -> e.id().equals(id)).findFirst();
    }
}
