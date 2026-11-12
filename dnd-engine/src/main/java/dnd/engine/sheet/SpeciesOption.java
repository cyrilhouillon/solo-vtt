package dnd.engine.sheet;

import dnd.engine.state.DamageType;

/**
 * Les options d'un trait d'espèce (SRD 5.2.1) : ascendance draconique, lignage elfique, lignage gnome, ascendance gigante,
 * héritage fiélon. Une option qui change la vitesse ou la Vision dans le noir la porte ; 0 sinon.
 */
public enum SpeciesOption {
    BRASS_DRAGON(Species.DRAGONBORN, "Airain", DamageType.FIRE),
    SILVER_DRAGON(Species.DRAGONBORN, "Argent", DamageType.COLD),
    WHITE_DRAGON(Species.DRAGONBORN, "Blanc", DamageType.COLD),
    BLUE_DRAGON(Species.DRAGONBORN, "Bleu", DamageType.LIGHTNING),
    BRONZE_DRAGON(Species.DRAGONBORN, "Bronze", DamageType.LIGHTNING),
    COPPER_DRAGON(Species.DRAGONBORN, "Cuivre", DamageType.ACID),
    BLACK_DRAGON(Species.DRAGONBORN, "Noir", DamageType.ACID),
    GOLD_DRAGON(Species.DRAGONBORN, "Or", DamageType.FIRE),
    RED_DRAGON(Species.DRAGONBORN, "Rouge", DamageType.FIRE),
    GREEN_DRAGON(Species.DRAGONBORN, "Vert", DamageType.POISON),

    DROW(Species.ELF, "Drow", "Vision dans le noir portée à 36 m ; sort mineur lumières dansantes", 0, 36),
    WOOD_ELF(Species.ELF, "Elfe sylvestre", "Vitesse portée à 10,50 m ; sort mineur druidisme", 10.5, 0),
    HIGH_ELF(Species.ELF, "Haut-elfe",
            "Sort mineur prestidigitation, remplaçable à chaque Repos long par un sort mineur de la liste du Magicien", 0, 0),

    FOREST_GNOME(Species.GNOME, "Gnome des forêts",
            "Sort mineur illusion mineure ; communication avec les animaux toujours préparé, lancé sans emplacement bonus "
                    + "de maîtrise fois par Repos long", 0, 0),
    ROCK_GNOME(Species.GNOME, "Gnome des roches",
            "Sorts mineurs prestidigitation et réparation ; en 10 minutes, un appareil mécanique (CA 5, 1 PV) qui produit un "
                    + "effet de prestidigitation, trois à la fois", 0, 0),

    FIRE_GIANT(Species.GOLIATH, "Brûlure ignée (géants du feu)",
            "Sur un coup qui inflige des dégâts : +1d10 dégâts de feu ; bonus de maîtrise fois par Repos long", 0, 0),
    STONE_GIANT(Species.GOLIATH, "Endurance de la pierre (géants des pierres)",
            "Réaction quand vous subissez des dégâts : les réduire de 1d12 + mod. Con ; bonus de maîtrise fois par Repos long", 0, 0),
    FROST_GIANT(Species.GOLIATH, "Froid mordant (géants du givre)",
            "Sur un coup qui inflige des dégâts : +1d6 dégâts de froid et −3 m de vitesse jusqu’à votre tour suivant ; bonus "
                    + "de maîtrise fois par Repos long", 0, 0),
    HILL_GIANT(Species.GOLIATH, "Renversement des coteaux (géants des collines)",
            "Sur un coup qui inflige des dégâts à une créature de taille G ou moins : elle tombe À terre ; bonus de maîtrise "
                    + "fois par Repos long", 0, 0),
    CLOUD_GIANT(Species.GOLIATH, "Saut des nuées (géants des nuages)",
            "Action Bonus : se téléporter jusqu’à 9 m dans un espace inoccupé visible ; bonus de maîtrise fois par Repos long", 0, 0),
    STORM_GIANT(Species.GOLIATH, "Tonnerre des cieux (géants des tempêtes)",
            "Réaction quand une créature à 18 m ou moins vous inflige des dégâts : 1d8 dégâts de tonnerre ; bonus de maîtrise "
                    + "fois par Repos long", 0, 0),

    ABYSSAL(Species.TIEFLING, "Abyssal", "Résistance aux dégâts de poison ; sort mineur bouffée de poison", 0, 0),
    CHTHONIC(Species.TIEFLING, "Chtonien", "Résistance aux dégâts nécrotiques ; sort mineur contact glacial", 0, 0),
    INFERNAL(Species.TIEFLING, "Infernal", "Résistance aux dégâts de feu ; sort mineur trait de feu", 0, 0);

    private final Species species;
    private final String label;
    private final String text;
    private final double speed;
    private final int darkvision;
    private final DamageType damageType;

    SpeciesOption(Species species, String label, String text, double speed, int darkvision) {
        this.species = species;
        this.label = label;
        this.text = text;
        this.speed = speed;
        this.darkvision = darkvision;
        this.damageType = null;
    }

    /** Une ascendance draconique : son type de dégâts fait la Résistance et le Souffle. */
    SpeciesOption(Species species, String label, DamageType damageType) {
        this.species = species;
        this.label = label;
        this.text = "Résistance aux dégâts %s ; Souffle : dégâts %s".formatted(damageType.label(), damageType.label());
        this.speed = 0;
        this.darkvision = 0;
        this.damageType = damageType;
    }

    public Species species() {
        return species;
    }

    public String label() {
        return label;
    }

    /** Ce que l'option donne au niveau 1, en une phrase. */
    public String text() {
        return text;
    }

    /** La vitesse en mètres que l'option donne, 0 si elle ne la change pas. */
    public double speed() {
        return speed;
    }

    /** La portée de Vision dans le noir que l'option donne, 0 si elle ne la change pas. */
    public int darkvision() {
        return darkvision;
    }

    /** Le type de dégâts d'une ascendance draconique, {@code null} pour les autres options. */
    public DamageType damageType() {
        return damageType;
    }
}
