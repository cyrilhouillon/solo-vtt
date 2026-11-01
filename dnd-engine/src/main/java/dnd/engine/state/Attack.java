package dnd.engine.state;

import solovtt.scene.DistanceUnit;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

/**
 * Une façon d'attaquer avec une arme : au corps à corps (dans l'allonge) ou à distance
 * (dans la portée). Une Dague donne deux attaques : « Dague » et « Dague (lancer) ».
 */
public record Attack(Weapon weapon, Mode mode) {

    public enum Mode { MELEE, RANGED }

    /** Marge de calcul : une distance égale à l'allonge reste dans l'allonge. */
    private static final double EPSILON = 1e-9;

    public Attack {
        if (mode == Mode.MELEE && !weapon.isMelee() || mode == Mode.RANGED && weapon.range() == null) {
            throw new IllegalArgumentException(weapon.name() + " ne permet pas d'attaque " + mode);
        }
    }

    /** Identifiant stable dans les attaques d'une créature. */
    public String id() {
        return mode.name().toLowerCase(Locale.ROOT) + ":" + weapon.name();
    }

    public String label() {
        return isThrown() ? weapon.name() + " (lancer)" : weapon.name();
    }

    /** Attaque à distance avec une arme de corps à corps. */
    public boolean isThrown() {
        return mode == Mode.RANGED && weapon.isMelee();
    }

    /** Au-delà de la portée normale : le jet se fait avec désavantage. */
    public boolean beyondNormalRange(double distanceMetres) {
        return mode == Mode.RANGED && distanceMetres > weapon.range().normal() + EPSILON;
    }

    /**
     * La raison pour laquelle la cible ne peut pas être attaquée à cette distance, ou rien.
     * Les longueurs du message sont données dans {@code unit}.
     */
    public Optional<String> refusal(double distanceMetres, DistanceUnit unit) {
        double limit = mode == Mode.MELEE ? weapon.reach() : weapon.range().longRange();
        if (distanceMetres <= limit + EPSILON) return Optional.empty();
        String what = mode == Mode.MELEE ? "hors d'allonge" : "au-delà de la portée longue";
        return Optional.of("%s : cible à %s, %s (%s)".formatted(label(), length(distanceMetres, unit), what, length(limit, unit)));
    }

    private static String length(double metres, DistanceUnit unit) {
        var format = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.FRANCE));
        return format.format(DistanceUnit.METRE.convert(metres, unit)) + " " + unit.symbol();
    }
}
