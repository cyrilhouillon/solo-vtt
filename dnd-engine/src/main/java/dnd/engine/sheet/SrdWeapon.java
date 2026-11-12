package dnd.engine.sheet;

import dnd.engine.state.DamageType;

import java.util.EnumSet;
import java.util.Set;

import static dnd.engine.sheet.SrdWeapon.Category.MARTIAL_MELEE;
import static dnd.engine.sheet.SrdWeapon.Category.MARTIAL_RANGED;
import static dnd.engine.sheet.SrdWeapon.Category.SIMPLE_MELEE;
import static dnd.engine.sheet.SrdWeapon.Category.SIMPLE_RANGED;
import static dnd.engine.sheet.SrdWeapon.Property.AMMUNITION;
import static dnd.engine.sheet.SrdWeapon.Property.FINESSE;
import static dnd.engine.sheet.SrdWeapon.Property.HEAVY;
import static dnd.engine.sheet.SrdWeapon.Property.LIGHT;
import static dnd.engine.sheet.SrdWeapon.Property.LOADING;
import static dnd.engine.sheet.SrdWeapon.Property.REACH;
import static dnd.engine.sheet.SrdWeapon.Property.THROWN;
import static dnd.engine.sheet.SrdWeapon.Property.TWO_HANDED;
import static dnd.engine.sheet.SrdWeapon.Property.VERSATILE;
import static dnd.engine.state.DamageType.BLUDGEONING;
import static dnd.engine.state.DamageType.PIERCING;
import static dnd.engine.state.DamageType.SLASHING;

/**
 * Les 38 armes du SRD 5.2.1 (« Weapons ») : catégorie, dégâts, propriétés, portée en mètres (Lancer ou Munitions, 0
 * sinon) et botte d'arme.
 */
public enum SrdWeapon {
    CLUB("Gourdin", SIMPLE_MELEE, "1d4", BLUDGEONING, "Ralentissement", 0, 0, LIGHT),
    DAGGER("Dague", SIMPLE_MELEE, "1d4", PIERCING, "Coup double", 6, 18, FINESSE, THROWN, LIGHT),
    GREATCLUB("Massue", SIMPLE_MELEE, "1d8", BLUDGEONING, "Poussée", 0, 0, TWO_HANDED),
    HANDAXE("Hachette", SIMPLE_MELEE, "1d6", SLASHING, "Ouverture", 6, 18, THROWN, LIGHT),
    JAVELIN("Javeline", SIMPLE_MELEE, "1d6", PIERCING, "Ralentissement", 9, 36, THROWN),
    LIGHT_HAMMER("Marteau léger", SIMPLE_MELEE, "1d4", BLUDGEONING, "Coup double", 6, 18, THROWN, LIGHT),
    MACE("Masse d’armes", SIMPLE_MELEE, "1d6", BLUDGEONING, "Sape", 0, 0),
    QUARTERSTAFF("Bâton de combat", SIMPLE_MELEE, "1d6", BLUDGEONING, "Renversement", 0, 0, VERSATILE),
    SICKLE("Serpe", SIMPLE_MELEE, "1d4", SLASHING, "Coup double", 0, 0, LIGHT),
    SPEAR("Lance", SIMPLE_MELEE, "1d6", PIERCING, "Sape", 6, 18, THROWN, VERSATILE),
    DART("Fléchette", SIMPLE_RANGED, "1d4", PIERCING, "Ouverture", 6, 18, FINESSE, THROWN),
    LIGHT_CROSSBOW("Arbalète légère", SIMPLE_RANGED, "1d8", PIERCING, "Ralentissement", 24, 96, AMMUNITION, LOADING, TWO_HANDED),
    SHORTBOW("Arc court", SIMPLE_RANGED, "1d6", PIERCING, "Ouverture", 24, 96, AMMUNITION, TWO_HANDED),
    SLING("Fronde", SIMPLE_RANGED, "1d4", BLUDGEONING, "Ralentissement", 9, 36, AMMUNITION),
    BATTLEAXE("Hache d’armes", MARTIAL_MELEE, "1d8", SLASHING, "Renversement", 0, 0, VERSATILE),
    FLAIL("Fléau d’armes", MARTIAL_MELEE, "1d8", BLUDGEONING, "Sape", 0, 0),
    GLAIVE("Coutille", MARTIAL_MELEE, "1d10", SLASHING, "Écorchure", 0, 0, HEAVY, REACH, TWO_HANDED),
    GREATAXE("Hache à deux mains", MARTIAL_MELEE, "1d12", SLASHING, "Enchaînement", 0, 0, HEAVY, TWO_HANDED),
    GREATSWORD("Épée à deux mains", MARTIAL_MELEE, "2d6", SLASHING, "Écorchure", 0, 0, HEAVY, TWO_HANDED),
    HALBERD("Hallebarde", MARTIAL_MELEE, "1d10", SLASHING, "Enchaînement", 0, 0, HEAVY, REACH, TWO_HANDED),
    LANCE("Lance d’arçon", MARTIAL_MELEE, "1d10", PIERCING, "Renversement", 0, 0, HEAVY, REACH, TWO_HANDED),
    LONGSWORD("Épée longue", MARTIAL_MELEE, "1d8", SLASHING, "Sape", 0, 0, VERSATILE),
    MAUL("Maillet d’armes", MARTIAL_MELEE, "2d6", BLUDGEONING, "Renversement", 0, 0, HEAVY, TWO_HANDED),
    MORNINGSTAR("Morgenstern", MARTIAL_MELEE, "1d8", PIERCING, "Sape", 0, 0),
    PIKE("Pique", MARTIAL_MELEE, "1d10", PIERCING, "Poussée", 0, 0, HEAVY, REACH, TWO_HANDED),
    RAPIER("Rapière", MARTIAL_MELEE, "1d8", PIERCING, "Ouverture", 0, 0, FINESSE),
    SCIMITAR("Cimeterre", MARTIAL_MELEE, "1d6", SLASHING, "Coup double", 0, 0, FINESSE, LIGHT),
    SHORTSWORD("Épée courte", MARTIAL_MELEE, "1d6", PIERCING, "Ouverture", 0, 0, FINESSE, LIGHT),
    TRIDENT("Trident", MARTIAL_MELEE, "1d8", PIERCING, "Renversement", 6, 18, THROWN, VERSATILE),
    WARHAMMER("Marteau de guerre", MARTIAL_MELEE, "1d8", BLUDGEONING, "Poussée", 0, 0, VERSATILE),
    WAR_PICK("Pic de guerre", MARTIAL_MELEE, "1d8", PIERCING, "Sape", 0, 0, VERSATILE),
    WHIP("Fouet", MARTIAL_MELEE, "1d4", SLASHING, "Ralentissement", 0, 0, FINESSE, REACH),
    BLOWGUN("Sarbacane", MARTIAL_RANGED, "1", PIERCING, "Ouverture", 7.5, 30, AMMUNITION, LOADING),
    HAND_CROSSBOW("Arbalète de poing", MARTIAL_RANGED, "1d6", PIERCING, "Ouverture", 9, 36, AMMUNITION, LIGHT, LOADING),
    HEAVY_CROSSBOW("Arbalète lourde", MARTIAL_RANGED, "1d10", PIERCING, "Poussée", 30, 120, AMMUNITION, HEAVY, LOADING, TWO_HANDED),
    LONGBOW("Arc long", MARTIAL_RANGED, "1d8", PIERCING, "Ralentissement", 45, 180, AMMUNITION, HEAVY, TWO_HANDED),
    MUSKET("Mousquet", MARTIAL_RANGED, "1d12", PIERCING, "Ralentissement", 12, 36, AMMUNITION, LOADING, TWO_HANDED),
    PISTOL("Pistolet", MARTIAL_RANGED, "1d10", PIERCING, "Ouverture", 9, 27, AMMUNITION, LOADING);

