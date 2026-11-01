package dnd.engine.state;

/**
 * Arme simplifiée. Dans un vrai moteur, attackBonus et damageBonus seraient calculés
 * (caractéristique + maîtrise), et les propriétés/maîtrises d'arme 2024 seraient des listeners.
 */
public record Weapon(String name, int diceCount, int dieSides, DamageType damageType,
                     int attackBonus, int damageBonus, boolean oneHandedMelee) {

    public String diceNotation() {
        return diceCount + "d" + dieSides;
    }
}
