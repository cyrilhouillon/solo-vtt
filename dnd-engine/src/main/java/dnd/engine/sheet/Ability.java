package dnd.engine.sheet;

/** Les six caractéristiques d'une créature (SRD 5.2.1, « Ability Scores »). */
public enum Ability {
    STRENGTH("FOR", "Force"),
    DEXTERITY("DEX", "Dextérité"),
    CONSTITUTION("CON", "Constitution"),
    INTELLIGENCE("INT", "Intelligence"),
    WISDOM("SAG", "Sagesse"),
    CHARISMA("CHA", "Charisme");

    private final String abbreviation;
    private final String label;

    Ability(String abbreviation, String label) {
        this.abbreviation = abbreviation;
        this.label = label;
    }

    public String abbreviation() {
        return abbreviation;
    }

    public String label() {
        return label;
    }
}
