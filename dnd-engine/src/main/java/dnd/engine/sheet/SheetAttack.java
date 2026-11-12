package dnd.engine.sheet;

import dnd.engine.state.DamageType;
import dnd.engine.state.Weapon;

import java.util.Set;

/**
 * Une attaque de la fiche, en données que le moteur joue : bonus d'attaque, dés et bonus de dégâts, type de dégâts, et
 * allonge et/ou portée en mètres. Une attaque qui a les deux est une arme de lancer ; {@code null} veut dire « sans ».
 *
 * @param diceCount 0 pour des dégâts fixes (sarbacane), portés par {@code damageBonus}
 */
public record SheetAttack(String name, int bonus, int diceCount, int dieSides, int damageBonus, DamageType damageType,
                          Double reach, Double normalRange, Double longRange) {

    public static final int MAX_DICE = 20;
    public static final Set<Integer> DIE_SIDES = Set.of(4, 6, 8, 10, 12, 20);

    public SheetAttack {
        name = CharacterSheet.text(name);
        if (name.isEmpty()) throw new IllegalArgumentException("Une attaque a un nom");
        if (diceCount < 0 || diceCount > MAX_DICE) {
            throw new IllegalArgumentException("%s : entre 0 et %d dés de dégâts".formatted(name, MAX_DICE));
        }
        if (!DIE_SIDES.contains(dieSides)) throw new IllegalArgumentException("%s : dé de dégâts d%d inconnu".formatted(name, dieSides));
        if (damageType == null) throw new IllegalArgumentException(name + " : choisir un type de dégâts");
        if (reach == null && normalRange == null && longRange == null) {
            throw new IllegalArgumentException(name + " : une allonge ou une portée");
        }
        if (reach != null && !(reach > 0)) throw new IllegalArgumentException(name + " : allonge positive");
        if ((normalRange == null) != (longRange == null)) {
            throw new IllegalArgumentException(name + " : portée normale et portée longue vont ensemble");
        }
        if (normalRange != null && (!(normalRange > 0) || longRange < normalRange)) {
            throw new IllegalArgumentException(name + " : portée normale positive, portée longue au moins égale");
        }
    }

    /** L'arme que joue le moteur. Tenue à une main n'est pas noté sur la fiche : le style Duel ne s'y applique pas. */
    public Weapon weapon() {
        return new Weapon(name, diceCount, dieSides, damageType, bonus, damageBonus, false, reach,
                normalRange == null ? null : new Weapon.Range(normalRange, longRange));
    }
}
