package solovtt.server;

import dnd.engine.catalog.CatalogEntry;
import dnd.engine.state.Creature;
import dnd.engine.state.DamageType;
import solovtt.scene.DistanceUnit;
import solovtt.scene.Scene;

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

    /** `catalogId` : l'entrée du catalogue d'où vient la créature, pour la reposer à l'identique. */
    record TokenDto(String id, String creatureId, String catalogId, String name, CatalogEntry.Kind kind, double x, double y) {}

    record SceneDto(double width, double height, DistanceUnit unit, String unitSymbol, List<TokenDto> tokens) {
        static SceneDto of(Scene scene, SceneSession session) {
            var tokens = scene.tokens().stream()
                    .map(t -> {
                        var entry = session.entryOf(t.entityId());
                        return new TokenDto(t.id(), t.entityId(), entry.id(),
                                session.creature(t.entityId()).map(Creature::name).orElse(t.entityId()),
                                entry.kind(), t.position().x(), t.position().y());
                    })
                    .toList();
            return new SceneDto(scene.width(), scene.height(), scene.unit(), scene.unit().symbol(), tokens);
        }
    }

    record Measure(double value, String unitSymbol) {}

    record WeaponDto(String name, int attackBonus, String damage, String damageType) {}

    record CreatureSheetDto(String id, String name, CatalogEntry.Kind kind, int armorClass,
                            int hitPoints, int maxHitPoints, Measure speed, WeaponDto weapon,
                            List<String> resistances, List<String> vulnerabilities) {

        /** La vitesse, en mètres dans le moteur, est donnée dans l'unité de la carte. */
        static CreatureSheetDto of(Creature c, CatalogEntry.Kind kind, DistanceUnit unit) {
            double speed = DistanceUnit.METRE.convert(c.speed(), unit);
            var w = c.weapons().isEmpty() ? null : c.weapons().getFirst();
            WeaponDto weapon = w == null ? null : new WeaponDto(w.name(), w.attackBonus(),
                    w.diceNotation() + (w.damageBonus() == 0 ? "" : "%+d".formatted(w.damageBonus())),
                    w.damageType().label());
            List<String> resistances = Arrays.stream(DamageType.values()).filter(c::isResistantTo).map(DamageType::label).toList();
            List<String> vulnerabilities = Arrays.stream(DamageType.values()).filter(c::isVulnerableTo).map(DamageType::label).toList();
            return new CreatureSheetDto(c.id(), c.name(), kind, c.armorClass(), c.hitPoints(), c.maxHitPoints(),
                    new Measure(speed, unit.symbol()), weapon, resistances, vulnerabilities);
        }
    }
}
