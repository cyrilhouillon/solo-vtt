package dnd.engine.sheet;

/** Les dix-huit compétences et la caractéristique dont chacune dépend (SRD 5.2.1, « Skills »). */
public enum Skill {
    ACROBATICS("Acrobaties", Ability.DEXTERITY),
    ARCANA("Arcanes", Ability.INTELLIGENCE),
    ATHLETICS("Athlétisme", Ability.STRENGTH),
    STEALTH("Discrétion", Ability.DEXTERITY),
    ANIMAL_HANDLING("Dressage", Ability.WISDOM),
    SLEIGHT_OF_HAND("Escamotage", Ability.DEXTERITY),
    HISTORY("Histoire", Ability.INTELLIGENCE),
    INTIMIDATION("Intimidation", Ability.CHARISMA),
    INSIGHT("Intuition", Ability.WISDOM),
    INVESTIGATION("Investigation", Ability.INTELLIGENCE),
    MEDICINE("Médecine", Ability.WISDOM),
    NATURE("Nature", Ability.INTELLIGENCE),
    PERCEPTION("Perception", Ability.WISDOM),
    PERSUASION("Persuasion", Ability.CHARISMA),
    RELIGION("Religion", Ability.INTELLIGENCE),
    PERFORMANCE("Représentation", Ability.CHARISMA),
    SURVIVAL("Survie", Ability.WISDOM),
    DECEPTION("Tromperie", Ability.CHARISMA);

    private final String label;
    private final Ability ability;

    Skill(String label, Ability ability) {
        this.label = label;
        this.ability = ability;
    }

    public String label() {
        return label;
    }

    public Ability ability() {
        return ability;
    }
}
