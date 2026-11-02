package dnd.engine.state;

import solovtt.scene.DistanceUnit;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Une longueur du moteur (en mètres) dite au joueur dans l'unité de sa carte, au dixième. */
public final class Length {

    private Length() {
    }

    public static String of(double metres, DistanceUnit unit) {
        var format = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.FRANCE));
        return format.format(DistanceUnit.METRE.convert(metres, unit)) + " " + unit.symbol();
    }
}
