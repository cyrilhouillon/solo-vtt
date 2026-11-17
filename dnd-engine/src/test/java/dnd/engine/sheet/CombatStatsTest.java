package dnd.engine.sheet;

import dnd.engine.state.DamageType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** CA et attaques calculées de ce que la fiche porte. */
class CombatStatsTest {

    private static CharacterSheet construite(CharacterClass classe) {
        return SheetState.blank().rebuild(Build.suggested(classe));
    }

    /** La fiche, avec cet inventaire. */
    private static CharacterSheet porte(CharacterSheet f, Inventory inventaire) {
        return copie(f, inventaire, f.abilities(), f.armorBonus(), f.otherAttacks());
    }

    /** La fiche, avec en plus ces objets. */
    private static CharacterSheet ajoute(CharacterSheet f, Item... objets) {
        return porte(f, f.inventory().plus(new Inventory(List.of(objets), 0)));
    }

    /** La fiche, sans les objets de cette arme ou armure. */
    private static CharacterSheet retire(CharacterSheet f, Object objet) {
        List<Item> gardes = new ArrayList<>(f.inventory().items());
        gardes.removeIf(i -> objet.equals(i.weapon()) || objet.equals(i.armor()));
        return porte(f, new Inventory(gardes, f.inventory().gold()));
    }

    private static CharacterSheet copie(CharacterSheet f, Inventory inventaire, Map<Ability, Integer> valeurs, int ajustement,
                                        List<SheetAttack> autres) {
        return new CharacterSheet(f.characterClass(), f.fightingStyle(), f.level(), f.species(), f.size(), f.background(), valeurs,
                f.proficiencyBonus(), f.savingThrows(), f.skills(), ajustement, f.hitPointMax(), f.hitPoints(), f.hitDice(), f.speed(),
                f.initiative(), autres, f.features(), f.speciesTraits(), f.feats(), inventaire, f.proficiencies(), f.languages(),
                f.spellSlots(), null, List.of(), List.of(), List.of());
    }

    private static List<String> noms(CharacterSheet fiche) {
        return fiche.attacks().stream().map(SheetAttack::name).toList();
    }

    @Test
    void unGuerrierQuiPrendUnBouclier_gagne2() {
        CharacterSheet guerrier = construite(CharacterClass.FIGHTER);

        assertEquals(17, guerrier.armorClass(), "cotte de mailles 16 + Défense 1");
        assertEquals(19, ajoute(guerrier, Item.armor(SrdArmor.SHIELD)).armorClass());
    }

    @Test
    void unBarbareEnArmure_perdSaDefenseSansArmure() {
        CharacterSheet barbare = construite(CharacterClass.BARBARIAN);
        assertEquals(13, barbare.armorClass(), "10 + Dex 1 + Con 2");

        CharacterSheet mailles = ajoute(barbare, Item.armor(SrdArmor.CHAIN_SHIRT));
        assertEquals(14, mailles.armorClass(), "chemise de mailles 13 + Dex 1");
        assertEquals(16, ajoute(mailles, Item.armor(SrdArmor.SHIELD)).armorClass());
        assertEquals(15, ajoute(barbare, Item.armor(SrdArmor.SHIELD)).armorClass(), "sans armure, bouclier permis : 13 + 2");
    }

    @Test
    void unMoineEnArmure_perdArtsMartiauxEtSaDefense() {
        CharacterSheet moine = ajoute(construite(CharacterClass.MONK), Item.armor(SrdArmor.LEATHER));

        assertEquals(13, moine.armorClass(), "cuir 11 + Dex 2, sans Défense sans armure");
        assertEquals(List.of(
                new SheetAttack("Lance", 3, 1, 6, 1, DamageType.PIERCING, 1.5, 6.0, 18.0),
                new SheetAttack("Dague", 4, 1, 4, 2, DamageType.PIERCING, 1.5, 6.0, 18.0),
                new SheetAttack("Attaque à mains nues", 3, 0, 4, 2, DamageType.BLUDGEONING, 1.5, null, null)), moine.attacks());
    }

