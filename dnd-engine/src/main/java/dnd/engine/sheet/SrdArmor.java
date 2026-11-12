package dnd.engine.sheet;

/** Les armures de départ du Guerrier (SRD 5.2.1, « Armor ») et ce qu'elles donnent à la CA. */
public enum SrdArmor {
    CHAIN_MAIL("Cotte de mailles", Category.HEAVY, 16),
    STUDDED_LEATHER("Armure de cuir clouté", Category.LIGHT, 12),
    SHIELD("Bouclier", Category.SHIELD, 2);

    /** La catégorie décide de la part de Dextérité : entière en légère, 2 au plus en intermédiaire, aucune en lourde. */
    public enum Category {
        LIGHT, MEDIUM, HEAVY, SHIELD
    }

    private final String label;
    private final Category category;
    private final int armorClass;

    SrdArmor(String label, Category category, int armorClass) {
        this.label = label;
        this.category = category;
        this.armorClass = armorClass;
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

    /** La CA que donne l'armure portée, avec la part de Dextérité de sa catégorie ; pour un bouclier, son bonus. */
    public int armorClass(int dexterityModifier) {
        return switch (category) {
            case LIGHT -> armorClass + dexterityModifier;
            case MEDIUM -> armorClass + Math.min(dexterityModifier, 2);
            case HEAVY, SHIELD -> armorClass;
        };
    }
}
