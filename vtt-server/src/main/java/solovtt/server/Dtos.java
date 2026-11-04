package solovtt.server;

import com.fasterxml.jackson.databind.JsonNode;
import dnd.engine.catalog.CatalogEntry;
import dnd.engine.state.Attack;
import dnd.engine.state.Combat;
import dnd.engine.state.Creature;
import dnd.engine.state.DamageType;
import solovtt.engine.Decision;
import solovtt.scene.DistanceUnit;
import solovtt.scene.Scene;
import solovtt.scene.Token;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Formes JSON échangées avec le client. */
final class Dtos {

    private Dtos() {
    }

    record ErrorDto(String message) {}

    /** Une image gardée, par son empreinte ; elle se relit sous `/api/images/{id}`. */
    record ImageDto(String id) {}

    record CatalogEntryDto(String id, String name, CatalogEntry.Kind kind) {
        static CatalogEntryDto of(CatalogEntry e) {
            return new CatalogEntryDto(e.id(), e.name(), e.kind());
        }
    }

    /** {@code imageId} : l'image de fond déjà envoyée ({@code /api/images}), absente pour une carte sans image. */
    record NewSceneRequest(double width, double height, DistanceUnit unit, String imageId) {}

    /** {@code null} si absent de la requête. */
    record GridRequest(Boolean visible) {}

    /**
     * Le calage de la grille : dimensions de la carte mise à l'échelle (cases carrées) et décalage de la grille, en fraction
     * de case ; {@code null} si absent.
     */
    record AlignmentRequest(Double width, Double height, Double gridOffsetX, Double gridOffsetY) {}

    /** Le nom sous lequel enregistrer la carte en cours, ou celui de la carte à charger ; {@code null} si absent. */
    record MapNameRequest(String name) {}

    /** Le nom d'une campagne à créer. */
    record CampaignNameRequest(String name) {}

    /** Le titre d'une page à créer ; {@code null} si absent. */
    record PageTitleRequest(String title) {}

    /** Le document de l'éditeur à garder pour une page ; {@code null} si absent. */
    record PageContentRequest(JsonNode content) {}

    /** La page consultée ; {@code null} si absent. */
    record LastPageRequest(String pageId) {}

    /** Une page, telle que la liste du Carnet la montre. */
    record PageSummaryDto(String id, String title) {
        static PageSummaryDto of(Page page) {
            return new PageSummaryDto(page.id(), page.title());
        }
    }

    /** {@code content} : le document de l'éditeur, {@code null} pour une page neuve. */
    record PageDto(String id, String title, JsonNode content) {}

    /** {@code lastPageId} : la dernière page consultée, {@code null} si aucune. */
    record NotebookDto(List<PageSummaryDto> pages, String lastPageId) {}

    record CampaignDto(String id, String name) {
        static CampaignDto of(Campaign campaign) {
            return new CampaignDto(campaign.id(), campaign.name());
        }
    }

    /** Une carte enregistrée, telle que la liste la montre. */
    record SavedMapDto(String name, double width, double height, String unitSymbol, String imageId, int creatureCount) {
        static SavedMapDto of(SavedMap map) {
            return new SavedMapDto(map.name(), map.width(), map.height(), map.unit().symbol(), map.imageId(), map.tokens().size());
        }
    }

    record PlaceTokenRequest(String catalogId, double x, double y) {}

    /** La position d'arrivée, dans l'unité de la carte ; {@code null} si absente de la requête. */
    record MoveTokenRequest(Double x, Double y) {}

    /**
     * `catalogId` : l'entrée du catalogue d'où vient la créature, pour la reposer à l'identique.
     * CA et PV permettent de lire l'état de chaque créature sans ouvrir sa feuille.
     */
    record TokenDto(String id, String creatureId, String catalogId, String name, CatalogEntry.Kind kind, double x, double y,
                    int armorClass, int hitPoints, int maxHitPoints) {
        static TokenDto of(Token token, CatalogEntry entry, Creature creature) {
            return new TokenDto(token.id(), token.entityId(), entry.id(), creature.name(), entry.kind(),
                    token.position().x(), token.position().y(),
                    creature.armorClass(), creature.hitPoints(), creature.maxHitPoints());
        }
    }

