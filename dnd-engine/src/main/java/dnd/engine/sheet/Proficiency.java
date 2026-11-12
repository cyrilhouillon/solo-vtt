package dnd.engine.sheet;

/** La maîtrise d'une compétence : aucune, maîtrise (+ bonus de maîtrise) ou expertise (deux fois le bonus). */
public enum Proficiency {
    NONE(0),
    PROFICIENT(1),
    EXPERTISE(2);

    private final int multiplier;

    Proficiency(int multiplier) {
        this.multiplier = multiplier;
    }

    /** Combien de fois le bonus de maîtrise s'ajoute. */
    public int multiplier() {
        return multiplier;
    }
}
