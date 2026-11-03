package solovtt.server;

import dnd.engine.attack.AttackAction;
import dnd.engine.catalog.CatalogEntry;
import dnd.engine.catalog.SrdCatalog;
import dnd.engine.combat.EndCombat;
import dnd.engine.combat.EndTurn;
import dnd.engine.combat.JoinCombat;
import dnd.engine.combat.LeaveCombat;
import dnd.engine.combat.Move;
import dnd.engine.combat.StartCombat;
import dnd.engine.events.GameEvent;
import dnd.engine.rules.StandardRules;
import dnd.engine.state.Combat;
import dnd.engine.state.Creature;
import dnd.engine.state.GameState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import solovtt.engine.Decision;
import solovtt.engine.Dice;
import solovtt.engine.Engine;
import solovtt.engine.Resolution;
import solovtt.engine.StepResult;
import solovtt.scene.DistanceUnit;
import solovtt.scene.Position;
import solovtt.scene.Scene;
import solovtt.scene.Token;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * La scène en préparation et l'état D&D qui l'accompagne. Une seule session, en mémoire :
 * le jeu est local et solo.
 *
 * <p>La préparation construit l'état initial : elle ne passe pas par le journal du moteur.
 * Les actions (attaques) passent par le moteur, et chaque événement émis est décrit dans le journal
 * au moment où il arrive, pour garder les PV de cet instant.</p>
 *
 * <p>Quand une règle pose une décision (réaction Bouclier, « Qui joue ? »), l'action reste suspendue : attaque,
 * pose, retrait, lancement du combat, fin de tour et fin du combat sont refusés jusqu'à la réponse ({@link DecisionPendingException}).
 * Une nouvelle carte reste permise : elle jette le moteur, sa décision et le combat.</p>
 *
 * <p>Le combat passe par le moteur : lancement, fin de tour, fin du combat, et en combat l'entrée d'une créature posée (elle tire
 * son initiative) ou la sortie d'une créature retirée. La créature elle-même est créée et supprimée hors journal,
 * comme pendant la préparation. Un refus d'état (combat déjà lancé, pas de combat…) lève
 * {@link ActionRefusedException}.</p>
 *
 * <p>La session tient la campagne ouverte : sa partie et ses cartes enregistrées. Au lancement, aucune campagne n'est
 * ouverte, et créer ou charger une carte est refusé ({@link NoCampaignException}). Ouvrir une campagne remet la session
 * à zéro puis rejoue sa partie.</p>
 *
 * <p>La partie se garde par ses commandes : chaque appel réussi qui la modifie est ajouté, avec les dés tirés pendant
 * l'appel, à une {@link SavedGame} confiée au {@link GameStore} de la campagne. À l'ouverture, la session rejoue la partie
 * chargée avec ces dés : scène, état, journal et décision en attente reviennent à l'identique. Un rejeu qui échoue (règles
 * changées depuis la sauvegarde, partie illisible) laisse la session sans carte et met la partie de côté.</p>
 */
@ApplicationScoped
public class SceneSession {

