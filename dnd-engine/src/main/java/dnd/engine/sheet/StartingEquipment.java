package dnd.engine.sheet;

import java.util.List;

/**
 * Les équipements de départ d'une classe (SRD 5.2.1), option tout en pièces d'or comprise : ni armure ni arme, seulement
 * des po. L'équipement du Moine contient l'outil d'artisan ou l'instrument qu'il a choisi pour sa maîtrise.
 */
public enum StartingEquipment {
    FIGHTER_A("Équipement A", CharacterClass.FIGHTER, SrdArmor.CHAIN_MAIL, false,
            List.of(SrdWeapon.GREATSWORD, SrdWeapon.FLAIL, SrdWeapon.JAVELIN),
            "Cotte de mailles, épée à deux mains, fléau d’armes, 8 javelines, paquetage d’exploration souterraine", 4),
    FIGHTER_B("Équipement B", CharacterClass.FIGHTER, SrdArmor.STUDDED_LEATHER, false,
            List.of(SrdWeapon.SCIMITAR, SrdWeapon.SHORTSWORD, SrdWeapon.LONGBOW),
            "Armure de cuir clouté, cimeterre, épée courte, arc long, 20 flèches, carquois, paquetage d’exploration souterraine", 11),
    FIGHTER_C("Équipement C", CharacterClass.FIGHTER, null, false, List.of(), "", 155),
    BARBARIAN_A("Équipement A", CharacterClass.BARBARIAN, null, false,
            List.of(SrdWeapon.GREATAXE, SrdWeapon.HANDAXE),
            "Hache à deux mains, 4 hachettes, paquetage d’explorateur", 15),
    BARBARIAN_B("Équipement B", CharacterClass.BARBARIAN, null, false, List.of(), "", 75),
    MONK_A("Équipement A", CharacterClass.MONK, null, false,
            List.of(SrdWeapon.SPEAR, SrdWeapon.DAGGER),
            "Lance, 5 dagues, %s, paquetage d’explorateur", 11),
    MONK_B("Équipement B", CharacterClass.MONK, null, false, List.of(), "", 50),
    ROGUE_A("Équipement A", CharacterClass.ROGUE, SrdArmor.LEATHER, false,
            List.of(SrdWeapon.DAGGER, SrdWeapon.SHORTSWORD, SrdWeapon.SHORTBOW),
            "Armure de cuir, 2 dagues, épée courte, arc court, 20 flèches, carquois, outils de voleur, paquetage de cambrioleur", 8),
    ROGUE_B("Équipement B", CharacterClass.ROGUE, null, false, List.of(), "", 100);

    /** L'outil du Moine sur la fiche tant qu'il n'est pas choisi, dans l'Équipement comme dans les Maîtrises. */
    static final String TOOL_TO_CHOOSE = "outil d’artisan ou instrument de musique (à choisir)";
    /** L'outil du Moine dans l'option proposée, qui vaut pour tout outil choisi. */
    private static final String CHOSEN_TOOL = "l’outil d’artisan ou l’instrument de musique choisi";

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

    /** Les objets, sans les pièces d'or ; vide pour l'option tout en pièces d'or. */
    public String items() {
        return items;
    }

    /** Les pièces d'or. */
    public int gold() {
        return gold;
    }

    /** Les objets puis les pièces d'or, tels que l'option se propose : l'outil de classe désigné sans être nommé. */
    public String text() {
        return format(CHOSEN_TOOL);
    }

    /**
     * Les objets puis les pièces d'or, tels qu'ils s'écrivent sur la fiche.
     *
     * @param tool l'outil choisi pour la maîtrise de la classe, s'il fait partie de l'équipement ; {@code null} : à choisir
     */
    public String text(Tool tool) {
        return format(tool == null ? TOOL_TO_CHOOSE : tool.label());
    }

    private String format(String tool) {
        if (items.isEmpty()) return gold + " po";
        String objects = items.contains("%s") ? items.formatted(tool) : items;
        return objects + ", " + gold + " po";
    }
}