    @Test
    void unMoineAuBouclier_perdArtsMartiaux_sansBonusDeCA() {
        CharacterSheet moine = ajoute(construite(CharacterClass.MONK), Item.armor(SrdArmor.SHIELD));

        assertEquals(12, moine.armorClass(), "10 + Dex 2 : ni Défense sans armure, ni bouclier sans formation");
        assertEquals(new SheetAttack("Attaque à mains nues", 3, 0, 4, 2, DamageType.BLUDGEONING, 1.5, null, null),
                moine.attacks().getLast());
    }

    @Test
    void uneArmeNonMaitrisee_nAjoutePasLeBonusDeMaitrise() {
        CharacterSheet moine = ajoute(construite(CharacterClass.MONK), Item.weapon(SrdWeapon.LONGSWORD, 1));

        assertEquals(new SheetAttack("Épée longue", 1, 1, 8, 1, DamageType.SLASHING, 1.5, null, null), moine.attacks().get(2),
                "For 1, sans maîtrise ni Arts martiaux");
    }

    @Test
    void unRoublardAuBouclier_nyGagneRien_etSansArmure_a10PlusDex() {
        CharacterSheet roublard = construite(CharacterClass.ROGUE);
        assertEquals(13, roublard.armorClass(), "cuir 11 + Dex 2");
        assertEquals(13, ajoute(roublard, Item.armor(SrdArmor.SHIELD)).armorClass(), "pas de formation au bouclier");
        assertEquals(12, retire(roublard, SrdArmor.LEATHER).armorClass());
    }

    @Test
    void retirerUneArme_laRetireDesAttaques_etDeuxLignes_neDonnentQuUneAttaque() {
        CharacterSheet roublard = construite(CharacterClass.ROGUE);

        assertEquals(List.of("Dague", "Épée courte", "Attaque à mains nues"), noms(retire(roublard, SrdWeapon.SHORTBOW)));
        assertEquals(List.of("Dague", "Épée courte", "Arc court", "Attaque à mains nues"),
                noms(ajoute(roublard, Item.weapon(SrdWeapon.DAGGER, 3))));
    }

    @Test
    void lAjustementEtLesAutresAttaques_sAjoutentAuCalcul() {
        CharacterSheet guerrier = construite(CharacterClass.FIGHTER);
        SheetAttack trait = new SheetAttack("Trait de feu", 4, 1, 10, 0, DamageType.FIRE, null, 36.0, 36.0);

        CharacterSheet fiche = copie(guerrier, guerrier.inventory(), guerrier.abilities(), 3, List.of(trait));

        assertEquals(20, fiche.armorClass());
        assertEquals(trait, fiche.attacks().getLast());
        assertEquals("Trait de feu", fiche.profile().create("pj", "Aria").weapons().getLast().name());
        assertEquals(20, fiche.profile().create("pj", "Aria").armorClass());
    }

    @Test
    void uneCaracteristiqueRetouchee_estSuivieSansAutreGeste() {
        CharacterSheet roublard = construite(CharacterClass.ROGUE);
        Map<Ability, Integer> valeurs = new EnumMap<>(roublard.abilities());
        valeurs.put(Ability.DEXTERITY, 18);

        CharacterSheet agile = copie(roublard, roublard.inventory(), valeurs, 0, List.of());

        assertEquals(15, agile.armorClass(), "cuir 11 + Dex 4");
        assertEquals(6, agile.attacks().getFirst().bonus(), "Dague : maîtrise 2 + Dex 4");
    }

    @Test
    void uneConstructionQuiChangeDOption_remplaceLInventaire() {
        Build a = Build.suggested(CharacterClass.ROGUE);
        Build b = new Build(a.characterClass(), a.method(), a.scores(), a.skills(), null, StartingEquipment.ROGUE_B,
                a.masteries(), null, null, Set.of(), null, List.of(), List.of(), List.of());
        CharacterSheet fiche = ajoute(construite(CharacterClass.ROGUE), Item.named("Corde", 1));
        SheetState etat = new SheetState(fiche, a, a.abilities(), null);

        CharacterSheet apres = etat.rebuild(b);

        assertEquals(new Inventory(List.of(), 100), apres.inventory());
        assertEquals(12, apres.armorClass());
        assertEquals(List.of("Attaque à mains nues"), noms(apres));
    }
}
