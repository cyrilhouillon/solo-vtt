package mythic.engine;

/** Le focus d'un événement aléatoire, tiré au d100 dans la table de Mythic GME 2e. */
public enum EventFocus {
    REMOTE_EVENT("Événement distant", 5),
    AMBIGUOUS_EVENT("Événement ambigu", 10),
    NEW_NPC("Nouveau PNJ", 20),
    NPC_ACTION("Action d'un PNJ", 40),
    NPC_NEGATIVE("PNJ négatif", 45),
    NPC_POSITIVE("PNJ positif", 50),
    MOVE_TOWARD_A_THREAD("Vers un fil", 55),
    MOVE_AWAY_FROM_A_THREAD("Loin d'un fil", 65),
    CLOSE_A_THREAD("Clore un fil", 70),
    PC_NEGATIVE("PJ négatif", 80),
    PC_POSITIVE("PJ positif", 85),
    CURRENT_CONTEXT("Contexte actuel", 100);

    private final String label;
    /** Le plus haut jet de sa tranche ; la tranche commence après celle du focus précédent. */
    private final int upTo;

    EventFocus(String label, int upTo) {
        this.label = label;
        this.upTo = upTo;
    }

    public String label() {
        return label;
    }

    public static EventFocus of(int roll) {
        if (roll < 1 || roll > 100) throw new IllegalArgumentException("Un d100 va de 1 à 100 : " + roll);
        for (EventFocus focus : values()) if (roll <= focus.upTo) return focus;
        throw new AssertionError(roll);
    }
}
