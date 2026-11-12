package dnd.engine.sheet;

/** Les emplacements de sort d'un niveau : combien la créature en a, et combien elle en a dépensé. */
public record SpellSlots(int level, int max, int expended) {

    public SpellSlots {
        if (level < 1 || level > 9) throw new IllegalArgumentException("Niveau d'emplacement de sort entre 1 et 9 : " + level);
        if (max < 0) throw new IllegalArgumentException("Nombre d'emplacements négatif au niveau " + level);
        if (expended < 0 || expended > max) {
            throw new IllegalArgumentException("Emplacements dépensés au niveau %d entre 0 et %d : %d".formatted(level, max, expended));
        }
    }
}
