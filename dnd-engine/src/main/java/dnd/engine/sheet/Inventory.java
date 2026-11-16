package dnd.engine.sheet;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Ce que porte un personnage : ses objets, dans l'ordre, et ses pièces d'or. Une seule armure portée et un seul bouclier
 * tenu à la fois (SRD 5.2.1, « Armor »).
 */
public record Inventory(List<Item> items, int gold) {

    public static final Inventory EMPTY = new Inventory(List.of(), 0);

    public Inventory {
        items = items == null ? List.of() : List.copyOf(items);
        if (gold < 0) throw new IllegalArgumentException("Pièces d’or négatives : " + gold);
        if (worn(items, false).count() > 1) throw new IllegalArgumentException("Une seule armure portée à la fois");
        if (worn(items, true).count() > 1) throw new IllegalArgumentException("Un seul bouclier tenu à la fois");
    }

    /** L'armure portée, {@code null} sans. */
    public SrdArmor armor() {
        return worn(items, false).findFirst().orElse(null);
    }

    /** Un bouclier est-il tenu. */
    public boolean shield() {
        return worn(items, true).findAny().isPresent();
    }

    /** Les armes tenues, chacune une fois, dans l'ordre de l'inventaire. */
    public List<SrdWeapon> weapons() {
        return items.stream().filter(Item::equipped).map(Item::weapon).filter(w -> w != null).distinct().toList();
    }

    /** Cet inventaire, suivi des objets et des pièces d'or d'un autre. */
    public Inventory plus(Inventory other) {
        List<Item> all = new ArrayList<>(items);
        all.addAll(other.items);
        return new Inventory(all, gold + other.gold);
    }

    /** Les objets puis les pièces d'or, tels qu'une option d'équipement se décrit. */
    public String text() {
        String objects = items.stream().map(Item::text).collect(Collectors.joining(", "));
        return objects.isEmpty() ? gold + " po" : objects + ", " + gold + " po";
    }

    private static Stream<SrdArmor> worn(List<Item> items, boolean shield) {
        return items.stream().filter(Item::equipped).map(Item::armor)
                .filter(a -> a != null && (a == SrdArmor.SHIELD) == shield);
    }
}
