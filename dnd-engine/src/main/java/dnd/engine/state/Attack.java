package dnd.engine.state;

import solovtt.scene.DistanceUnit;

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

    /**
     * Hors grille, une créature n'occupe pas une case exacte : une cible au corps à corps est à l'allonge si son centre
     * est à au plus allonge + une demi-case (0,75 m) du centre de l'attaquant. Le contact hors grille et la diagonale
     * (2,12 m) comptent ; deux cases d'écart (3 m) restent hors d'allonge.
     */
    public static final double CONTACT_TOLERANCE = Weapon.STANDARD_REACH / 2;

    /** La distance de centre à centre jusqu'où porte une attaque au corps à corps (allonge + demi-case), ou rien à distance. */
    public Optional<Double> reachLimit() {
        return mode == Mode.MELEE ? Optional.of(weapon.reach() + CONTACT_TOLERANCE) : Optional.empty();
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
        double limit = reachLimit().orElseGet(() -> weapon.range().longRange());
        if (distanceMetres <= limit + EPSILON) return Optional.empty();
        String what = mode == Mode.MELEE ? "hors d'allonge" : "au-delà de la portée longue";
        double shown = mode == Mode.MELEE ? weapon.reach() : limit;
        return Optional.of("%s : cible à %s, %s (%s)".formatted(label(), Length.of(distanceMetres, unit), what, Length.of(shown, unit)));
    }
}
