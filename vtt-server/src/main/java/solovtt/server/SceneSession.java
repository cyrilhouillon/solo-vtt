package solovtt.server;

import dnd.engine.attack.AttackAction;
import dnd.engine.catalog.CatalogEntry;
import dnd.engine.catalog.SrdCatalog;
import dnd.engine.events.GameEvent;
import dnd.engine.rules.StandardRules;
import dnd.engine.state.Creature;
import dnd.engine.state.GameState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import solovtt.engine.Decision;
import solovtt.engine.Dice;
import solovtt.engine.Engine;
import solovtt.engine.StepResult;
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
 *
 * <p>Quand une règle pose une décision (réaction Bouclier), l'action reste suspendue : attaque, pose et retrait
 * sont refusés jusqu'à la réponse ({@link DecisionPendingException}). Une nouvelle carte reste permise :
 * elle jette le moteur et sa décision.</p>
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
    /** La cible de la dernière action, pour rendre ses PV quand l'action reprend après une décision. */
    private String actionTargetId;

    /** Une ligne du journal : la description d'un événement, et l'action à laquelle il appartient. */
    public record JournalEntry(int action, String text) {}

    /** Ce qu'une action (ou sa reprise) a ajouté au journal, sa cible, et la décision qui l'arrête, s'il y en a une. */
    public record ActionOutcome(List<JournalEntry> entries, String targetId, Optional<Decision> decision) {}

    @Inject
    public SceneSession(Dice dice) {
        this.dice = dice;
    }

    public synchronized Scene create(double width, double height, DistanceUnit unit) {
        Scene created = new Scene(width, height, unit);
        scene = created;
        state = new GameState();
        engine = new Engine<>(state, StandardRules.create(), dice);
        engine.subscribe(event -> journal.add(new JournalEntry(action, event.describe(state))));
        journal.clear();
        action = 0;
        actionTargetId = null;
        entries.clear();
        instanceCounters.clear();
        return created;
    }

    /**
     * Résout une attaque. La distance est celle de la carte, convertie par les règles D&D,
     * qui refusent ce qu'elles interdisent ({@link IllegalArgumentException} avec la raison).
     *
     * @return les entrées du journal ajoutées par cette attaque, et la décision qui la suspend, s'il y en a une
     */
    public synchronized ActionOutcome attack(String attackerId, String attackId, String targetId) {
        if (scene == null) throw new IllegalStateException("aucune carte");
        requireNoDecision();
        // Le jeton porte l'id de sa créature (voir place).
        double distance = scene.distance(attackerId, targetId);
        var resolution = AttackAction.of(attackerId, attackId, targetId, distance, scene.unit(), state);
        int from = journal.size();
        action++;
        actionTargetId = targetId;
        return outcome(from, engine.submit(resolution));
    }

    /**
     * Répond à la décision en attente ; l'action suspendue reprend, et ses entrées gardent son numéro.
     * Une option absente lève {@link IllegalArgumentException} et laisse la décision en attente.
     *
     * @return les entrées ajoutées par la reprise, ou {@link Optional#empty()} si aucune décision n'a cet id
     */
    public synchronized Optional<ActionOutcome> answer(String decisionId, String option) {
        if (pendingDecision().filter(d -> d.id().equals(decisionId)).isEmpty()) return Optional.empty();
        int from = journal.size();
        return Optional.of(outcome(from, engine.answer(decisionId, option)));
    }

    public synchronized Optional<Decision> pendingDecision() {
        return engine == null ? Optional.empty() : engine.pendingDecision();
    }

    private ActionOutcome outcome(int from, StepResult result) {
        Optional<Decision> decision = result instanceof StepResult.AwaitingDecision(Decision d) ? Optional.of(d) : Optional.empty();
        return new ActionOutcome(List.copyOf(journal.subList(from, journal.size())), actionTargetId, decision);
    }

    private void requireNoDecision() {
        var pending = pendingDecision();
        if (pending.isPresent()) throw new DecisionPendingException(pending.get());
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
        requireNoDecision();
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
        requireNoDecision();
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
