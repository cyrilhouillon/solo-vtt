package dnd.engine.sheet;

import dnd.engine.state.DamageType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * La CA et les attaques d'une fiche, calculées de ce qu'elle porte (SRD 5.2.1, niveau 1) : sa classe, son Style de combat,
 * ses caractéristiques, son bonus de maîtrise, son inventaire (armure portée, bouclier et armes tenus), puis son ajustement
 * de CA et ses autres attaques saisies. Rien n'est gardé : la même fiche donne toujours les mêmes valeurs.
 */
public final class CombatStats {

    static final String UNARMED_STRIKE = "Attaque à mains nues";
    private static final int DEFENSE_BONUS = 1;
    private static final int ARCHERY_BONUS = 2;
    private static final double MELEE_REACH = 1.5;
    private static final double REACH_WEAPON = 3;

    private CombatStats() {
    }

    /**
     * L'armure portée (Dextérité selon sa catégorie, + Défense) ; sans armure, la meilleure de 10 + Dex et de la Défense
     * sans armure de la classe (si le bouclier tenu la permet) ; puis le bouclier, seulement avec la formation ; puis
     * l'ajustement saisi.
     */
    static int armorClass(CharacterSheet sheet) {
        CharacterClass characterClass = sheet.characterClass();
        Inventory inventory = sheet.inventory();
        SrdArmor armor = inventory.armor();
        boolean shield = inventory.shield();
        int dex = sheet.modifier(Ability.DEXTERITY);
        int armorClass;
        if (armor != null) {
            armorClass = armor.armorClass(dex) + (sheet.fightingStyle() == FightingStyle.DEFENSE ? DEFENSE_BONUS : 0);
        } else {
            armorClass = 10 + dex;
            Ability unarmored = characterClass.unarmoredDefense();
            if (unarmored != null && (!shield || characterClass.unarmoredDefenseWithShield())) {
                armorClass = Math.max(armorClass, 10 + dex + sheet.modifier(unarmored));
            }
        }
        if (shield && characterClass.trained(SrdArmor.SHIELD)) armorClass += SrdArmor.SHIELD.armorClass();
        return armorClass + sheet.armorBonus();
    }

    /** Une attaque par arme tenue, dans l'ordre de l'inventaire, puis l'attaque à mains nues, puis les autres attaques. */
    static List<SheetAttack> attacks(CharacterSheet sheet) {
        CharacterClass characterClass = sheet.characterClass();
        int str = sheet.modifier(Ability.STRENGTH);
        int dex = sheet.modifier(Ability.DEXTERITY);
        int proficiency = sheet.proficiencyBonus();
        boolean martialArts = martialArts(characterClass, sheet.inventory());
        List<SheetAttack> attacks = new ArrayList<>();
        for (SrdWeapon weapon : sheet.inventory().weapons()) {
            boolean monkWeapon = martialArts && CharacterClass.monkWeapon(weapon);
            attacks.add(attack(weapon, str, dex, characterClass.proficient(weapon) ? proficiency : 0,
                    sheet.fightingStyle(), monkWeapon));
        }
        attacks.add(unarmedStrike(str, dex, proficiency, martialArts));
        attacks.addAll(sheet.otherAttacks());
        return attacks;
    }

    /** Les noms des attaques calculées, qu'une autre attaque ne peut pas reprendre ; aucun sans classe. */
    static Set<String> derivedAttackNames(CharacterClass characterClass, Inventory inventory) {
        Set<String> names = new LinkedHashSet<>();
        if (characterClass == null) return names;
        inventory.weapons().forEach(weapon -> names.add(weapon.label()));
        names.add(UNARMED_STRIKE);
        return names;
    }

    /** Arts martiaux ne tient que sans armure ni bouclier (SRD 5.2.1, « Martial Arts »). */
    private static boolean martialArts(CharacterClass characterClass, Inventory inventory) {
        return characterClass.martialArts() && inventory.armor() == null && !inventory.shield();
    }

    /**
     * Maîtrise (si la classe maîtrise l'arme) + Force, Dextérité à distance, la meilleure en Finesse ; + 2 en Archerie avec
     * une arme à distance. Une arme de Moine, avec Arts martiaux, prend la meilleure de Force et Dextérité, et le dé
     * d'Arts martiaux s'il est plus fort que le sien. Allonge 1,5 m (3 m pour Allonge) au corps à corps, aucune à distance ;
     * portée pour Lancer et Munitions.
     *
     * @param proficiency le bonus de maîtrise, 0 pour une arme que la classe ne maîtrise pas
     */
    static SheetAttack attack(SrdWeapon weapon, int str, int dex, int proficiency, FightingStyle style, boolean martialArts) {
        boolean ranged = weapon.category().ranged();
        int ability = martialArts || weapon.has(SrdWeapon.Property.FINESSE) ? Math.max(str, dex) : ranged ? dex : str;
        int bonus = proficiency + ability + (ranged && style == FightingStyle.ARCHERY ? ARCHERY_BONUS : 0);
        Double reach = ranged ? null : weapon.has(SrdWeapon.Property.REACH) ? REACH_WEAPON : MELEE_REACH;
        boolean hasRange = weapon.normalRange() > 0;
        int diceCount = weapon.diceCount();
        int dieSides = weapon.dieSides();
        // Le dé le plus fort en moyenne : n × (faces + 1) / 2 contre (d + 1) / 2.
        if (martialArts && diceCount * (dieSides + 1) < CharacterClass.MARTIAL_ARTS_DIE + 1) {
            diceCount = 1;
            dieSides = CharacterClass.MARTIAL_ARTS_DIE;
        }
        // Des dégâts fixes, sans dé (sarbacane), ne prennent pas le modificateur (SRD 5.2.1 p. 16, « Damage Rolls »).
        int damageBonus = diceCount == 0 ? weapon.flatDamage() : ability;
        return new SheetAttack(weapon.label(), bonus, diceCount, dieSides, damageBonus,
                weapon.damageType(), reach, hasRange ? weapon.normalRange() : null, hasRange ? weapon.longRange() : null);
    }

    /**
     * L'attaque à mains nues de toute créature (SRD 5.2.1, « Unarmed Strike ») : Force + maîtrise, 1 + Force contondants ;
     * avec Arts martiaux, la meilleure de Force et Dextérité au toucher et aux dégâts, et le dé d'Arts martiaux.
     */
    static SheetAttack unarmedStrike(int str, int dex, int proficiency, boolean martialArts) {
        if (martialArts) {
            int ability = Math.max(str, dex);
            return new SheetAttack(UNARMED_STRIKE, proficiency + ability, 1, CharacterClass.MARTIAL_ARTS_DIE, ability,
                    DamageType.BLUDGEONING, MELEE_REACH, null, null);
        }
        return new SheetAttack(UNARMED_STRIKE, proficiency + str, 0, 4, 1 + str, DamageType.BLUDGEONING,
                MELEE_REACH, null, null);
    }
}
