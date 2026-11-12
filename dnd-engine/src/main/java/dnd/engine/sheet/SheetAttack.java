package dnd.engine.sheet;

/**
 * Une attaque telle que le joueur l'écrit sur sa fiche : seul le bonus est un nombre, le reste est du texte libre
 * (« 1d8+3 », « tranchant », « 1,5 m » ou « 24/96 m »).
 */
public record SheetAttack(String name, int bonus, String damage, String damageType, String range) {

    public SheetAttack {
        name = CharacterSheet.text(name);
        if (name.isEmpty()) throw new IllegalArgumentException("Une attaque a un nom");
        damage = CharacterSheet.text(damage);
        damageType = CharacterSheet.text(damageType);
        range = CharacterSheet.text(range);
    }
}
