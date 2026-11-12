package dnd.engine.sheet;

/** Les dons de Style de combat du SRD 5.2.1 (« Fighting Style Feats »). */
public enum FightingStyle {
    ARCHERY("Archerie", "+2 aux jets d’attaque avec une arme à distance."),
    DEFENSE("Défense", "+1 à la CA en armure légère, intermédiaire ou lourde."),
    GREAT_WEAPON_FIGHTING("Armes à deux mains",
            "Avec une arme de corps à corps Deux mains ou Polyvalente tenue à deux mains, les 1 et 2 aux dés de dégâts comptent comme des 3."),
    TWO_WEAPON_FIGHTING("Combat à deux armes",
            "L’attaque supplémentaire d’une arme Légère ajoute le modificateur de caractéristique à ses dégâts.");

    private final String label;
    private final String text;

    FightingStyle(String label, String text) {
        this.label = label;
        this.text = text;
    }

    public String label() {
        return label;
    }

    /** Ce que fait le don, en une phrase. */
    public String text() {
        return text;
    }
}
