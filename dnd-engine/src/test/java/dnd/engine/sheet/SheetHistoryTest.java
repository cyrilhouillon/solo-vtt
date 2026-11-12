package dnd.engine.sheet;

import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SheetHistoryTest {

    private static List<AbilityRoll> sixJets() {
        Dice dice = Dice.scripted(6, 5, 4, 1, 6, 6, 6, 6, 3, 3, 3, 3, 2, 4, 5, 1, 1, 1, 1, 1, 6, 2, 1, 2);
        return IntStream.range(0, 6).mapToObj(i -> AbilityRoll.roll(dice)).toList();
    }

    private static CharacterSheet construite(Build build) {
        return GuidedValues.of(build).rebuild(CharacterSheet.blank(), GuidedValues.NONE);
    }

    @Test
    void uneFicheCreee_naNiConstructionNiJets() {
        SheetState etat = SheetHistory.replay(List.of(new SheetEvent.SheetCreated()));

        assertEquals(CharacterSheet.blank(), etat.values());
        assertNull(etat.build());
        assertNull(etat.rolls());
    }

    @Test
    void rejouerUneFicheCreeeConstruiteSaisie_redonneValeursConstructionEtJets() {
        Build guerrier = Build.fighter();
        CharacterSheet construction = construite(guerrier);
        CharacterSheet retouchee = CharacterSheet.blank();
        List<AbilityRoll> jets = sixJets();

        SheetState etat = SheetHistory.replay(List.of(new SheetEvent.SheetCreated(),
                new SheetEvent.AbilitiesRolled(jets),
                new SheetEvent.SheetBuilt(guerrier, construction),
                new SheetEvent.SheetEdited(retouchee)));

        assertEquals(retouchee, etat.values(), "la saisie remplace les valeurs");
        assertEquals(guerrier, etat.build(), "la saisie garde la construction");
        assertEquals(jets, etat.rolls(), "la saisie garde les jets");
        assertEquals(List.of(15, 18, 9, 11, 3, 10), etat.rolls().stream().map(AbilityRoll::total).toList());
    }

    @Test
    void uneConstruction_remplaceLesValeursEtLaConstructionPrecedente() {
        Build premiere = Build.fighter();
        Build seconde = new Build(premiere.characterClass(), premiere.method(), premiere.scores(), premiere.skills(),
                FightingStyle.ARCHERY, StartingEquipment.FIGHTER_B, StartingEquipment.FIGHTER_B.weapons(), null, null);

        SheetState etat = SheetHistory.replay(List.of(new SheetEvent.SheetCreated(),
                new SheetEvent.SheetBuilt(premiere, construite(premiere)),
                new SheetEvent.SheetBuilt(seconde, construite(seconde))));

        assertEquals(seconde, etat.build());
        assertEquals(construite(seconde), etat.values());
        assertNull(etat.rolls());
    }

    @Test
    void lesDes_neSeLancentQuUneFois() {
        var refus = assertThrows(IllegalArgumentException.class, () -> SheetHistory.replay(List.of(
                new SheetEvent.SheetCreated(), new SheetEvent.AbilitiesRolled(sixJets()), new SheetEvent.AbilitiesRolled(sixJets()))));
        assertEquals("Les dés de caractéristique ne se lancent qu'une fois", refus.getMessage());
    }

    @Test
    void unEvenementIncomplet_estRefuse() {
        assertThrows(IllegalArgumentException.class, () -> new SheetEvent.SheetBuilt(null, CharacterSheet.blank()));
        assertThrows(IllegalArgumentException.class, () -> new SheetEvent.SheetBuilt(Build.fighter(), null));
        assertThrows(IllegalArgumentException.class, () -> new SheetEvent.AbilitiesRolled(null));
        assertThrows(IllegalArgumentException.class, () -> new SheetEvent.AbilitiesRolled(sixJets().subList(0, 5)));
    }

    /** La fiche avec cette Dextérité retouchée à la main (hausse de l'historique). */
    private static CharacterSheet avecDex(CharacterSheet f, int dex) {
        Map<Ability, Integer> abilities = new EnumMap<>(f.abilities());
        abilities.put(Ability.DEXTERITY, dex);
        return new CharacterSheet(f.className(), f.level(), f.species(), f.size(), f.background(), abilities, f.proficiencyBonus(),
                f.savingThrows(), f.skills(), f.armorClass(), f.hitPointMax(), f.hitPoints(), f.hitDice(), f.speed(),
                f.initiative(), f.attacks(), f.features(), f.speciesTraits(), f.feats(), f.equipment(), f.proficiencies(), f.languages(), f.spellSlots());
    }

    @Test
    void uneDexteriteRetouchee_compteAuChoixSuivant_etSurvitAuChoix() {
        Build premiere = Build.fighter();
        Build archer = new Build(premiere.characterClass(), premiere.method(), premiere.scores(), premiere.skills(),
                FightingStyle.ARCHERY, StartingEquipment.FIGHTER_B, premiere.masteries(), null, null);
        SheetState etat = SheetHistory.replay(List.of(new SheetEvent.SheetCreated(),
                new SheetEvent.SheetBuilt(premiere, SheetState.blank().rebuild(premiere))));
        SheetState retouchee = SheetHistory.replay(List.of(new SheetEvent.SheetCreated(),
                new SheetEvent.SheetBuilt(premiere, etat.values()),
                new SheetEvent.SheetEdited(avecDex(etat.values(), 16))));

        CharacterSheet archerie = retouchee.rebuild(archer);

        assertEquals(16, archerie.abilities().get(Ability.DEXTERITY), "la retouche survit : les valeurs de la construction n'ont pas changé");
        assertEquals(15, archerie.armorClass(), "cuir clouté 12 + Dex 3, sans Défense");
        assertEquals(3, archerie.initiative());
        assertEquals(7, archerie.attacks().get(2).bonus(), "Arc long : maîtrise 2 + Dex 3 + Archerie 2");
        assertEquals(3, archerie.attacks().get(0).damageBonus(), "Cimeterre en Finesse : Dex 3 plutôt que For 2");

        SheetState apresB = SheetHistory.replay(List.of(new SheetEvent.SheetCreated(),
                new SheetEvent.SheetBuilt(premiere, etat.values()),
                new SheetEvent.SheetEdited(avecDex(etat.values(), 16)),
                new SheetEvent.SheetBuilt(archer, archerie)));
        Build defense = new Build(archer.characterClass(), archer.method(), archer.scores(), archer.skills(),
                FightingStyle.DEFENSE, archer.equipment(), archer.masteries(), null, null);
        assertEquals(16, apresB.rebuild(defense).armorClass(), "12 + Dex 3 + Défense 1");
    }

    @Test
    void uneEspeceChoisie_neSeRetirePas() {
        Build nain = Build.fighter().withSpecies(SpeciesChoice.of(Species.DWARF));
        SheetState etat = SheetHistory.replay(List.of(new SheetEvent.SheetCreated(),
                new SheetEvent.SheetBuilt(nain, SheetState.blank().rebuild(nain))));

        var refus = assertThrows(IllegalArgumentException.class, () -> etat.rebuild(Build.fighter()));

        assertEquals("Espèce : elle se change, mais ne se retire pas", refus.getMessage());
        assertEquals(11, etat.rebuild(Build.fighter().withSpecies(SpeciesChoice.of(Species.ELF))).hitPointMax(),
                "changer d'espèce reste permis : Nain → Elfe, PV 12 → 11");
    }
}