    private Scene scene;
    /** L'empreinte de l'image de fond de la carte ({@link ImageStore}), {@code null} pour une carte sans image. */
    private String imageId;
    /** Le nom de la carte enregistrée d'où vient la carte, à titre d'information ; {@code null} pour une nouvelle carte. */
    private String mapName;
    /** La grille de la carte s'affiche : un choix d'affichage, sans effet sur les règles. */
    private boolean gridVisible = true;
    /**
     * Le décalage de la grille depuis le coin haut gauche de la carte, en fraction de case, dans [0, 1) : de l'affichage
     * et du calage côté client, sans effet sur les règles.
     */
    private double gridOffsetX;
    private double gridOffsetY;
    private GameState state;
    /** L'entrée du catalogue de chaque créature posée, par id de créature. */
    private final Map<String, CatalogEntry> entries = new HashMap<>();
    private final Map<String, Integer> instanceCounters = new HashMap<>();
    private final Dice dice;
    /** Les campagnes, {@code null} pour une session de test ouverte d'emblée sur une partie. */
    private final CampaignStore campaigns;
    /** La campagne ouverte, {@code null} avant d'en ouvrir une. */
    private Campaign campaign;
    /** La partie de la campagne ouverte. */
    private GameStore store;
    /** Les cartes enregistrées de la campagne ouverte. */
    private MapStore maps;
    /** D'où viennent les dés : ceux du jeu, ou ceux de la commande rejouée. */
    private Dice source;
    /** Les dés tirés pendant l'appel en cours, gardés avec sa commande. */
    private final List<Integer> rolls = new ArrayList<>();
    /** Les commandes réussies depuis la dernière carte. */
    private final List<SavedGame.SavedCommand> commands = new ArrayList<>();
    private boolean replaying;
    private Engine<GameState, GameEvent> engine;
    private final List<JournalEntry> journal = new ArrayList<>();
    /** Numéro de l'action en cours, qui groupe ses entrées dans le journal. */
    private int action;
    /** La cible de la dernière action (aucune pour le combat), pour rendre ses PV quand l'action reprend après une décision. */
    private String actionTargetId;

    /**
     * Une ligne du journal : la description d'un événement, l'action à laquelle il appartient, si elle ouvre un round,
     * et ce que la piste à dés en retient ({@code null} pour un détail ou ce qui n'est pas une attaque). {@code roll} et
     * {@code damage} portent les faits de l'issue et des dégâts, {@code dice} ceux d'un jet de dés : la piste les lit sans
     * relire le texte.
     */
    public record JournalEntry(int action, String text, boolean round, Highlight highlight, AttackRoll roll, Damage damage,
                               RolledDice dice) {}

    /**
     * Des dés tirés par le serveur : leurs faces et leurs valeurs, dans l'ordre du tirage. {@code kept} est la valeur
     * gardée d'un d20 (le meilleur avec avantage, le pire avec désavantage), {@code null} pour les autres dés.
     */
    public record RolledDice(int sides, List<Integer> values, Integer kept) {}

    private static RolledDice dice(GameEvent event) {
        return switch (event) {
            case GameEvent.D20Rolled d -> new RolledDice(20, d.rolls(), d.kept());
            case GameEvent.DiceRolled d -> new RolledDice(d.sides(), d.values(), null);
            default -> null;
        };
    }

    /** L'issue d'une attaque : touché, raté ou coup critique, et le calcul qui la tranche. */
    public record AttackRoll(Outcome outcome, int total, int armorClass) {}

    public enum Outcome { HIT, MISS, CRITICAL }

    /** Des dégâts subis, après résistance et vulnérabilité ; {@code type} est le libellé français. */
    public record Damage(int amount, String type) {}

    /** Ce que la piste retient d'une attaque : son issue, les dégâts, une réaction qui l'a changée. */
    public enum Highlight { OUTCOME, DAMAGE, REACTION }

    private static Highlight highlight(GameEvent event) {
        return switch (event) {
            case GameEvent.AttackResolved ignored -> Highlight.OUTCOME;
            case GameEvent.DamageDealt ignored -> Highlight.DAMAGE;
            case GameEvent.ReactionUsed ignored -> Highlight.REACTION;
            default -> null;
        };
    }

    /** Ce qu'une action (ou sa reprise) a ajouté au journal, sa cible ({@code null} sans cible), et la décision qui l'arrête, s'il y en a une. */
    public record ActionOutcome(List<JournalEntry> entries, String targetId, Optional<Decision> decision) {}

    private static final System.Logger LOG = System.getLogger(SceneSession.class.getName());

    /** La campagne sans nom d'une session ouverte d'emblée sur une partie (tests). */
    private static final Campaign UNNAMED = new Campaign("", "");

