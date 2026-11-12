package dnd.engine.state;

/** Les 13 types de dégâts du SRD 5.2.1 (« Damage Types ») ; le libellé suit « dégâts » dans le journal. */
public enum DamageType {
    BLUDGEONING("contondants"),
    PIERCING("perforants"),
    SLASHING("tranchants"),
    FIRE("de feu"),
    COLD("de froid"),
    FORCE("de force"),
    ACID("d’acide"),
    LIGHTNING("de foudre"),
    NECROTIC("nécrotiques"),
    POISON("de poison"),
    PSYCHIC("psychiques"),
    RADIANT("radiants"),
    THUNDER("de tonnerre");

    private final String label;

    DamageType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
