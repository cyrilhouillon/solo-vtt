package dnd.engine.sheet;

import java.util.List;

/** Les équipements de départ d'une classe (SRD 5.2.1) ; l'option tout en pièces d'or n'est pas proposée. */
public enum StartingEquipment {
    FIGHTER_A("Équipement A", CharacterClass.FIGHTER, SrdArmor.CHAIN_MAIL, false,
            List.of(SrdWeapon.GREATSWORD, SrdWeapon.FLAIL, SrdWeapon.JAVELIN),
            "Cotte de mailles, épée à deux mains, fléau d’armes, 8 javelines, paquetage d’exploration souterraine", 4),
    FIGHTER_B("Équipement B", CharacterClass.FIGHTER, SrdArmor.STUDDED_LEATHER, false,
            List.of(SrdWeapon.SCIMITAR, SrdWeapon.SHORTSWORD, SrdWeapon.LONGBOW),
            "Armure de cuir clouté, cimeterre, épée courte, arc long, 20 flèches, carquois, paquetage d’exploration souterraine", 11);

    private final String label;
    private final CharacterClass characterClass;
    private final SrdArmor armor;
    private final boolean shield;
    private final List<SrdWeapon> weapons;
    private final String items;
    private final int gold;

    StartingEquipment(String label, CharacterClass characterClass, SrdArmor armor, boolean shield,
                      List<SrdWeapon> weapons, String items, int gold) {
        this.label = label;
        this.characterClass = characterClass;
        this.armor = armor;
        this.shield = shield;
        this.weapons = weapons;
        this.items = items;
        this.gold = gold;
    }

    public String label() {
        return label;
    }

    public CharacterClass characterClass() {
        return characterClass;
    }

    /** L'armure portée, {@code null} sans armure. */
    public SrdArmor armor() {
        return armor;
    }

    /** Un bouclier fait-il partie de l'équipement. */
    public boolean shield() {
        return shield;
    }

    public List<SrdWeapon> weapons() {
        return weapons;
    }

    /** Les objets, sans les pièces d'or. */
    public String items() {
        return items;
    }

    /** Les pièces d'or. */
    public int gold() {
        return gold;
    }

    /** Les objets puis les pièces d'or, tels qu'ils s'écrivent sur la fiche. */
    public String text() {
        return items + ", " + gold + " po";
    }
}
