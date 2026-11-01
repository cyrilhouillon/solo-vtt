package dnd.engine.state;

public enum Condition {
    INVISIBLE("Invisible"),
    POISONED("Empoisonné"),
    PRONE("À terre"),
    RESTRAINED("Entravé");

    private final String label;

    Condition(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
