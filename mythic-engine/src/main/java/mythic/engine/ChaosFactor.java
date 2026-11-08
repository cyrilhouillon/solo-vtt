package mythic.engine;

/** Le facteur de chaos, de 1 à 9 : plus il est haut, plus l'oracle penche vers oui et plus les événements surgissent. */
public record ChaosFactor(int value) {

    public static final int MIN = 1;
    public static final int MAX = 9;
    /** Le chaos d'une partie qui commence. */
    public static final ChaosFactor INITIAL = new ChaosFactor(5);

    public ChaosFactor {
        if (value < MIN || value > MAX) throw new IllegalArgumentException("Le facteur de chaos va de 1 à 9 : " + value);
    }

    public static ChaosFactor of(int value) {
        return new ChaosFactor(value);
    }

    /** Un cran de plus, sans dépasser 9. */
    public ChaosFactor raise() {
        return new ChaosFactor(Math.min(MAX, value + 1));
    }

    /** Un cran de moins, sans descendre sous 1. */
    public ChaosFactor lower() {
        return new ChaosFactor(Math.max(MIN, value - 1));
    }

    /** Le chaos après une scène : un cran de moins si le joueur a gardé le contrôle, sinon un de plus, entre 1 et 9. */
    public ChaosFactor afterScene(boolean inControl) {
        return inControl ? lower() : raise();
    }
}
