package dnd.engine.combat;

import dnd.engine.events.GameEvent;
import dnd.engine.events.GameEvent.CreatureMoved;
import dnd.engine.state.Combat;
import dnd.engine.state.Creature;
import dnd.engine.state.GameState;
import dnd.engine.state.Length;
import solovtt.engine.Decision;
import solovtt.engine.Engine;
import solovtt.scene.DistanceUnit;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Le déplacement de la créature active en combat (SRD 5.2.1, p. 14) : à son tour, elle se déplace d'au plus sa
 * vitesse, en autant de fois qu'elle veut ; chaque déplacement se déduit de ce qui reste. La distance est la ligne
 * droite du départ à l'arrivée, fournie par l'appelant. Hors combat, ou pour une autre créature, aucune règle ne
 * limite le déplacement : l'appelant repositionne librement ({@link #limits}).
 */
public final class Move extends CombatStep {

    /** Tolérance d'arrondi : un déplacement pile égal à la vitesse restante est permis. */
    private static final double EPSILON = 1e-6;

    private final String creatureId;
    private final double metres;
    private final DistanceUnit unit;

    private Move(String creatureId, double metres, DistanceUnit unit) {
        this.creatureId = creatureId;
        this.metres = metres;
        this.unit = unit;
    }

    /** Vrai si les règles limitent le déplacement de la créature : c'est la créature active d'un combat. */
    public static boolean limits(String creatureId, GameState state) {
        return state.combat().flatMap(Combat::activeId).filter(creatureId::equals).isPresent();
    }

    /** La vitesse restante de la créature active, en mètres ; vide sans créature active. */
    public static OptionalDouble remaining(GameState state) {
        return state.combat()
                .flatMap(c -> c.activeId().map(id -> c.movementLeft(state.get(id))))
                .map(OptionalDouble::of)
                .orElseGet(OptionalDouble::empty);
    }

    /**
     * Un déplacement de {@code distance}, exprimée dans {@code unit}. Refusé avant toute émission.
     *
     * @throws IllegalStateException hors combat, ou si la créature n'est pas la créature active
     * @throws IllegalArgumentException vitesse de 0, ou au-delà de la vitesse restante ; longueurs dites dans {@code unit}
     */
    public static Move of(String creatureId, double distance, DistanceUnit unit, GameState state) {
        Creature creature = state.get(creatureId);
        if (!limits(creatureId, state)) throw new IllegalStateException("Ce n'est pas le tour de " + creature.name());
        creature.speedZeroReason().ifPresent(reason -> {
            throw new IllegalArgumentException(reason);
        });
        double metres = unit.convert(distance, DistanceUnit.METRE);
        double left = remaining(state).orElseThrow();
        if (metres > left + EPSILON) {
            throw new IllegalArgumentException("%s ne peut plus parcourir que %s ce tour-ci (déplacement de %s)"
                    .formatted(creature.name(), Length.of(left, unit), Length.of(metres, unit)));
        }
        return new Move(creatureId, metres, unit);
    }

    @Override
    protected Optional<Decision> resolve(Engine<GameState, GameEvent> engine) {
        engine.emit(new CreatureMoved(creatureId, metres, unit));
        return Optional.empty();
    }
}