    /** Une session ouverte d'emblée sur une campagne sans nom, qui ne garde pas sa partie. */
    public SceneSession(Dice dice) {
        this(dice, GameStore.none());
    }

    /**
     * Une session ouverte d'emblée sur une campagne sans nom, qui reprend la partie gardée par {@code store}, puis l'y
     * sauve après chaque commande réussie ; ses cartes enregistrées vivent en mémoire.
     */
    public SceneSession(Dice dice, GameStore store) {
        this.dice = dice;
        this.source = dice;
        this.campaigns = null;
        this.campaign = UNNAMED;
        this.store = store;
        this.maps = MapStore.inMemory();
        restore();
    }

    /** La session de l'application : aucune campagne ouverte avant {@link #open(Campaign)}. */
    @Inject
    public SceneSession(Dice dice, CampaignStore campaigns) {
        this.dice = dice;
        this.source = dice;
        this.campaigns = campaigns;
        this.store = GameStore.none();
    }

    /**
     * Ouvre la campagne : la session repart de zéro puis rejoue sa partie. La campagne quittée n'a rien à garder de plus,
     * chaque commande y est déjà sauvée, décision en attente comprise. Permis à tout moment.
     */
    public synchronized void open(Campaign campaign) {
        if (campaigns == null) throw new IllegalStateException("session sans campagnes");
        GameStore games = campaigns.games(campaign.id());
        MapStore campaignMaps = campaigns.maps(campaign.id());
        reset();
        this.campaign = campaign;
        this.store = games;
        this.maps = campaignMaps;
        restore();
    }

    /** La campagne ouverte, vide avant d'en ouvrir une. */
    public synchronized Optional<Campaign> campaign() {
        return Optional.ofNullable(campaign);
    }

    /** Les cartes enregistrées de la campagne ouverte. */
    public synchronized MapStore maps() {
        requireCampaign();
        return maps;
    }

    private void requireCampaign() {
        if (campaign == null) throw new NoCampaignException();
    }

    /** Crée une carte, avec l'image de fond {@code imageId} ou sans ({@code null}) ; sa grille s'affiche. */
    public synchronized Scene create(double width, double height, DistanceUnit unit, String imageId) {
        requireCampaign();
        return create(width, height, unit, imageId, null);
    }

    /** Crée une carte qui vient de la carte enregistrée {@code mapName} ({@code null} pour une nouvelle carte). */
    private Scene create(double width, double height, DistanceUnit unit, String imageId, String mapName) {
        rolls.clear();
        commands.clear();
        Scene created = new Scene(width, height, unit);
        scene = created;
        this.imageId = imageId;
        this.mapName = mapName;
        gridVisible = true;
        gridOffsetX = 0;
        gridOffsetY = 0;
        state = new GameState();
        engine = new Engine<>(state, StandardRules.create(), sides -> {
            int value = source.roll(sides);
            rolls.add(value);
            return value;
        });
        engine.subscribe(event -> journal.add(
                new JournalEntry(action, event.describe(state), event instanceof GameEvent.RoundStarted, highlight(event),
                        event instanceof GameEvent.AttackResolved a
                                ? new AttackRoll(a.critical() ? Outcome.CRITICAL : a.hit() ? Outcome.HIT : Outcome.MISS, a.total(), a.armorClass())
                                : null,
                        event instanceof GameEvent.DamageDealt d ? new Damage(d.amount(), d.type().label()) : null,
                        dice(event))));
        journal.clear();
        action = 0;
        actionTargetId = null;
        entries.clear();
        instanceCounters.clear();
        record(r -> new SavedGame.SavedCommand.Create(width, height, unit, imageId, mapName, r));
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
        return recorded(() -> run(resolution, targetId), r -> new SavedGame.SavedCommand.Attack(attackerId, attackId, targetId, r));
    }

    /** Lance le combat : chaque créature tire son initiative. */
    public synchronized ActionOutcome startCombat() {
        if (scene == null) throw new IllegalStateException("aucune carte");
        requireNoDecision();
        return recorded(() -> run(refusable(() -> StartCombat.of(state)), null), SavedGame.SavedCommand.StartCombat::new);
    }