    /** {@code imageId} : l'image de fond, à lire sous {@code /api/images/{id}}, {@code null} sans image.
     * {@code mapName} : la carte enregistrée d'où vient la carte, à titre d'information ; {@code null} pour une nouvelle carte. */
    record SceneDto(double width, double height, DistanceUnit unit, String unitSymbol, String imageId, boolean gridVisible,
                    double gridOffsetX, double gridOffsetY, String mapName, List<TokenDto> tokens) {
        static SceneDto of(Scene scene, SceneSession session) {
            var tokens = scene.tokens().stream()
                    .map(t -> TokenDto.of(t, session.entryOf(t.entityId()), session.creature(t.entityId()).orElseThrow()))
                    .toList();
            return new SceneDto(scene.width(), scene.height(), scene.unit(), scene.unit().symbol(), session.imageId(),
                    session.gridVisible(), session.gridOffsetX(), session.gridOffsetY(), session.mapName(), tokens);
        }
    }

    record Measure(double value, String unitSymbol) {}

    private static double inUnit(double metres, DistanceUnit unit) {
        return DistanceUnit.METRE.convert(metres, unit);
    }

    record AttackRequest(String attackerId, String attackId, String targetId) {}

    /**
     * {@code round} : l'entrée ouvre un round, le journal la montre en séparateur. {@code highlight} : ce que la piste
     * à dés retient d'une attaque (issue, dégâts, réaction), ou {@code null}. {@code roll} et {@code damage} : les faits
     * de l'issue et des dégâts, pour que la piste ne relise pas le texte. {@code dice} : les dés d'un jet (faces, valeurs,
     * valeur gardée du d20), ou {@code null} ; ceux d'avant l'issue sont le jet d'attaque, ceux d'après les dégâts.
     */
    record JournalEntryDto(int action, String text, boolean round, SceneSession.Highlight highlight,
                           SceneSession.AttackRoll roll, SceneSession.Damage damage, SceneSession.RolledDice dice) {
        static JournalEntryDto of(SceneSession.JournalEntry e) {
            return new JournalEntryDto(e.action(), e.text(), e.round(), e.highlight(), e.roll(), e.damage(), e.dice());
        }
    }

    /**
     * Une question du moteur ; le joueur y répond pour toute créature (jeu solo). {@code chooserId} et
     * {@code chooserName} sont {@code null} quand aucune créature ne choisit (« Qui joue ? »).
     */
    record DecisionDto(String id, String chooserId, String chooserName, String prompt, List<String> options) {
        static DecisionDto of(Decision d, SceneSession session) {
            String name = session.creature(d.chooserId()).map(Creature::name).orElse(d.chooserId());
            return new DecisionDto(d.id(), d.chooserId(), name, d.prompt(), d.options());
        }
    }

    record AnswerRequest(String option) {}

    /** Un membre d'un groupe d'initiative ; {@code played} s'il a déjà joué ce round. */
    record TurnMemberDto(String creatureId, String name, boolean played) {}

    /** Les créatures qui partagent un rang d'initiative ; chacune joue une fois par round. */
    record InitiativeGroupDto(int initiative, List<TurnMemberDto> members) {}

    /**
     * Le combat en cours : le round, la créature qui joue ({@code null} tant que « Qui joue ? » attend),
     * les candidats du tour, l'ordre de jeu du plus haut au plus bas, et la vitesse restante de la créature active
     * dans l'unité de la carte ({@code movementLeft}, {@code null} quand rien ne limite le déplacement, jamais 0 pour
     * dire « illimité »).
     */
    record CombatDto(int round, String activeId, List<String> candidates, List<InitiativeGroupDto> groups, Double movementLeft) {
        static CombatDto of(Combat c, SceneSession session) {
            var groups = c.groups().stream().map(g -> new InitiativeGroupDto(g.initiative(), g.members().stream()
                            .map(id -> new TurnMemberDto(id, session.creature(id).map(Creature::name).orElse(id), c.hasPlayed(id)))
                            .toList()))
                    .toList();
            var left = session.movementLeft();
            return new CombatDto(c.round(), c.activeId().orElse(null),
                    c.turn().map(t -> t.candidates()).orElse(List.of()), groups,
                    left.isPresent() ? left.getAsDouble() : null);
        }
    }

