package dnd.engine.sheet;

/** L'équipement de départ d'un historique : son ensemble (A) ou 50 po (B) (SRD 5.2.1, « Character Backgrounds »). */
public enum BackgroundEquipment {
    A("Équipement A"),
    B("Équipement B");

    /** Les pièces d'or de l'équipement B. */
    public static final int GOLD_B = 50;

    private final String label;

    BackgroundEquipment(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
