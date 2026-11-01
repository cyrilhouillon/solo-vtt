package solovtt.scene;

/** Unité de longueur d'une carte. Positions et distances s'expriment dans cette unité. */
public enum DistanceUnit {
    METRE("m"),
    FOOT("ft");

    private final String symbol;

    DistanceUnit(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
