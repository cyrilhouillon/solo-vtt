package dnd.engine.sheet;

/** Les variantes de la boîte de jeux (SRD 5.2.1, « Tools »), dont le Soldat choisit une. */
public enum GamingSet {
    PLAYING_CARDS("cartes à jouer"),
    DICE("dés"),
    DRAGONCHESS("échecs draconiques"),
    THREE_DRAGON_ANTE("jeu des dragons");

    private final String label;

    GamingSet(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
