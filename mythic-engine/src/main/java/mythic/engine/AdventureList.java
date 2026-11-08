package mythic.engine;

import solovtt.engine.Dice;

import java.util.List;

/**
 * Une liste d'aventure de Mythic GME 2e (Fils ou Personnages), vue comme une table de hasard : seules comptent ses
 * lignes, écrites ou vides, dans l'ordre. La feuille a 25 lignes en 5 sections de 5 ; une section est active dès qu'on y
 * écrit. Avec une seule section active, un d10 lu par paires (1-2, 3-4…) donne la ligne ; avec plus, un dé de section
 * (d4, d6, d8 ou d10, lu par paires) la précède.
 *
 * @param written une valeur par ligne : {@code true} si une entrée y est écrite
 */
public record AdventureList(Kind kind, List<Boolean> written) {

    /** Les lignes d'une feuille de liste. */
    public static final int LINES = 25;
    /** Les lignes d'une section. */
    public static final int SECTION = 5;

    /** Les deux listes de Mythic. */
    public enum Kind {
        THREADS("Fils"),
        CHARACTERS("Personnages");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public AdventureList {
        if (kind == null) throw new IllegalArgumentException("Liste sans sorte");
        written = written == null ? List.of() : List.copyOf(written);
    }

    /** Une liste sans aucune ligne. */
    public static AdventureList empty(Kind kind) {
        return new AdventureList(kind, List.of());
    }

    /**
     * Tire une ligne. Aucune entrée écrite : {@link ListDraw.Empty}, sans dé ; une entrée écrite au-delà de la 25e ligne :
     * {@link ListDraw.Full}, sans dé, la feuille est à nettoyer ; sinon la ligne tirée, vide ou non.
     */
    public ListDraw draw(Dice dice) {
        int last = written.lastIndexOf(true) + 1;
        if (last == 0) return new ListDraw.Empty(kind);
        if (last > LINES) return new ListDraw.Full(kind);
        int sections = (last + SECTION - 1) / SECTION;
        Integer sectionDie = sections > 1 ? 2 * sections : null;
        Integer sectionRoll = sectionDie == null ? null : dice.roll(sectionDie);
        int section = sectionRoll == null ? 1 : (sectionRoll + 1) / 2;
        int lineRoll = dice.roll(10);
        int line = (section - 1) * SECTION + (lineRoll + 1) / 2;
        boolean blank = line > written.size() || !written.get(line - 1);
        return new ListDraw.Line(kind, line, sectionDie, sectionRoll, lineRoll, blank);
    }
}
