package dnd.engine.state;

public enum DamageType {
    BLUDGEONING("contondants"),
    PIERCING("perforants"),
    SLASHING("tranchants"),
    FIRE("de feu"),
    COLD("de froid"),
    FORCE("de force");

    private final String label;

    DamageType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