    /** Termine le tour de la créature active et ouvre le suivant. */
    public synchronized ActionOutcome endTurn() {
        if (scene == null) throw new IllegalStateException("aucune carte");
        requireNoDecision();
        return recorded(() -> run(refusable(() -> EndTurn.of(state)), null), SavedGame.SavedCommand.EndTurn::new);
    }

    /** Met fin au combat : plus de tours ; les créatures restent sur la carte avec leurs PV. */
    public synchronized ActionOutcome endCombat() {
        if (scene == null) throw new IllegalStateException("aucune carte");
        requireNoDecision();
        return recorded(() -> run(refusable(() -> EndCombat.of(state)), null), SavedGame.SavedCommand.EndCombat::new);
    }

    /**
     * Déplace un jeton. En combat, la créature active se déplace selon les règles D&D : dans sa vitesse restante
     * ({@link IllegalArgumentException} avec la raison sinon), et son déplacement entre au journal. Toute autre
     * créature, et toute créature hors combat, se repositionne librement, hors journal, comme pendant la préparation.
     * Un refus (règles, position hors de la carte) laisse le jeton et le journal intacts.
     */
    public synchronized ActionOutcome move(String tokenId, double x, double y) {
        if (scene == null) throw new IllegalStateException("aucune carte");
        requireNoDecision();
        Token token = scene.token(tokenId);
        Position to = new Position(x, y);
        String creatureId = token.entityId();
        return recorded(() -> {
            if (!Move.limits(creatureId, state)) {
                scene.move(tokenId, to);
                return new ActionOutcome(List.of(), null, Optional.empty());
            }
            Move move = Move.of(creatureId, token.position().distanceTo(to), scene.unit(), state);
            scene.move(tokenId, to);
            return run(move, null);
        }, r -> new SavedGame.SavedCommand.Move(tokenId, x, y, r));
    }

    /** La vitesse restante de la créature active, dans l'unité de la carte ; vide quand rien ne limite le déplacement. */
    public synchronized OptionalDouble movementLeft() {
        if (scene == null || state == null) return OptionalDouble.empty();
        var left = Move.remaining(state);
        return left.isPresent() ? OptionalDouble.of(DistanceUnit.METRE.convert(left.getAsDouble(), scene.unit())) : left;
    }

    public synchronized Optional<Combat> combat() {
        return state == null ? Optional.empty() : state.combat();
    }

    /** Une nouvelle action : ses entrées du journal partagent un numéro. */
    private ActionOutcome run(Resolution<GameState, GameEvent> resolution, String targetId) {
        int from = journal.size();
        action++;
        actionTargetId = targetId;
        return outcome(from, engine.submit(resolution));
    }

    /** Les refus d'état des règles (combat déjà lancé, aucun combat…) deviennent des refus d'action, rendus en 409. */
    private static <R> R refusable(java.util.function.Supplier<R> rule) {
        try {
            return rule.get();
        } catch (IllegalStateException e) {
            throw new ActionRefusedException(e.getMessage());
        }
    }

