package dnd.engine.sheet;

import java.util.ArrayList;
import java.util.List;

import static dnd.engine.sheet.Item.armor;
import static dnd.engine.sheet.Item.named;
import static dnd.engine.sheet.Item.weapon;

/**
 * Les équipements de départ d'une classe (SRD 5.2.1), en objets : armure portée et armes tenues ; l'option tout en
 * pièces d'or n'a aucun objet. L'équipement A du Moine contient l'outil d'artisan ou l'instrument qu'il a choisi pour sa
 * maîtrise, après ses armes.
 */
public enum StartingEquipment {
    FIGHTER_A("Équipement A", CharacterClass.FIGHTER, false, 4, armor(SrdArmor.CHAIN_MAIL),
            weapon(SrdWeapon.GREATSWORD, 1), weapon(SrdWeapon.FLAIL, 1), weapon(SrdWeapon.JAVELIN, 8),
            named("Paquetage d’exploration souterraine", 1)),
    FIGHTER_B("Équipement B", CharacterClass.FIGHTER, false, 11, armor(SrdArmor.STUDDED_LEATHER),
            weapon(SrdWeapon.SCIMITAR, 1), weapon(SrdWeapon.SHORTSWORD, 1), weapon(SrdWeapon.LONGBOW, 1),
            named("Flèches", 20), named("Carquois", 1), named("Paquetage d’exploration souterraine", 1)),
    FIGHTER_C("Équipement C", CharacterClass.FIGHTER, false, 155),
    BARBARIAN_A("Équipement A", CharacterClass.BARBARIAN, false, 15,
            weapon(SrdWeapon.GREATAXE, 1), weapon(SrdWeapon.HANDAXE, 4), named("Paquetage d’explorateur", 1)),
    BARBARIAN_B("Équipement B", CharacterClass.BARBARIAN, false, 75),
    MONK_A("Équipement A", CharacterClass.MONK, true, 11,
            weapon(SrdWeapon.SPEAR, 1), weapon(SrdWeapon.DAGGER, 5), named("Paquetage d’explorateur", 1)),
    MONK_B("Équipement B", CharacterClass.MONK, false, 50),
    ROGUE_A("Équipement A", CharacterClass.ROGUE, false, 8, armor(SrdArmor.LEATHER),
            weapon(SrdWeapon.DAGGER, 2), weapon(SrdWeapon.SHORTSWORD, 1), weapon(SrdWeapon.SHORTBOW, 1),
            named("Flèches", 20), named("Carquois", 1), named("Outils de voleur", 1), named("Paquetage de cambrioleur", 1)),
    ROGUE_B("Équipement B", CharacterClass.ROGUE, false, 100);

    /** L'outil du Moine sur la fiche tant qu'il n'est pas choisi, dans l'inventaire comme dans les Maîtrises. */
    static final String TOOL_TO_CHOOSE = "outil d’artisan ou instrument de musique (à choisir)";
    /** L'outil du Moine dans l'option proposée, qui vaut pour tout outil choisi. */
    private static final String CHOSEN_TOOL = "l’outil d’artisan ou l’instrument de musique choisi";

    private final String label;
    private final CharacterClass characterClass;
    private final boolean classTool;
    private final int gold;
    private final List<Item> items;

    StartingEquipment(String label, CharacterClass characterClass, boolean classTool, int gold, Item... items) {
        this.label = label;
        this.characterClass = characterClass;
        this.classTool = classTool;
        this.gold = gold;
        this.items = List.of(items);
    }

    public String label() {
        return label;
    }

    public CharacterClass characterClass() {
        return characterClass;
    }

    /** Les armes de l'équipement, une fois chacune, dans l'ordre. */
    public List<SrdWeapon> weapons() {
        return items.stream().map(Item::weapon).filter(w -> w != null).toList();
    }

    /** Les pièces d'or. */
    public int gold() {
        return gold;
    }

    /**
     * L'inventaire de départ.
     *
     * @param tool l'outil choisi pour la maîtrise de la classe, s'il fait partie de l'équipement ; {@code null} : à choisir
     */
    public Inventory inventory(Tool tool) {
        return inventory(tool == null ? TOOL_TO_CHOOSE : tool.label());
    }

    /** Les objets puis les pièces d'or, tels que l'option se propose : l'outil de classe désigné sans être nommé. */
    public String text() {
        return inventory(CHOSEN_TOOL).text();
    }

    private Inventory inventory(String tool) {
        List<Item> all = new ArrayList<>(items);
        if (classTool) all.add((int) all.stream().filter(i -> i.weapon() != null || i.armor() != null).count(), Item.named(tool, 1));
        return new Inventory(all, gold);
    }
}
