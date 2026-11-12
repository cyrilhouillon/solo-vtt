package dnd.engine.sheet;

/** Les catégories de taille qu'une espèce jouable peut avoir (SRD 5.2.1). */
public enum CreatureSize {
    MEDIUM("M (moyenne)"),
    SMALL("P (petite)");

    private final String label;

    CreatureSize(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
