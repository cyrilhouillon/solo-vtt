package dnd.engine.sheet;

import dnd.engine.state.DamageType;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuidedValuesTest {

    private static final Set<Skill> ATHLETISME_PERCEPTION = Set.of(Skill.ATHLETICS, Skill.PERCEPTION);

    private static Build guerrier(StartingEquipment equipement, FightingStyle style) {
        Build base = Build.fighter();
        return new Build(base.characterClass(), base.method(), base.scores(), ATHLETISME_PERCEPTION, style,
                equipement, base.masteries());
    }

    private static CharacterSheet construite(Build build) {
        return GuidedValues.of(build).rebuild(CharacterSheet.blank(), GuidedValues.NONE);
    }

    /** La fiche avec une CA, des PV max ou des compétences retouchés à la main. */
    private static CharacterSheet retouchee(CharacterSheet f, int armorClass, int hitPointMax, Map<Skill, Proficiency> skills) {
        return new CharacterSheet(f.className(), f.level(), f.species(), f.background(), f.abilities(), f.proficiencyBonus(),
                f.savingThrows(), skills, armorClass, hitPointMax, f.hitPoints(), f.hitDice(), f.speed(),
                f.initiative(), f.attacks(), f.features(), f.equipment(), f.proficiencies(), f.languages(), f.spellSlots());
    }

    @Test
    void unGuerrierA_Defense_remplitLaFiche() {
        CharacterSheet fiche = construite(guerrier(StartingEquipment.FIGHTER_A, FightingStyle.DEFENSE));

        assertEquals("Guerrier", fiche.className());
        assertEquals(1, fiche.level());
        assertEquals(2, fiche.proficiencyBonus());
        assertEquals(15, fiche.abilities().get(Ability.STRENGTH));
        assertEquals(8, fiche.abilities().get(Ability.INTELLIGENCE));
        assertEquals(Set.of(Ability.STRENGTH, Ability.CONSTITUTION), fiche.savingThrows());
        assertEquals(Map.of(Skill.ATHLETICS, Proficiency.PROFICIENT, Skill.PERCEPTION, Proficiency.PROFICIENT), fiche.skills());
        assertEquals(17, fiche.armorClass(), "cotte de mailles 16 + Défense 1");
        assertEquals(11, fiche.hitPointMax(), "d10 + Con 1");
        assertEquals(11, fiche.hitPoints());
        assertEquals("1d10", fiche.hitDice());
        assertEquals(2, fiche.initiative());
        assertEquals(List.of(
                new SheetAttack("Épée à deux mains", 4, 2, 6, 2, DamageType.SLASHING, 1.5, null, null),
                new SheetAttack("Fléau d’armes", 4, 1, 8, 2, DamageType.BLUDGEONING, 1.5, null, null),
                new SheetAttack("Javeline", 4, 1, 6, 2, DamageType.PIERCING, 1.5, 9.0, 36.0)), fiche.attacks());
        assertTrue(fiche.features().contains(
                "Bottes d’arme : Épée à deux mains (Écorchure), Fléau d’armes (Sape), Javeline (Ralentissement)"), fiche.features());
        assertTrue(fiche.features().contains("Second souffle (2 utilisations) : action Bonus, 1d10 + 1 PV"), fiche.features());
        assertTrue(fiche.features().contains("Style de combat : Défense"), fiche.features());
        assertTrue(fiche.equipment().startsWith("Cotte de mailles, épée à deux mains"), fiche.equipment());
        assertTrue(fiche.equipment().endsWith("4 po"), fiche.equipment());
        assertTrue(fiche.proficiencies().contains("Armes courantes et armes de guerre"), fiche.proficiencies());
    }

    @Test
    void unGuerrierB_Archerie_aCA14_etLArcLongA6() {
        CharacterSheet fiche = construite(guerrier(StartingEquipment.FIGHTER_B, FightingStyle.ARCHERY));

        assertEquals(14, fiche.armorClass(), "cuir clouté 12 + Dex 2, sans Défense");
        assertEquals(List.of(
                new SheetAttack("Cimeterre", 4, 1, 6, 2, DamageType.SLASHING, 1.5, null, null),
                new SheetAttack("Épée courte", 4, 1, 6, 2, DamageType.PIERCING, 1.5, null, null),
                new SheetAttack("Arc long", 6, 1, 8, 2, DamageType.PIERCING, null, 45.0, 180.0)), fiche.attacks());
        assertTrue(fiche.equipment().endsWith("11 po"), fiche.equipment());
    }

    @Test
    void passerDeAaB_recalculeLaCA_retouchee_etGardeLesPVMaxRetouches() {
        Build avant = guerrier(StartingEquipment.FIGHTER_A, FightingStyle.DEFENSE);
        Build apres = guerrier(StartingEquipment.FIGHTER_B, FightingStyle.DEFENSE);
        CharacterSheet fiche = construite(avant);
        fiche = retouchee(fiche, 20, 30, fiche.skills());

        CharacterSheet recalculee = GuidedValues.of(apres).rebuild(fiche, GuidedValues.of(avant));

        assertEquals(15, recalculee.armorClass(), "12 + Dex 2 + Défense 1");
        assertEquals(30, recalculee.hitPointMax(), "les PV max ne dépendent pas de l'équipement");
        assertEquals("Arc long", recalculee.attacks().get(2).name());
        assertTrue(recalculee.equipment().endsWith("11 po"), recalculee.equipment());
    }

    @Test
    void echangerConstitutionEtIntelligence_gardeLExpertiseRetouchee_etPasseLesPVA9() {
        Build avant = guerrier(StartingEquipment.FIGHTER_A, FightingStyle.DEFENSE);
        Map<Ability, Integer> echangees = new EnumMap<>(avant.scores());
        echangees.put(Ability.CONSTITUTION, 8);
        echangees.put(Ability.INTELLIGENCE, 13);
        Build apres = new Build(avant.characterClass(), avant.method(), echangees, avant.skills(),
                avant.fightingStyle(), avant.equipment(), avant.masteries());
        CharacterSheet fiche = construite(avant);
        Map<Skill, Proficiency> expertise = new HashMap<>(fiche.skills());
        expertise.put(Skill.PERCEPTION, Proficiency.EXPERTISE);
        fiche = retouchee(fiche, fiche.armorClass(), fiche.hitPointMax(), expertise);

        CharacterSheet recalculee = GuidedValues.of(apres).rebuild(fiche, GuidedValues.of(avant));

        assertEquals(Proficiency.EXPERTISE, recalculee.proficiency(Skill.PERCEPTION));
        assertEquals(8, recalculee.abilities().get(Ability.CONSTITUTION));
        assertEquals(13, recalculee.abilities().get(Ability.INTELLIGENCE));
        assertEquals(9, recalculee.hitPointMax(), "d10 + Con −1");
        assertEquals(9, recalculee.hitPoints());
    }
}
