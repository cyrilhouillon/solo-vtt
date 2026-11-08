package mythic.engine;

/** Ce que devient une scène attendue au test du chaos. */
public enum SceneOutcome {
    EXPECTED("Scène attendue"),
    ALTERED("Scène modifiée"),
    INTERRUPTED("Scène interrompue");

    private final String label;

    SceneOutcome(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
