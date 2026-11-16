package dnd.engine.state;

/**
 * Arme simplifiée. Dans un vrai moteur, attackBonus et damageBonus seraient calculés
 * (caractéristique + maîtrise), et les propriétés/maîtrises d'arme 2024 seraient des listeners.
 *
 * <p>Longueurs en mètres. {@code reach} n'est nul que pour une arme qui ne sert pas au corps à corps
 * (arc) ; {@code range} n'est nul que pour une arme qui ne s'utilise pas à distance. Une arme de
 * corps à corps qui a une portée est une arme de lancer (propriété Thrown du SRD).</p>
 *
 * @param oneHandedMelee l'arme se tient à une main pour le corps à corps (style Duel) ;
 *                       faux pour une arme naturelle comme la Morsure.
 * @param id             ce qui distingue l'arme parmi celles d'une créature quand son nom ne suffit pas (une attaque
 *                       saisie sur une fiche, qui peut porter le nom d'une arme tenue) ; {@code null} : son nom.
 */
public record Weapon(String name, int diceCount, int dieSides, DamageType damageType,
                     int attackBonus, int damageBonus, boolean oneHandedMelee,
                     Double reach, Range range, String id) {

    /** Allonge d'une créature : 5 ft, soit 1,5 m (SRD 5.2.1, « Reach »). */
    public static final double STANDARD_REACH = 1.5;

    /** Portée normale et portée longue, en mètres. */
    public record Range(double normal, double longRange) {
        public Range {
            if (!(normal > 0) || longRange < normal) {
                throw new IllegalArgumentException("Portée invalide : %s/%s".formatted(normal, longRange));
            }
        }
    }

    public Weapon {
        if (reach == null && range == null) {
            throw new IllegalArgumentException(name + " n'a ni allonge ni portée");
        }
    }

    /** Arme identifiée par son nom. */
    public Weapon(String name, int diceCount, int dieSides, DamageType damageType,
                  int attackBonus, int damageBonus, boolean oneHandedMelee, Double reach, Range range) {
        this(name, diceCount, dieSides, damageType, attackBonus, damageBonus, oneHandedMelee, reach, range, null);
    }

    /** Arme de corps à corps, à l'allonge standard. */
    public Weapon(String name, int diceCount, int dieSides, DamageType damageType,
                  int attackBonus, int damageBonus, boolean oneHandedMelee) {
        this(name, diceCount, dieSides, damageType, attackBonus, damageBonus, oneHandedMelee, STANDARD_REACH, null);
    }

    /** Arme qui ne sert qu'à distance (arc, arbalète). */
    public static Weapon ranged(String name, int diceCount, int dieSides, DamageType damageType,
                                int attackBonus, int damageBonus, double normal, double longRange) {
        return new Weapon(name, diceCount, dieSides, damageType, attackBonus, damageBonus, false,
                null, new Range(normal, longRange));
    }

    /** La même arme, qui peut aussi être lancée. */
    public Weapon thrown(double normal, double longRange) {
        return new Weapon(name, diceCount, dieSides, damageType, attackBonus, damageBonus, oneHandedMelee,
                reach, new Range(normal, longRange), id);
    }

    /** Ce qui identifie l'arme dans les attaques d'une créature : son {@code id}, sinon son nom. */
    public String key() {
        return id != null ? id : name;
    }

    public boolean isMelee() {
        return reach != null;
    }

    public String diceNotation() {
        return diceCount + "d" + dieSides;
    }
}