    /** Armes courantes ou de guerre, de corps à corps ou à distance. */
    public enum Category {
        SIMPLE_MELEE("Armes courantes de corps à corps", false),
        SIMPLE_RANGED("Armes courantes à distance", true),
        MARTIAL_MELEE("Armes de guerre de corps à corps", false),
        MARTIAL_RANGED("Armes de guerre à distance", true);

        private final String label;
        private final boolean ranged;

        Category(String label, boolean ranged) {
            this.label = label;
            this.ranged = ranged;
        }

        public String label() {
            return label;
        }

        public boolean ranged() {
            return ranged;
        }
    }

    /** Les propriétés d'arme (SRD 5.2.1, « Weapon Properties »). */
    public enum Property {
        AMMUNITION("Munitions"),
        FINESSE("Finesse"),
        HEAVY("Lourde"),
        LIGHT("Légère"),
        LOADING("Chargement"),
        REACH("Allonge"),
        THROWN("Lancer"),
        TWO_HANDED("Deux mains"),
        VERSATILE("Polyvalente");

        private final String label;

        Property(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final String label;
    private final Category category;
    private final int diceCount;
    private final int dieSides;
    private final int flatDamage;
    private final DamageType damageType;
    private final String mastery;
    private final double normalRange;
    private final double longRange;
    private final Set<Property> properties;

    SrdWeapon(String label, Category category, String damage, DamageType damageType, String mastery,
              double normalRange, double longRange, Property... properties) {
        this.label = label;
        this.category = category;
        int d = damage.indexOf('d');
        this.diceCount = d < 0 ? 0 : Integer.parseInt(damage.substring(0, d));
        this.dieSides = d < 0 ? 4 : Integer.parseInt(damage.substring(d + 1));
        this.flatDamage = d < 0 ? Integer.parseInt(damage) : 0;
        this.damageType = damageType;
        this.mastery = mastery;
        this.normalRange = normalRange;
        this.longRange = longRange;
        this.properties = properties.length == 0 ? Set.of() : Set.copyOf(EnumSet.of(properties[0], properties));
    }

    public String label() {
        return label;
    }

    public Category category() {
        return category;
    }

    /** Les dés de dégâts, sans modificateur (« 2d6 », ou « 1 » pour la sarbacane). */
    public String damage() {
        return diceCount == 0 ? String.valueOf(flatDamage) : diceCount + "d" + dieSides;
    }

    /** Le nombre de dés de dégâts, 0 pour des dégâts fixes (sarbacane). */
    public int diceCount() {
        return diceCount;
    }

    /** Les faces d'un dé de dégâts ; sans dé, un d4 qui n'est jamais lancé. */
    public int dieSides() {
        return dieSides;
    }

    /** Les dégâts fixes d'une arme sans dé (1 pour la sarbacane), 0 sinon. */
    public int flatDamage() {
        return flatDamage;
    }

    public DamageType damageType() {
        return damageType;
    }

    /** Le nom de la botte d'arme. */
    public String mastery() {
        return mastery;
    }

    /** Portée normale en mètres, 0 sans Lancer ni Munitions. */
    public double normalRange() {
        return normalRange;
    }

    /** Portée longue en mètres, 0 sans Lancer ni Munitions. */
    public double longRange() {
        return longRange;
    }

    public Set<Property> properties() {
        return properties;
    }

    public boolean has(Property property) {
        return properties.contains(property);
    }
}
