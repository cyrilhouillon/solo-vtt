package dnd.engine.catalog;

import dnd.engine.catalog.CatalogEntry.Kind;
import dnd.engine.rules.DuelingStyle;
import dnd.engine.rules.ShieldReaction;
import dnd.engine.state.Creature;
import dnd.engine.state.DamageType;
import dnd.engine.state.Weapon;

import java.util.List;

/**
 * Créatures prêtes à l'emploi, tirées du System Reference Document 5.2.1 de Wizards of the Coast,
 * sous licence CC-BY-4.0 (https://creativecommons.org/licenses/by/4.0/legalcode).
 *
 * <p>Seul ce que le moteur sait jouer est repris : CA, PV, vitesse, attaque principale,
 * résistances et vulnérabilités. Les immunités (poison du Squelette), les effets annexes
 * (Gobelin : +1d4 avec avantage ; Loup : À terre, Tactique de meute) et les attaques secondaires
 * ne sont pas encore modélisés. Vitesses converties à 1,5 m pour 5 ft.</p>
 */
public final class SrdCatalog {

    private static final List<CatalogEntry> ENTRIES = List.of(
            // Niveau 1 : For 17, Con 14 ; cotte de mailles + bouclier ; Épée longue (For +3, maîtrise +2).
            new CatalogEntry("guerriere", "Guerrière", Kind.CHARACTER, id ->
                    Creature.builder(id, "Guerrière")
                            .armorClass(18).hitPoints(12).speed(9)
                            .weapon(new Weapon("Épée longue", 1, 8, DamageType.SLASHING, 5, 3, true))
                            .feature(DuelingStyle.FEATURE)
                            .build()),
            // Niveau 1 : Int 17, Dex 14, Con 13 ; sans armure ; Dague (Dex +2, maîtrise +2).
            new CatalogEntry("magicienne", "Magicienne", Kind.CHARACTER, id ->
                    Creature.builder(id, "Magicienne")
                            .armorClass(12).hitPoints(7).speed(9)
                            .weapon(new Weapon("Dague", 1, 4, DamageType.PIERCING, 4, 2, true))
                            .knows(ShieldReaction.SPELL).spellSlots(1, 2)
                            .build()),
            new CatalogEntry("gobelin", "Gobelin guerrier", Kind.MONSTER, id ->
                    Creature.builder(id, "Gobelin guerrier")
                            .armorClass(15).hitPoints(10).speed(9)
                            .weapon(new Weapon("Cimeterre", 1, 6, DamageType.SLASHING, 4, 2, true))
                            .build()),
            new CatalogEntry("squelette", "Squelette", Kind.MONSTER, id ->
                    Creature.builder(id, "Squelette")
                            .armorClass(13).hitPoints(13).speed(9)
                            .weapon(new Weapon("Épée courte", 1, 6, DamageType.PIERCING, 4, 2, true))
                            .vulnerability(DamageType.BLUDGEONING)
                            .build()),
            new CatalogEntry("loup", "Loup", Kind.MONSTER, id ->
                    Creature.builder(id, "Loup")
                            .armorClass(13).hitPoints(11).speed(12)
                            .weapon(new Weapon("Morsure", 2, 4, DamageType.PIERCING, 4, 2, false))
                            .build())
    );

    private SrdCatalog() {
    }

    public static List<CatalogEntry> entries() {
        return ENTRIES;
    }

    public static CatalogEntry entry(String entryId) {
        return ENTRIES.stream().filter(e -> e.id().equals(entryId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Entrée de catalogue inconnue : " + entryId));
    }

    public static Creature create(String entryId, String instanceId) {
        return entry(entryId).create(instanceId);
    }
}
