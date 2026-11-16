package dnd.engine.sheet;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryTest {

    private static String refus(Runnable creation) {
        return assertThrows(IllegalArgumentException.class, creation::run).getMessage();
    }

    @Test
    void unInventaireImpossible_estRefuseAvecSaRaison() {
        assertEquals("Une seule armure portée à la fois", refus(() -> new Inventory(List.of(
                Item.armor(SrdArmor.LEATHER), Item.armor(SrdArmor.CHAIN_MAIL)), 0)));
        assertEquals("Un seul bouclier tenu à la fois", refus(() -> new Inventory(List.of(
                Item.armor(SrdArmor.SHIELD), Item.armor(SrdArmor.SHIELD)), 0)));
        assertEquals("Pièces d’or négatives : -1", refus(() -> new Inventory(List.of(), -1)));
        assertEquals("Corde : seules armes, armures et boucliers s’équipent",
                refus(() -> new Item(null, null, "Corde", 1, true)));
        assertEquals("Dague : quantité d’au moins 1", refus(() -> new Item(SrdWeapon.DAGGER, null, null, 0, false)));
        assertEquals("Un objet a un nom", refus(() -> Item.named("  ", 1)));
        assertEquals("Nom d’objet trop long (80 caractères au plus)", refus(() -> Item.named("x".repeat(81), 1)));
        assertEquals("Un objet est une arme, une armure ou un nom",
                refus(() -> new Item(SrdWeapon.DAGGER, SrdArmor.LEATHER, null, 1, false)));
        assertEquals("Un objet est une arme, une armure ou un nom", refus(() -> new Item(null, null, null, 1, false)));
    }

    @Test
    void uneArmureRangee_etUnBouclierRange_neSontPasPortes() {
        Inventory sac = new Inventory(List.of(new Item(null, SrdArmor.CHAIN_MAIL, null, 1, false),
                new Item(null, SrdArmor.SHIELD, null, 1, false), Item.armor(SrdArmor.LEATHER)), 0);

        assertEquals(SrdArmor.LEATHER, sac.armor());
        assertFalse(sac.shield());
    }

    @Test
    void lesArmesTenues_uneFoisChacune_dansLOrdre() {
        Inventory sac = new Inventory(List.of(Item.weapon(SrdWeapon.DAGGER, 2), Item.weapon(SrdWeapon.SHORTBOW, 1),
                new Item(SrdWeapon.LONGSWORD, null, null, 1, false), Item.weapon(SrdWeapon.DAGGER, 1)), 0);

        assertEquals(List.of(SrdWeapon.DAGGER, SrdWeapon.SHORTBOW), sac.weapons());
        assertNull(sac.armor());
    }

    @Test
    void lInventaireDeDepart_aLesObjetsDeLaClasseEtDeLHistorique_etLeursPo() {
        Build guerrier = Build.fighter().withBackground(new BackgroundChoice(Background.SOLDIER,
                java.util.Map.of(Ability.STRENGTH, 2, Ability.CONSTITUTION, 1), BackgroundEquipment.A, GamingSet.DICE, null));

        Inventory inventaire = guerrier.startingInventory();

        assertEquals(18, inventaire.gold(), "4 + 14");
        assertEquals(Item.armor(SrdArmor.CHAIN_MAIL), inventaire.items().getFirst());
        assertEquals(Item.weapon(SrdWeapon.JAVELIN, 8), inventaire.items().get(3));
        assertEquals(Item.named("Boîte de jeux (dés)", 1), inventaire.items().getLast());
        assertEquals(List.of(SrdWeapon.GREATSWORD, SrdWeapon.FLAIL, SrdWeapon.JAVELIN, SrdWeapon.SHORTBOW, SrdWeapon.SPEAR),
                inventaire.weapons());
    }

    @Test
    void uneOptionDEquipement_seDecritParSesObjets() {
        assertEquals("Cotte de mailles, Épée à deux mains, Fléau d’armes, Javeline ×8, Paquetage d’exploration souterraine, 4 po",
                StartingEquipment.FIGHTER_A.text());
        assertEquals("Lance, Dague ×5, l’outil d’artisan ou l’instrument de musique choisi, Paquetage d’explorateur, 11 po",
                StartingEquipment.MONK_A.text());
        assertEquals("155 po", StartingEquipment.FIGHTER_C.text());
        assertTrue(Background.SOLDIER.equipmentText(BackgroundEquipment.A).contains("Boîte de jeux (même type)"));
        assertEquals("50 po", Background.SOLDIER.equipmentText(BackgroundEquipment.B));
        assertEquals(List.of(SrdWeapon.SPEAR, SrdWeapon.DAGGER), StartingEquipment.MONK_A.weapons());
    }

    @Test
    void lesArmuresDuSrd_etLaFormationDesClasses() {
        assertEquals(13, SrdArmor.values().length, "12 armures et le bouclier");
        assertEquals(15, SrdArmor.CHAIN_SHIRT.armorClass(5), "13 + Dex 2 au plus");
        assertEquals(18, SrdArmor.PLATE.armorClass(3));
        assertEquals(15, SrdArmor.PLATE.strength());
        assertEquals(13, SrdArmor.CHAIN_MAIL.strength());
        assertTrue(SrdArmor.PADDED.stealthDisadvantage());
        assertFalse(SrdArmor.BREASTPLATE.stealthDisadvantage());

        assertTrue(CharacterClass.FIGHTER.trained(SrdArmor.PLATE));
        assertTrue(CharacterClass.BARBARIAN.trained(SrdArmor.SHIELD));
        assertFalse(CharacterClass.BARBARIAN.trained(SrdArmor.PLATE));
        assertFalse(CharacterClass.MONK.trained(SrdArmor.LEATHER));
        assertFalse(CharacterClass.MONK.trained(SrdArmor.SHIELD));
        assertTrue(CharacterClass.ROGUE.trained(SrdArmor.STUDDED_LEATHER));
        assertFalse(CharacterClass.ROGUE.trained(SrdArmor.HIDE));
        assertFalse(CharacterClass.ROGUE.trained(SrdArmor.SHIELD));

        assertTrue(CharacterClass.BARBARIAN.proficient(SrdWeapon.LONGBOW), "maîtrise d'arme, distincte des Bottes");
        assertFalse(CharacterClass.BARBARIAN.masters(SrdWeapon.LONGBOW));
        assertTrue(CharacterClass.MONK.proficient(SrdWeapon.SCIMITAR));
        assertTrue(CharacterClass.MONK.proficient(SrdWeapon.SHORTBOW));
        assertFalse(CharacterClass.MONK.proficient(SrdWeapon.LONGSWORD));
        assertTrue(CharacterClass.ROGUE.proficient(SrdWeapon.RAPIER));
        assertFalse(CharacterClass.ROGUE.proficient(SrdWeapon.GREATSWORD));
    }
}
