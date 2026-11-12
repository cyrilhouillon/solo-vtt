package dnd.engine.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Ce qui fait une créature avant qu'elle ne joue : de quoi la recréer à l'identique, sans le code qui l'a décrite. Un PJ
 * posé en combat est gardé sous cette forme, copie de sa fiche à la pose.
 *
 * @param speed       en mètres
 * @param spellSlots  emplacements par niveau de sort, du niveau 1 au niveau 9 (liste éventuellement plus courte)
 */
public record CreatureProfile(int armorClass, int hitPointMax, int hitPoints, double speed, int initiative,
                              List<Weapon> weapons, Set<String> features, Set<String> spellsKnown,
                              List<Integer> spellSlots) {

    public CreatureProfile {
        if (hitPointMax <= 0) throw new IllegalArgumentException("PV maximum à " + hitPointMax + " : la créature ne peut pas jouer");
        if (hitPoints < 0 || hitPoints > hitPointMax) {
            throw new IllegalArgumentException("PV actuels entre 0 et %d : %d".formatted(hitPointMax, hitPoints));
        }
        weapons = weapons == null ? List.of() : List.copyOf(weapons);
        features = features == null ? Set.of() : Set.copyOf(features);
        spellsKnown = spellsKnown == null ? Set.of() : Set.copyOf(spellsKnown);
        spellSlots = spellSlots == null ? List.of() : List.copyOf(spellSlots);
        if (spellSlots.size() > 9) throw new IllegalArgumentException("Emplacements de sort du niveau 1 au niveau 9");
    }

    /** La créature de ce profil, sous cet id et ce nom. */
    public Creature create(String id, String name) {
        Creature.Builder builder = Creature.builder(id, name)
                .armorClass(armorClass).hitPoints(hitPointMax).currentHitPoints(hitPoints)
                .speed(speed).initiative(initiative);
        weapons.forEach(builder::weapon);
        features.forEach(builder::feature);
        spellsKnown.forEach(builder::knows);
        for (int level = 1; level <= spellSlots.size(); level++) builder.spellSlots(level, spellSlots.get(level - 1));
        return builder.build();
    }

    /** Un profil sans sort ni capacité : celui d'un PJ tiré de sa fiche. */
    public static CreatureProfile of(int armorClass, int hitPointMax, int hitPoints, double speed, int initiative,
                                     List<Weapon> weapons) {
        return new CreatureProfile(armorClass, hitPointMax, hitPoints, speed, initiative, new ArrayList<>(weapons),
                Set.of(), Set.of(), List.of());
    }
}
