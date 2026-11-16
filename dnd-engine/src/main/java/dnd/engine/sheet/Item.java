package dnd.engine.sheet;

/**
 * Un objet de l'inventaire : une arme du SRD, une armure ou un bouclier du SRD, ou un objet décrit par son nom ; un seul
 * des trois. {@code equipped} : l'armure est portée, l'arme ou le bouclier tenu ; jamais pour un objet en texte.
 */
public record Item(SrdWeapon weapon, SrdArmor armor, String name, int quantity, boolean equipped) {

    public static final int MAX_NAME_LENGTH = 80;

    public Item {
        name = name == null ? null : name.strip();
        int kinds = (weapon == null ? 0 : 1) + (armor == null ? 0 : 1) + (name == null ? 0 : 1);
        if (kinds != 1) throw new IllegalArgumentException("Un objet est une arme, une armure ou un nom");
        if (name != null && name.isEmpty()) throw new IllegalArgumentException("Un objet a un nom");
        if (name != null && name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Nom d’objet trop long (%d caractères au plus)".formatted(MAX_NAME_LENGTH));
        }
        if (quantity < 1) throw new IllegalArgumentException("%s : quantité d’au moins 1".formatted(name == null ? label(weapon, armor) : name));
        if (equipped && name != null) throw new IllegalArgumentException(name + " : seules armes, armures et boucliers s’équipent");
    }

    public static Item weapon(SrdWeapon weapon, int quantity) {
        return new Item(weapon, null, null, quantity, true);
    }

    public static Item armor(SrdArmor armor) {
        return new Item(null, armor, null, 1, true);
    }

    public static Item named(String name, int quantity) {
        return new Item(null, null, name, quantity, false);
    }

    /** Le nom de l'objet tel qu'il s'affiche. */
    public String label() {
        return name != null ? name : label(weapon, armor);
    }

    /** Le nom, suivi de « ×n » au-delà d'un exemplaire. */
    public String text() {
        return quantity > 1 ? label() + " ×" + quantity : label();
    }

    private static String label(SrdWeapon weapon, SrdArmor armor) {
        return weapon != null ? weapon.label() : armor.label();
    }
}
