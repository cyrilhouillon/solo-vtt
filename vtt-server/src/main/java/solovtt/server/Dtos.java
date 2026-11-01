package solovtt.server;

import dnd.engine.catalog.CatalogEntry;
import dnd.engine.state.Attack;
import dnd.engine.state.Creature;
import dnd.engine.state.DamageType;
import solovtt.scene.DistanceUnit;
import solovtt.scene.Scene;
import solovtt.scene.Token;

import java.util.Arrays;
import java.util.List;

/** Formes JSON échangées avec le client. */
final class Dtos {

    private Dtos() {
    }

    record ErrorDto(String message) {}

    record CatalogEntryDto(String id, String name, CatalogEntry.Kind kind) {
        static CatalogEntryDto of(CatalogEntry e) {
            return new CatalogEntryDto(e.id(), e.name(), e.kind());
        }
    }

    record NewSceneRequest(double width, double height, DistanceUnit unit) {}

    record PlaceTokenRequest(String catalogId, double x, double y) {}

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

    record SceneDto(double width, double height, DistanceUnit unit, String unitSymbol, List<TokenDto> tokens) {
        static SceneDto of(Scene scene, SceneSession session) {
            var tokens = scene.tokens().stream()
                    .map(t -> TokenDto.of(t, session.entryOf(t.entityId()), session.creature(t.entityId()).orElseThrow()))
                    .toList();
            return new SceneDto(scene.width(), scene.height(), scene.unit(), scene.unit().symbol(), tokens);
        }
    }

    record Measure(double value, String unitSymbol) {}

    private static double inUnit(double metres, DistanceUnit unit) {
        return DistanceUnit.METRE.convert(metres, unit);
    }

    record AttackRequest(String attackerId, String attackId, String targetId) {}

    record JournalEntryDto(int action, String text) {
        static JournalEntryDto of(SceneSession.JournalEntry e) {
            return new JournalEntryDto(e.action(), e.text());
        }
    }

    /** Les entrées ajoutées au journal par l'attaque, et les PV de la cible après coup. */
    record AttackResultDto(List<JournalEntryDto> entries, String targetId, int targetHitPoints, int targetMaxHitPoints) {}

    record RangeDto(double normal, double longRange, String unitSymbol) {}

    /** Une attaque de la feuille ; {@code reach} pour le corps à corps, {@code range} pour la distance. */
    record AttackDto(String id, String label, Attack.Mode mode, int attackBonus, String damage, String damageType,
                     Measure reach, RangeDto range) {
        static AttackDto of(Attack a, DistanceUnit unit) {
            var w = a.weapon();
            String damage = w.diceNotation() + (w.damageBonus() == 0 ? "" : "%+d".formatted(w.damageBonus()));
            Measure reach = a.mode() == Attack.Mode.MELEE ? new Measure(inUnit(w.reach(), unit), unit.symbol()) : null;
            RangeDto range = a.mode() == Attack.Mode.RANGED
                    ? new RangeDto(inUnit(w.range().normal(), unit), inUnit(w.range().longRange(), unit), unit.symbol())
                    : null;
            return new AttackDto(a.id(), a.label(), a.mode(), w.attackBonus(), damage, w.damageType().label(), reach, range);
        }
    }

    record CreatureSheetDto(String id, String name, CatalogEntry.Kind kind, int armorClass,
                            int hitPoints, int maxHitPoints, Measure speed, List<AttackDto> attacks,
                            List<String> resistances, List<String> vulnerabilities) {

        /** Vitesse, allonges et portées, en mètres dans le moteur, sont données dans l'unité de la carte. */
        static CreatureSheetDto of(Creature c, CatalogEntry.Kind kind, DistanceUnit unit) {
            double speed = inUnit(c.speed(), unit);
            List<AttackDto> attacks = c.attacks().stream().map(a -> AttackDto.of(a, unit)).toList();
            List<String> resistances = Arrays.stream(DamageType.values()).filter(c::isResistantTo).map(DamageType::label).toList();
            List<String> vulnerabilities = Arrays.stream(DamageType.values()).filter(c::isVulnerableTo).map(DamageType::label).toList();
            return new CreatureSheetDto(c.id(), c.name(), kind, c.armorClass(), c.hitPoints(), c.maxHitPoints(),
                    new Measure(speed, unit.symbol()), attacks, resistances, vulnerabilities);
        }
    }
}
