package solovtt.server;

import dnd.engine.attack.AttackAction;
import dnd.engine.attack.CoreAttackRules;
import dnd.engine.catalog.CatalogEntry;
import dnd.engine.catalog.SrdCatalog;
import dnd.engine.events.GameEvent;
import dnd.engine.rules.Bless;
import dnd.engine.rules.ConditionRules;
import dnd.engine.rules.DuelingStyle;
import dnd.engine.rules.RangeRules;
import dnd.engine.state.Creature;
import dnd.engine.state.GameState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import solovtt.engine.Dice;
import solovtt.engine.Engine;
import solovtt.engine.RuleBook;
import solovtt.scene.DistanceUnit;
import solovtt.scene.Position;
import solovtt.scene.Scene;
import solovtt.scene.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * La scène en préparation et l'état D&D qui l'accompagne. Une seule session, en mémoire :
 * le jeu est local et solo ; la persistance viendra avec la story « reprendre une partie ».
 *
 * <p>La préparation construit l'état initial : elle ne passe pas par le journal du moteur.
 * Les actions (attaques) passent par le moteur, et chaque événement émis est décrit dans le journal
 * au moment où il arrive, pour garder les PV de cet instant.</p>
 */
@ApplicationScoped
public class SceneSession {

    private Scene scene;
    private GameState state;
    /** L'entrée du catalogue de chaque créature posée, par id de créature. */
    private final Map<String, CatalogEntry> entries = new HashMap<>();
    private final Map<String, Integer> instanceCounters = new HashMap<>();
    private final Dice dice;
    private Engine<GameState, GameEvent> engine;
    private final List<JournalEntry> journal = new ArrayList<>();
    /** Numéro de l'action en cours, qui groupe ses entrées dans le journal. */
    private int action;

    /** Une ligne du journal : la description d'un événement, et l'action à laquelle il appartient. */
    public record JournalEntry(int action, String text) {}

    @Inject
    public SceneSession(Dice dice) {
        this.dice = dice;
    }

    public synchronized Scene create(double width, double height, DistanceUnit unit) {
        Scene created = new Scene(width, height, unit);
        scene = created;
        state = new GameState();
        engine = new Engine<>(state, rules(), dice);
        engine.subscribe(event -> journal.add(new JournalEntry(action, event.describe(state))));
        journal.clear();
        action = 0;
        entries.clear();
        instanceCounters.clear();
        return created;
    }

    /**
     * Toutes les règles de {@code StandardRules}, sauf la réaction Bouclier : elle pose une décision,
     * que l'interface ne sait pas encore présenter. Elle reviendra avec la story « répondre à une décision ».
     */
    private static RuleBook<GameState, GameEvent> rules() {
        return new RuleBook<GameState, GameEvent>()
                .registerAll(CoreAttackRules.all())
                .registerAll(ConditionRules.all())
                .registerAll(RangeRules.all())
                .register(new Bless())
                .register(new DuelingStyle());
    }

    /**
     * Résout une attaque. La distance est celle de la carte, convertie par les règles D&D,
     * qui refusent ce qu'elles interdisent ({@link IllegalArgumentException} avec la raison).
     *
     * @return les entrées du journal ajoutées par cette attaque
     */
    public synchronized List<JournalEntry> attack(String attackerId, String attackId, String targetId) {
        if (scene == null) throw new IllegalStateException("aucune carte");
        // Le jeton porte l'id de sa créature (voir place).
        double distance = scene.distance(attackerId, targetId);
        var resolution = AttackAction.of(attackerId, attackId, targetId, distance, scene.unit(), state);
        int from = journal.size();
        action++;
        engine.submit(resolution);
        return List.copyOf(journal.subList(from, journal.size()));
    }

    public synchronized List<JournalEntry> journal() {
        return List.copyOf(journal);
    }

    public synchronized Optional<Scene> scene() {
        return Optional.ofNullable(scene);
    }

    /**
     * Crée une créature depuis le catalogue et pose son jeton ; le jeton porte l'id de la créature.
     * À partir de la deuxième instance d'une entrée, le nom est numéroté (« Loup 2 ») pour rester distinct.
     */
    public synchronized Token place(CatalogEntry entry, double x, double y) {
        if (scene == null) throw new IllegalStateException("aucune carte");
        int n = instanceCounters.getOrDefault(entry.id(), 0) + 1;
        String id = entry.id() + "-" + n;
        Token token = new Token(id, id, new Position(x, y));
        scene.place(token);
        instanceCounters.put(entry.id(), n);
        state.add(entry.create(id, n == 1 ? entry.name() : entry.name() + " " + n));
        entries.put(id, entry);
        return token;
    }

    /** Retire le jeton et la créature qu'il porte ; la numérotation des suivantes ne recule pas. */
    public synchronized void remove(String tokenId) {
        if (scene == null) throw new IllegalStateException("aucune carte");
        Token token = scene.remove(tokenId);
        state.remove(token.entityId());
        entries.remove(token.entityId());
    }

    public synchronized Optional<Creature> creature(String id) {
        if (!entries.containsKey(id)) return Optional.empty();
        return Optional.of(state.get(id));
    }

    public synchronized CatalogEntry entryOf(String creatureId) {
        return entries.get(creatureId);
    }

    public static Optional<CatalogEntry> catalogEntry(String id) {
        return SrdCatalog.entries().stream().filter(e -> e.id().equals(id)).findFirst();
    }
}
