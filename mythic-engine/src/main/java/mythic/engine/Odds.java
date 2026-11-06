package mythic.engine;

/** La probabilité d'une question, de la moins à la plus probable : une ligne de la table de destin. */
public enum Odds {
    IMPOSSIBLE("Impossible"),
    NEARLY_IMPOSSIBLE("Quasi impossible"),
    VERY_UNLIKELY("Très improbable"),
    UNLIKELY("Improbable"),
    FIFTY_FIFTY("50/50"),
    LIKELY("Probable"),
    VERY_LIKELY("Très probable"),
    NEARLY_CERTAIN("Quasi certain"),
    CERTAIN("Certain");

    private final String label;

    Odds(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
