package solovtt.scene;

/**
 * Unité de longueur d'une carte. Positions et distances s'expriment dans cette unité.
 *
 * <p>La conversion suit la convention des battlemaps, 1,5 m pour 5 ft (une case),
 * et non le pied exact (0,3048 m) : une case reste une case dans les deux unités.</p>
 */
public enum DistanceUnit {
    METRE("m", 1.0),
    FOOT("ft", 0.3);

    private final String symbol;
    private final double metres;

    DistanceUnit(String symbol, double metres) {
        this.symbol = symbol;
        this.metres = metres;
    }

    public String symbol() {
        return symbol;
    }

    /** Convertit une longueur exprimée dans cette unité vers {@code target}. */
    public double convert(double value, DistanceUnit target) {
        return value * metres / target.metres;
    }
}
