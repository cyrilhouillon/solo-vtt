package dnd.engine.sheet;

import dnd.engine.catalog.CatalogEntry;
import dnd.engine.state.Attack;
import dnd.engine.state.Creature;
import dnd.engine.state.DamageType;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CharacterProfileTest {

    private static final SheetAttack EPEE = new SheetAttack("Épée longue", 5, 1, 8, 3, DamageType.SLASHING, 1.5, null, null);
    private static final SheetAttack JAVELINE = new SheetAttack("Javeline", 5, 1, 6, 3, DamageType.PIERCING, 1.5, 9.0, 36.0);

    private static CharacterSheet fiche(int pvMax, int pv, List<SheetAttack> attaques) {
        Map<Ability, Integer> valeurs = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) valeurs.put(ability, 10);
        return new CharacterSheet("Guerrier", 1, "", "", "", valeurs, 2, Set.of(), Map.of(),
                18, pvMax, pv, "1d10", 9, 2, attaques, "", "", "", "", "", List.of());
    }

    @Test
    void laCreatureDUneFiche_aSesValeurs_etUneAttaqueParMode() {
        Creature aria = fiche(12, 12, List.of(EPEE, JAVELINE)).profile().create("pj-1", "Aria");

        assertEquals("Aria", aria.name());
        assertEquals(18, aria.armorClass());
        assertEquals(12, aria.maxHitPoints());
        assertEquals(9, aria.speed());
        assertEquals(2, aria.initiative());
        assertEquals(List.of("Épée longue", "Javeline", "Javeline (lancer)"), aria.attacks().stream().map(Attack::label).toList());
        var epee = aria.weapons().getFirst();
        assertEquals(5, epee.attackBonus());
        assertEquals(3, epee.damageBonus());
        assertEquals("1d8", epee.diceNotation());
        assertEquals(DamageType.SLASHING, epee.damageType());
        assertEquals(9, aria.weapons().get(1).range().normal());
    }

    @Test
    void unPJBlesse_gardeSesPVActuels() {
        Creature aria = fiche(12, 5, List.of()).profile().create("pj-1", "Aria");

        assertEquals(5, aria.hitPoints());
        assertEquals(12, aria.maxHitPoints());
    }

    @Test
    void uneFicheAZeroPVMax_neSePosePas() {
        var refus = assertThrows(IllegalArgumentException.class, () -> CharacterSheet.blank().profile());
        assertEquals("PV maximum à 0 : compléter la fiche avant de la poser", refus.getMessage());
    }

    @Test
    void uneAttaqueImpossible_estRefuseeAvecSaRaison() {
        var sansPortee = assertThrows(IllegalArgumentException.class,
                () -> new SheetAttack("Coup", 2, 1, 4, 0, DamageType.BLUDGEONING, null, null, null));
        assertEquals("Coup : une allonge ou une portée", sansPortee.getMessage());
        var de7 = assertThrows(IllegalArgumentException.class,
                () -> new SheetAttack("Coup", 2, 1, 7, 0, DamageType.BLUDGEONING, 1.5, null, null));
        assertEquals("Coup : dé de dégâts d7 inconnu", de7.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> new SheetAttack("Arc", 2, 1, 6, 0, DamageType.PIERCING, null, 24.0, 12.0));
        assertThrows(IllegalArgumentException.class,
                () -> new SheetAttack("Arc", 2, 1, 6, 0, DamageType.PIERCING, null, 24.0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new SheetAttack("Coup", 2, 1, 6, 0, null, 1.5, null, null));
        var doublon = assertThrows(IllegalArgumentException.class, () -> fiche(12, 12, List.of(EPEE, EPEE)));
        assertEquals("Deux attaques portent le même nom", doublon.getMessage());
    }

    @Test
    void laSarbacaneGuidee_infligeDesDegatsFixes() {
        assertEquals(0, SrdWeapon.BLOWGUN.diceCount());
        assertEquals(1, SrdWeapon.BLOWGUN.flatDamage());
        assertEquals("1", SrdWeapon.BLOWGUN.damage());
        assertEquals("2d6", SrdWeapon.GREATSWORD.damage());
    }

    @Test
    void laSarbacaneGuidee_neDonnePasSonModificateurAuxDegatsFixes() {
        SheetAttack sarbacane = GuidedValues.attack(SrdWeapon.BLOWGUN, 0, 3, FightingStyle.ARCHERY);

        assertEquals(0, sarbacane.diceCount());
        assertEquals(1, sarbacane.damageBonus());
        // Le modificateur compte au jet d'attaque : maîtrise 2 + Dex 3 + Archerie 2.
        assertEquals(7, sarbacane.bonus());
    }

    @Test
    void lesTreizeTypesDeDegatsDuSrd() {
        assertEquals(13, DamageType.values().length);
    }

    @Test
    void uneEntreeDePersonnage_creeSaCopieSousLeNomDonne() {
        CatalogEntry entree = CatalogEntry.character("page-1", "Aria", fiche(12, 7, List.of(EPEE)).profile());

        Creature aria = entree.create("page-1-1", "Aria 2");

        assertEquals(CatalogEntry.Kind.CHARACTER, entree.kind());
        assertEquals("Aria 2", aria.name());
        assertEquals(7, aria.hitPoints());
    }
}