    /**
     * Répond à la décision en attente ; l'action suspendue reprend, et ses entrées gardent son numéro.
     * Une option absente lève {@link IllegalArgumentException} et laisse la décision en attente.
     *
     * @return les entrées ajoutées par la reprise, ou {@link Optional#empty()} si aucune décision n'a cet id
     */
    public synchronized Optional<ActionOutcome> answer(String decisionId, String option) {
        if (pendingDecision().filter(d -> d.id().equals(decisionId)).isEmpty()) return Optional.empty();
        return Optional.of(recorded(() -> {
            int from = journal.size();
            return outcome(from, engine.answer(decisionId, option));
        }, r -> new SavedGame.SavedCommand.Answer(decisionId, option, r)));
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

    /** Pourquoi la créature ne peut pas attaquer en ce moment (à terre, pas son tour), selon les règles D&D. */
    public synchronized Optional<String> attackRefusal(String creatureId) {
        return AttackAction.refusal(creatureId, state);
    }

    public synchronized List<JournalEntry> journal() {
        return List.copyOf(journal);
    }

    public synchronized Optional<Scene> scene() {
        return Optional.ofNullable(scene);
    }

    /** L'empreinte de l'image de fond de la carte, {@code null} sans image. */
    public synchronized String imageId() {
        return imageId;
    }

    /** Le nom de la carte enregistrée d'où vient la carte, {@code null} pour une nouvelle carte. */
    public synchronized String mapName() {
        return mapName;
    }

    public synchronized boolean gridVisible() {
        return gridVisible;
    }

    /**
     * Affiche ou masque la grille de la carte. C'est de l'affichage, pas une action de jeu : permis en combat
     * et pendant une décision.
     */
    public synchronized void showGrid(boolean visible) {
        if (scene == null) throw new IllegalStateException("aucune carte");
        recorded(() -> gridVisible = visible, r -> new SavedGame.SavedCommand.ShowGrid(visible, r));
    }

    public synchronized double gridOffsetX() {
        return gridOffsetX;
    }

    public synchronized double gridOffsetY() {
        return gridOffsetY;
    }

    /** Écart relatif toléré entre les échelles en largeur et en hauteur : les cases restent carrées. */
    private static final double SQUARE_TOLERANCE = 1e-6;

    /**
     * Cale la grille sur l'image de fond : la carte prend les dimensions {@code width} × {@code height}, chaque jeton est
     * mis à la même échelle (il reste au même endroit de l'image), et la grille part du décalage donné, en fraction de case.
     * Refusé en combat (la vitesse dépensée ne se remet pas à l'échelle), pendant une décision et sans image.
     *
     * @throws IllegalArgumentException dimension non positive, décalage hors de [0, 1), ou échelles différentes en largeur
     *                                  et en hauteur ; la carte ne change pas
     */
    public synchronized Scene alignGrid(double width, double height, double offsetX, double offsetY) {
        if (scene == null) throw new IllegalStateException("aucune carte");
        requireNoDecision();
        if (state.combat().isPresent()) throw new ActionRefusedException("Impossible de caler la grille pendant un combat");
        if (imageId == null) throw new ActionRefusedException("Impossible de caler la grille d'une carte sans image");
        if (!(width > 0) || !(height > 0) || Double.isInfinite(width) || Double.isInfinite(height)) {
            throw new IllegalArgumentException("Les dimensions de la carte doivent être positives : %s × %s".formatted(width, height));
        }
        if (!(offsetX >= 0 && offsetX < 1) || !(offsetY >= 0 && offsetY < 1)) {
            throw new IllegalArgumentException("Le décalage de la grille se donne en fraction de case, de 0 à moins de 1");
        }
        double factor = width / scene.width();
        if (Math.abs(height / scene.height() - factor) > SQUARE_TOLERANCE * factor) {
            throw new IllegalArgumentException("La largeur et la hauteur doivent changer d'échelle ensemble : les cases restent carrées");
        }
        return recorded(() -> {
            scene = scene.rescaled(factor);
            gridOffsetX = offsetX;
            gridOffsetY = offsetY;
            return scene;
        }, r -> new SavedGame.SavedCommand.AlignGrid(width, height, offsetX, offsetY, r));
    }

    /**
     * Crée une créature depuis le catalogue et pose son jeton ; le jeton porte l'id de la créature.
     * À partir de la deuxième instance d'une entrée, le nom est numéroté (« Loup 2 ») pour rester distinct.
     * En combat, la créature y entre : elle tire son initiative.
     */
    public synchronized Token place(CatalogEntry entry, double x, double y) {
        if (scene == null) throw new IllegalStateException("aucune carte");
        requireNoDecision();
        return recorded(() -> {
            int n = instanceCounters.getOrDefault(entry.id(), 0) + 1;
            String id = entry.id() + "-" + n;
            Token token = new Token(id, id, new Position(x, y));
            scene.place(token);
            instanceCounters.put(entry.id(), n);
            state.add(entry.create(id, n == 1 ? entry.name() : entry.name() + " " + n));
            entries.put(id, entry);
            if (state.combat().isPresent()) run(JoinCombat.of(state, id), null);
            return token;
        }, r -> new SavedGame.SavedCommand.Place(entry.id(), x, y, r));
    }

    /**
     * Retire le jeton et la créature qu'il porte ; la numérotation des suivantes ne recule pas.
     * En combat, la créature le quitte d'abord : si elle jouait, le tour suivant s'ouvre (et peut demander « Qui joue ? »).
     */
    public synchronized void remove(String tokenId) {
        if (scene == null) throw new IllegalStateException("aucune carte");
        requireNoDecision();
        recorded(() -> {
            String creatureId = scene.token(tokenId).entityId();
            if (state.combat().filter(c -> c.includes(creatureId)).isPresent()) run(LeaveCombat.of(state, creatureId), null);
            Token token = scene.remove(tokenId);
            state.remove(token.entityId());
            entries.remove(token.entityId());
            return token;
        }, r -> new SavedGame.SavedCommand.Remove(tokenId, r));
    }

    /**
     * La carte en cours comme modèle à enregistrer sous {@code name} : dimensions, unité, image, grille et son calage, et chaque créature
     * posée (son entrée du catalogue et sa position), dans l'ordre de pose. Ni PV, ni effets, ni combat : la partie ne
     * change pas.
     */
    public synchronized SavedMap snapshot(String name) {
        if (scene == null) throw new IllegalStateException("aucune carte");
        var tokens = scene.tokens().stream()
                .map(t -> new SavedMap.SavedToken(entries.get(t.entityId()).id(), t.position().x(), t.position().y()))
                .toList();
        return new SavedMap(name, scene.width(), scene.height(), scene.unit(), imageId, gridVisible, gridOffsetX, gridOffsetY, tokens);
    }

    /**
     * Remplace la carte en cours par la carte enregistrée, comme une nouvelle carte : la partie repart de zéro, sans
     * combat, et ses créatures sont neuves, aux positions enregistrées. La partie copie la carte en commandes ordinaires
     * (création, grille, calage, poses) et n'en garde le nom qu'à titre d'information. Permis en combat et pendant une décision.
     *
     * @throws ActionRefusedException si une créature de la carte n'est plus au catalogue ; la carte en cours ne change pas
     */
    public synchronized Scene load(SavedMap map) {
        requireCampaign();
        List<CatalogEntry> toPlace = map.tokens().stream()
                .map(t -> catalogEntry(t.catalogId()).orElseThrow(() -> new ActionRefusedException(
                        "Carte « " + map.name() + " » : créature inconnue au catalogue (" + t.catalogId() + ")")))
                .toList();
        create(map.width(), map.height(), map.unit(), map.imageId(), map.name());
        if (!map.gridVisible()) showGrid(false);
        if (map.gridOffsetX() != 0 || map.gridOffsetY() != 0) {
            alignGrid(map.width(), map.height(), map.gridOffsetX(), map.gridOffsetY());
        }
        for (int i = 0; i < toPlace.size(); i++) {
            place(toPlace.get(i), map.tokens().get(i).x(), map.tokens().get(i).y());
        }
        // Le calage remplace la scène créée : rendre celle qui porte les créatures.
        return scene;
    }

    public synchronized Optional<Creature> creature(String id) {
        if (!entries.containsKey(id)) return Optional.empty();
        return Optional.of(state.get(id));
    }

    public synchronized CatalogEntry entryOf(String creatureId) {
        return entries.get(creatureId);
    }

    /** Fait l'appel puis, seulement s'il réussit, l'ajoute à la partie avec les dés qu'il a tirés. */
    private <T> T recorded(Supplier<T> call, Function<List<Integer>, SavedGame.SavedCommand> command) {
        rolls.clear();
        T result = call.get();
        record(command);
        return result;
    }

    private void record(Function<List<Integer>, SavedGame.SavedCommand> command) {
        commands.add(command.apply(List.copyOf(rolls)));
        rolls.clear();
        if (!replaying) store.save(new SavedGame(commands));
    }

    /** Rejoue la partie gardée ; si elle ne se rejoue pas à l'identique, repart sans carte et la met de côté. */
    private void restore() {
        try {
            store.load().ifPresent(this::replay);
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Partie impossible à reprendre, mise de côté : " + e.getMessage(), e);
            reset();
            store.setAside();
        }
    }

    private void replay(SavedGame game) {
        replaying = true;
        try {
            for (SavedGame.SavedCommand command : game.commands()) {
                ReplayDice replayDice = new ReplayDice(command.rolls());
                source = replayDice;
                replay(command);
                if (!replayDice.used()) throw new IllegalStateException("Dés non consommés au rejeu de " + command);
            }
        } finally {
            replaying = false;
            source = dice;
        }
    }

    private void replay(SavedGame.SavedCommand command) {
        switch (command) {
            case SavedGame.SavedCommand.Create c -> create(c.width(), c.height(), c.unit(), c.imageId(), c.mapName());
            case SavedGame.SavedCommand.ShowGrid c -> showGrid(c.visible());
            case SavedGame.SavedCommand.AlignGrid c -> alignGrid(c.width(), c.height(), c.offsetX(), c.offsetY());
            case SavedGame.SavedCommand.Place c -> place(catalogEntry(c.catalogId())
                    .orElseThrow(() -> new IllegalStateException("Entrée de catalogue inconnue : " + c.catalogId())), c.x(), c.y());
            case SavedGame.SavedCommand.Remove c -> remove(c.tokenId());
            case SavedGame.SavedCommand.Move c -> move(c.tokenId(), c.x(), c.y());
            case SavedGame.SavedCommand.Attack c -> attack(c.attackerId(), c.attackId(), c.targetId());
            case SavedGame.SavedCommand.StartCombat c -> startCombat();
            case SavedGame.SavedCommand.EndTurn c -> endTurn();
            case SavedGame.SavedCommand.EndCombat c -> endCombat();
            case SavedGame.SavedCommand.Answer c -> answer(c.decisionId(), c.option())
                    .orElseThrow(() -> new IllegalStateException("Aucune décision " + c.decisionId() + " au rejeu"));
        }
    }

    /** Sans carte, comme au premier lancement. */
    private void reset() {
        scene = null;
        imageId = null;
        mapName = null;
        gridVisible = true;
        gridOffsetX = 0;
        gridOffsetY = 0;
        state = null;
        engine = null;
        journal.clear();
        action = 0;
        actionTargetId = null;
        entries.clear();
        instanceCounters.clear();
        commands.clear();
        rolls.clear();
    }

    /** Les dés d'une commande rejouée, dans l'ordre ; en manquer ou en laisser veut dire que le rejeu a divergé. */
    private static final class ReplayDice implements Dice {
        private final Deque<Integer> values;

        ReplayDice(List<Integer> values) {
            this.values = new ArrayDeque<>(values);
        }

        @Override
        public int roll(int sides) {
            Integer value = values.poll();
            if (value == null) throw new IllegalStateException("Dé manquant au rejeu (d" + sides + ")");
            if (value < 1 || value > sides) throw new IllegalStateException("Dé " + value + " impossible sur un d" + sides + " au rejeu");
            return value;
        }

        boolean used() {
            return values.isEmpty();
        }
    }

    public static Optional<CatalogEntry> catalogEntry(String id) {
        return SrdCatalog.entries().stream().filter(e -> e.id().equals(id)).findFirst();
    }
}
