package dnd.engine.catalog;

import dnd.engine.catalog.CatalogEntry.Kind;
import dnd.engine.state.Creature;
import dnd.engine.state.DamageType;
import dnd.engine.state.Weapon;

import java.util.List;

/**
 * Les monstres prêts à l'emploi, tirés du System Reference Document 5.2.1 de Wizards of the Coast,
 * sous licence CC-BY-4.0 (https://creativecommons.org/licenses/by/4.0/legalcode).
 *
 * Les personnages joueurs viennent des fiches de la campagne, pas d'ici.
 *
 * <p>Seul ce que le moteur sait jouer est repris : CA, PV, vitesse, initiative (modificateur des fiches), attaques
 * (allonge, portée, lancer), résistances et vulnérabilités. Les immunités (poison du Squelette) et les effets annexes
 * (Gobelin : +1d4 avec avantage ; Loup : À terre, Tactique de meute) ne sont pas encore modélisés.
 * Longueurs converties à 1,5 m pour 5 ft : allonge 5 ft = 1,5 m, 20/60 ft = 6/18 m, 80/320 ft = 24/96 m.</p>
 *
 * <p>Référence : https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf</p>
 */
public final class SrdCatalog {

    private static final List<CatalogEntry> ENTRIES = List.of(
            new CatalogEntry("gobelin", "Gobelin guerrier", Kind.MONSTER, (id, name) ->
                    Creature.builder(id, name)
                            .armorClass(15).hitPoints(10).speed(9).initiative(2)
                            .weapon(new Weapon("Cimeterre", 1, 6, DamageType.SLASHING, 4, 2, true))
                            .weapon(Weapon.ranged("Arc court", 1, 6, DamageType.PIERCING, 4, 2, 24, 96))
                            .build()),
            new CatalogEntry("squelette", "Squelette", Kind.MONSTER, (id, name) ->
                    Creature.builder(id, name)
                            .armorClass(14).hitPoints(13).speed(9).initiative(3)
                            .weapon(new Weapon("Épée courte", 1, 6, DamageType.PIERCING, 5, 3, true))
                            .weapon(Weapon.ranged("Arc court", 1, 6, DamageType.PIERCING, 5, 3, 24, 96))
                            .vulnerability(DamageType.BLUDGEONING)
                            .build()),
            new CatalogEntry("loup", "Loup", Kind.MONSTER, (id, name) ->
                    Creature.builder(id, name)
                            .armorClass(12).hitPoints(11).speed(12).initiative(2)
                            .weapon(new Weapon("Morsure", 1, 6, DamageType.PIERCING, 4, 2, false))
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
