package dnd.engine.sheet;

/**
 * Les 12 armures et le bouclier du SRD 5.2.1 (« Armor ») : ce qu'ils donnent à la CA, la Force requise (0 sans : en
 * dessous, la vitesse baisse de 3 m) et le Désavantage en Discrétion. Force et Discrétion ne sont pas jouées.
 */
public enum SrdArmor {
    PADDED("Armure matelassée", Category.LIGHT, 11, 0, true),
    LEATHER("Armure de cuir", Category.LIGHT, 11, 0, false),
    STUDDED_LEATHER("Armure de cuir clouté", Category.LIGHT, 12, 0, false),
    HIDE("Armure de peaux", Category.MEDIUM, 12, 0, false),
    CHAIN_SHIRT("Chemise de mailles", Category.MEDIUM, 13, 0, false),
    SCALE_MAIL("Armure d’écailles", Category.MEDIUM, 14, 0, true),
    BREASTPLATE("Cuirasse", Category.MEDIUM, 14, 0, false),
    HALF_PLATE("Demi-plate", Category.MEDIUM, 15, 0, true),
    RING_MAIL("Broigne", Category.HEAVY, 14, 0, true),
    CHAIN_MAIL("Cotte de mailles", Category.HEAVY, 16, 13, true),
    SPLINT("Clibanion", Category.HEAVY, 17, 15, true),
    PLATE("Harnois", Category.HEAVY, 18, 15, true),
    SHIELD("Bouclier", Category.SHIELD, 2, 0, false);

    /** La catégorie décide de la part de Dextérité : entière en légère, 2 au plus en intermédiaire, aucune en lourde. */
    public enum Category {
        LIGHT, MEDIUM, HEAVY, SHIELD
    }

    private final String label;
    private final Category category;
    private final int armorClass;
    private final int strength;
    private final boolean stealthDisadvantage;

    SrdArmor(String label, Category category, int armorClass, int strength, boolean stealthDisadvantage) {
        this.label = label;
        this.category = category;
        this.armorClass = armorClass;
        this.strength = strength;
        this.stealthDisadvantage = stealthDisadvantage;
    }

    public String label() {
        return label;
    }

    public Category category() {
        return category;
    }

    /** La CA de base d'une armure, ou le bonus d'un bouclier. */
    public int armorClass() {
        return armorClass;
    }

    /** La valeur de Force requise, 0 sans. */
    public int strength() {
        return strength;
    }

    /** L'armure donne-t-elle le Désavantage en Discrétion. */
    public boolean stealthDisadvantage() {
        return stealthDisadvantage;
    }

    /** La CA que donne l'armure portée, avec la part de Dextérité de sa catégorie ; pour un bouclier, son bonus. */
    public int armorClass(int dexterityModifier) {
        return switch (category) {
            case LIGHT -> armorClass + dexterityModifier;
            case MEDIUM -> armorClass + Math.min(dexterityModifier, 2);
            case HEAVY, SHIELD -> armorClass;
        };
    }
}