    /**
     * Ce qu'une action (attaque, combat) ou sa reprise a produit : les entrées ajoutées au journal, la cible et ses PV
     * après coup ({@code null} sans cible), la décision qui suspend l'action ({@code null} si elle est finie),
     * et le combat ({@code null} hors combat).
     */
    record ActionResultDto(List<JournalEntryDto> entries, String targetId, Integer targetHitPoints, Integer targetMaxHitPoints,
                           DecisionDto decision, CombatDto combat) {
        static ActionResultDto of(SceneSession.ActionOutcome outcome, SceneSession session) {
            var target = Optional.ofNullable(outcome.targetId()).flatMap(session::creature);
            return new ActionResultDto(outcome.entries().stream().map(JournalEntryDto::of).toList(),
                    target.map(Creature::id).orElse(null),
                    target.map(Creature::hitPoints).orElse(null),
                    target.map(Creature::maxHitPoints).orElse(null),
                    outcome.decision().map(d -> DecisionDto.of(d, session)).orElse(null),
                    session.combat().map(c -> CombatDto.of(c, session)).orElse(null));
        }
    }

    record RangeDto(double normal, double longRange, String unitSymbol) {}

    /**
     * Une attaque de la feuille ; {@code reach} (l'allonge affichée) et {@code reachLimit} (la distance de centre à
     * centre jusqu'où les règles acceptent la cible) pour le corps à corps, {@code range} pour la distance.
     */
    record AttackDto(String id, String label, Attack.Mode mode, int attackBonus, String damage, String damageType,
                     Measure reach, Measure reachLimit, RangeDto range) {
        static AttackDto of(Attack a, DistanceUnit unit) {
            var w = a.weapon();
            String damage = w.diceNotation() + (w.damageBonus() == 0 ? "" : "%+d".formatted(w.damageBonus()));
            Measure reach = a.mode() == Attack.Mode.MELEE ? new Measure(inUnit(w.reach(), unit), unit.symbol()) : null;
            Measure reachLimit = a.reachLimit().map(m -> new Measure(inUnit(m, unit), unit.symbol())).orElse(null);
            RangeDto range = a.mode() == Attack.Mode.RANGED
                    ? new RangeDto(inUnit(w.range().normal(), unit), inUnit(w.range().longRange(), unit), unit.symbol())
                    : null;
            return new AttackDto(a.id(), a.label(), a.mode(), w.attackBonus(), damage, w.damageType().label(), reach, reachLimit, range);
        }
    }

    /** {@code attackRefusal} : pourquoi la créature ne peut pas attaquer en ce moment, ou {@code null}. */
    record CreatureSheetDto(String id, String name, CatalogEntry.Kind kind, int armorClass,
                            int hitPoints, int maxHitPoints, Measure speed, List<AttackDto> attacks,
                            List<String> resistances, List<String> vulnerabilities, String attackRefusal) {

        /** Vitesse, allonges et portées, en mètres dans le moteur, sont données dans l'unité de la carte. */
        static CreatureSheetDto of(Creature c, CatalogEntry.Kind kind, DistanceUnit unit, String attackRefusal) {
            double speed = inUnit(c.speed(), unit);
            List<AttackDto> attacks = c.attacks().stream().map(a -> AttackDto.of(a, unit)).toList();
            List<String> resistances = Arrays.stream(DamageType.values()).filter(c::isResistantTo).map(DamageType::label).toList();
            List<String> vulnerabilities = Arrays.stream(DamageType.values()).filter(c::isVulnerableTo).map(DamageType::label).toList();
            return new CreatureSheetDto(c.id(), c.name(), kind, c.armorClass(), c.hitPoints(), c.maxHitPoints(),
                    new Measure(speed, unit.symbol()), attacks, resistances, vulnerabilities, attackRefusal);
        }
    }
}
