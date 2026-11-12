package dnd.engine.sheet;

/** Les listes de sorts où Initié à la magie peut puiser (SRD 5.2.1, « Magic Initiate »). */
public enum SpellList {
    CLERIC("Clerc"),
    DRUID("Druide"),
    WIZARD("Magicien");

    private final String label;

    SpellList(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
