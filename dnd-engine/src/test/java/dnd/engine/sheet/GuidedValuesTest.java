package dnd.engine.sheet;

import dnd.engine.state.DamageType;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuidedValuesTest {

    private static final Set<Skill> ATHLETISME_PERCEPTION = Set.of(Skill.ATHLETICS, Skill.PERCEPTION);

    private static Build guerrier(StartingEquipment equipement, FightingStyle style) {
        Build base = Build.fighter();
        return new Build(base.characterClass(), base.method(), base.scores(), ATHLETISME_PERCEPTION, style,
                equipement, base.masteries(), null, null, Set.of(), null);
    }

    private static CharacterSheet construite(Build build) {
        return GuidedValues.of(build).rebuild(CharacterSheet.blank(), GuidedValues.NONE);
    }

    /** La fiche avec un ajustement de CA, des PV max ou des compétences retouchés à la main. */
    private static CharacterSheet retouchee(CharacterSheet f, int armorBonus, int hitPointMax, Map<Skill, Proficiency> skills) {
        return new CharacterSheet(f.characterClass(), f.fightingStyle(), f.level(), f.species(), f.size(), f.background(), f.abilities(),
                f.proficiencyBonus(), f.savingThrows(), skills, armorBonus, hitPointMax, f.hitPoints(), f.hitDice(), f.speed(),
                f.initiative(), f.otherAttacks(), f.features(), f.speciesTraits(), f.feats(), f.inventory(), f.proficiencies(),
                f.languages(), f.spellSlots());
    }

    @Test
    void unGuerrierA_Defense_remplitLaFiche() {
        CharacterSheet fiche = construite(guerrier(StartingEquipment.FIGHTER_A, FightingStyle.DEFENSE));

        assertEquals(CharacterClass.FIGHTER, fiche.characterClass());
        assertEquals(FightingStyle.DEFENSE, fiche.fightingStyle());
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
                new SheetAttack("Javeline", 4, 1, 6, 2, DamageType.PIERCING, 1.5, 9.0, 36.0),
                new SheetAttack("Attaque à mains nues", 4, 0, 4, 3, DamageType.BLUDGEONING, 1.5, null, null)), fiche.attacks());
        assertTrue(fiche.features().contains(
                "Bottes d’arme : Épée à deux mains (Écorchure), Fléau d’armes (Sape), Javeline (Ralentissement)"), fiche.features());
        assertTrue(fiche.features().contains("Second souffle (2 utilisations) : action Bonus, 1d10 + 1 PV"), fiche.features());
        assertTrue(fiche.features().contains("Style de combat : Défense"), fiche.features());
        assertEquals(new Inventory(List.of(Item.armor(SrdArmor.CHAIN_MAIL), Item.weapon(SrdWeapon.GREATSWORD, 1),
                Item.weapon(SrdWeapon.FLAIL, 1), Item.weapon(SrdWeapon.JAVELIN, 8),
                Item.named("Paquetage d’exploration souterraine", 1)), 4), fiche.inventory());
        assertTrue(fiche.proficiencies().contains("Armes courantes et armes de guerre"), fiche.proficiencies());
    }

    @Test
    void unGuerrierB_Archerie_aCA14_etLArcLongA6() {
        CharacterSheet fiche = construite(guerrier(StartingEquipment.FIGHTER_B, FightingStyle.ARCHERY));

        assertEquals(14, fiche.armorClass(), "cuir clouté 12 + Dex 2, sans Défense");
        assertEquals(List.of(
                new SheetAttack("Cimeterre", 4, 1, 6, 2, DamageType.SLASHING, 1.5, null, null),
                new SheetAttack("Épée courte", 4, 1, 6, 2, DamageType.PIERCING, 1.5, null, null),
                new SheetAttack("Arc long", 6, 1, 8, 2, DamageType.PIERCING, null, 45.0, 180.0),
                new SheetAttack("Attaque à mains nues", 4, 0, 4, 3, DamageType.BLUDGEONING, 1.5, null, null)), fiche.attacks());
        assertEquals(11, fiche.inventory().gold());
    }

    @Test
    void passerDeAaB_remplaceLInventaire_etGardeAjustementEtPVMaxRetouches() {
        Build avant = guerrier(StartingEquipment.FIGHTER_A, FightingStyle.DEFENSE);
        Build apres = guerrier(StartingEquipment.FIGHTER_B, FightingStyle.DEFENSE);
        CharacterSheet fiche = construite(avant);
        fiche = retouchee(fiche, 1, 30, fiche.skills());

        CharacterSheet recalculee = GuidedValues.of(apres).rebuild(fiche, GuidedValues.of(avant));

        assertEquals(16, recalculee.armorClass(), "12 + Dex 2 + Défense 1 + ajustement 1");
        assertEquals(30, recalculee.hitPointMax(), "les PV max ne dépendent pas de l'équipement");
        assertEquals("Arc long", recalculee.attacks().get(2).name());
        assertEquals(11, recalculee.inventory().gold());
    }

    @Test
    void unChoixSansRapportAvecLEquipement_gardeLInventaireRetouche() {
        Build avant = guerrier(StartingEquipment.FIGHTER_A, FightingStyle.DEFENSE);
        Build apres = new Build(avant.characterClass(), avant.method(), avant.scores(), Set.of(Skill.ATHLETICS, Skill.HISTORY),
                avant.fightingStyle(), avant.equipment(), avant.masteries(), null, null, Set.of(), null);
        CharacterSheet fiche = construite(avant);
        Inventory bouclier = fiche.inventory().plus(new Inventory(List.of(Item.armor(SrdArmor.SHIELD)), 0));
        fiche = new CharacterSheet(fiche.characterClass(), fiche.fightingStyle(), fiche.level(), fiche.species(), fiche.size(),
                fiche.background(), fiche.abilities(), fiche.proficiencyBonus(), fiche.savingThrows(), fiche.skills(),
                fiche.armorBonus(), fiche.hitPointMax(), fiche.hitPoints(), fiche.hitDice(), fiche.speed(), fiche.initiative(),
                fiche.otherAttacks(), fiche.features(), fiche.speciesTraits(), fiche.feats(), bouclier, fiche.proficiencies(),
                fiche.languages(), fiche.spellSlots());

        CharacterSheet recalculee = GuidedValues.of(apres).rebuild(fiche, GuidedValues.of(avant));

        assertEquals(bouclier, recalculee.inventory());
        assertEquals(19, recalculee.armorClass(), "16 + Défense 1 + bouclier 2");
    }

    @Test
    void echangerConstitutionEtIntelligence_gardeLExpertiseRetouchee_etPasseLesPVA9() {
        Build avant = guerrier(StartingEquipment.FIGHTER_A, FightingStyle.DEFENSE);
        Map<Ability, Integer> echangees = new EnumMap<>(avant.scores());
        echangees.put(Ability.CONSTITUTION, 8);
        echangees.put(Ability.INTELLIGENCE, 13);
        Build apres = new Build(avant.characterClass(), avant.method(), echangees, avant.skills(),
                avant.fightingStyle(), avant.equipment(), avant.masteries(), null, null, Set.of(), null);
        CharacterSheet fiche = construite(avant);
        Map<Skill, Proficiency> expertise = new HashMap<>(fiche.skills());
        expertise.put(Skill.PERCEPTION, Proficiency.EXPERTISE);
        fiche = retouchee(fiche, fiche.armorBonus(), fiche.hitPointMax(), expertise);

        CharacterSheet recalculee = GuidedValues.of(apres).rebuild(fiche, GuidedValues.of(avant));

        assertEquals(Proficiency.EXPERTISE, recalculee.proficiency(Skill.PERCEPTION));
        assertEquals(8, recalculee.abilities().get(Ability.CONSTITUTION));
        assertEquals(13, recalculee.abilities().get(Ability.INTELLIGENCE));
        assertEquals(9, recalculee.hitPointMax(), "d10 + Con −1");
        assertEquals(9, recalculee.hitPoints());
    }

    private static Build guerrierA(SpeciesChoice espece) {
        return guerrier(StartingEquipment.FIGHTER_A, FightingStyle.DEFENSE).withSpecies(espece);
    }

    private static CharacterSheet vitesse(CharacterSheet f, String espece, double speed) {
        return new CharacterSheet(f.characterClass(), f.fightingStyle(), f.level(), espece, f.size(), f.background(), f.abilities(),
                f.proficiencyBonus(), f.savingThrows(), f.skills(), f.armorBonus(), f.hitPointMax(), f.hitPoints(), f.hitDice(), speed,
                f.initiative(), f.otherAttacks(), f.features(), f.speciesTraits(), f.feats(), f.inventory(), f.proficiencies(),
                f.languages(), f.spellSlots());
    }

    @Test
    void unNain_ecritSonEspece_saTaille_saVitesse_sesTraits_etUnPVDePlus() {
        CharacterSheet fiche = construite(guerrierA(SpeciesChoice.of(Species.DWARF)));

        assertEquals("Nain", fiche.species());
        assertEquals("M (moyenne)", fiche.size());
        assertEquals(9, fiche.speed());
        assertEquals(12, fiche.hitPointMax(), "d10 + Con 1 + Ténacité naine 1");
        assertEquals(12, fiche.hitPoints());
        assertTrue(fiche.speciesTraits().startsWith("Vision dans le noir : 36 m"), fiche.speciesTraits());
        assertTrue(fiche.speciesTraits().contains("Résistance naine"), fiche.speciesTraits());
        assertTrue(fiche.speciesTraits().contains("Ténacité naine"), fiche.speciesTraits());
    }

    @Test
    void unElfeSylvestre_aSaVitesse_etSaCompetence() {
        CharacterSheet fiche = construite(guerrierA(
                new SpeciesChoice(Species.ELF, null, SpeciesOption.WOOD_ELF, Skill.INSIGHT, Ability.WISDOM, null)));

        assertEquals(10.5, fiche.speed());
        assertEquals(11, fiche.hitPointMax());
        assertEquals(Proficiency.PROFICIENT, fiche.proficiency(Skill.INSIGHT));
        assertTrue(fiche.speciesTraits().contains("Lignage elfique : Elfe sylvestre — Vitesse portée à 10,50 m ; sort mineur druidisme"),
                fiche.speciesTraits());
        assertTrue(fiche.speciesTraits().contains("Caractéristique d’incantation : Sagesse"), fiche.speciesTraits());
    }

    @Test
    void unGoliathSansAscendance_etUnHumainSansSousChoix_disentCeQuiResteAChoisir() {
        CharacterSheet goliath = construite(guerrierA(SpeciesChoice.of(Species.GOLIATH)));
        assertEquals(10.5, goliath.speed());
        assertTrue(goliath.speciesTraits().contains("Ascendance gigante : à choisir"), goliath.speciesTraits());

        CharacterSheet humain = construite(guerrierA(SpeciesChoice.of(Species.HUMAN)));
        assertEquals("M ou P (à choisir)", humain.size());
        assertTrue(humain.speciesTraits().contains("Compétent : une compétence au choix (à choisir)"), humain.speciesTraits());
        assertTrue(humain.speciesTraits().contains("Polyvalent : un don d’origines au choix (à choisir)"), humain.speciesTraits());
    }

    @Test
    void passerDeNainAHautElfe_gardeLaVitesseRetouchee_etRecalculeTraitsEtPV() {
        Build nain = guerrierA(SpeciesChoice.of(Species.DWARF));
        Build hautElfe = guerrierA(new SpeciesChoice(Species.ELF, null, SpeciesOption.HIGH_ELF, null, null, null));
        CharacterSheet fiche = vitesse(construite(nain), "Nain", 12);

        CharacterSheet recalculee = GuidedValues.of(hautElfe).rebuild(fiche, GuidedValues.of(nain));

        assertEquals(12, recalculee.speed(), "9 → 9 : la valeur dérivée ne change pas");
        assertEquals("Elfe", recalculee.species());
        assertEquals(11, recalculee.hitPointMax());
        assertEquals(11, recalculee.hitPoints());
        assertTrue(recalculee.speciesTraits().contains("Lignage elfique : Haut-elfe"), recalculee.speciesTraits());
    }

    @Test
    void passerDElfeSylvestreANain_reecritLaVitesse_etRetireLaCompetenceDEspece() {
        Build elfe = guerrierA(new SpeciesChoice(Species.ELF, null, SpeciesOption.WOOD_ELF, Skill.INSIGHT, null, null));
        Build nain = guerrierA(SpeciesChoice.of(Species.DWARF));

        CharacterSheet recalculee = GuidedValues.of(nain).rebuild(construite(elfe), GuidedValues.of(elfe));

        assertEquals(9, recalculee.speed());
        assertEquals(Proficiency.NONE, recalculee.proficiency(Skill.INSIGHT));
        assertEquals(Proficiency.PROFICIENT, recalculee.proficiency(Skill.PERCEPTION), "compétence de la classe gardée");
    }

    @Test
    void uneConstructionSansEspece_gardeEspeceEtVitesseSaisies() {
        CharacterSheet saisie = vitesse(CharacterSheet.blank(), "Elfe des bois", 10.5);

        CharacterSheet fiche = GuidedValues.of(guerrierA(null)).rebuild(saisie, GuidedValues.NONE);

        assertEquals("Elfe des bois", fiche.species());
        assertEquals(10.5, fiche.speed());
        assertEquals("", fiche.speciesTraits());
        assertEquals(CharacterClass.FIGHTER, fiche.characterClass());
        assertEquals(FightingStyle.DEFENSE, fiche.fightingStyle());
    }

    /** La construction suggérée d'une classe, avec ces compétences, ces Expertises et cet outil. */
    private static Build classe(CharacterClass classe, Set<Skill> competences, Set<Skill> expertise, Tool outil) {
        Build base = Build.suggested(classe);
        return new Build(classe, base.method(), base.scores(), competences, base.fightingStyle(), base.equipment(),
                base.masteries(), null, null, expertise, outil);
    }

    private static Map<Ability, Integer> valeurs(int str, int dex, int con, int intel, int wis, int cha) {
        Map<Ability, Integer> valeurs = new EnumMap<>(Ability.class);
        valeurs.put(Ability.STRENGTH, str);
        valeurs.put(Ability.DEXTERITY, dex);
        valeurs.put(Ability.CONSTITUTION, con);
        valeurs.put(Ability.INTELLIGENCE, intel);
        valeurs.put(Ability.WISDOM, wis);
        valeurs.put(Ability.CHARISMA, cha);
        return valeurs;
    }

    private static Build achatDePoints(Build build, Map<Ability, Integer> valeurs) {
        return new Build(build.characterClass(), AbilityMethod.POINT_BUY, valeurs, build.skills(), build.fightingStyle(),
                build.equipment(), build.masteries(), build.species(), build.background(), build.expertise(), build.tool());
    }

    private static Build equipe(Build build, StartingEquipment equipement) {
        return new Build(build.characterClass(), build.method(), build.scores(), build.skills(), build.fightingStyle(),
                equipement, equipement.weapons().stream().filter(build.characterClass()::masters)
                        .limit(build.characterClass().masteryCount()).toList(),
                build.species(), build.background(), build.expertise(), build.tool());
    }

    @Test
    void unBarbare_aSaDefenseSansArmure_saRage_etSesArmes() {
        CharacterSheet fiche = construite(classe(CharacterClass.BARBARIAN, ATHLETISME_PERCEPTION, Set.of(), null));

        assertEquals(CharacterClass.BARBARIAN, fiche.characterClass());
        assertEquals(null, fiche.fightingStyle());
        assertEquals(14, fiche.hitPointMax(), "d12 + Con 2");
        assertEquals("1d12", fiche.hitDice());
        assertEquals(Set.of(Ability.STRENGTH, Ability.CONSTITUTION), fiche.savingThrows());
        assertEquals(13, fiche.armorClass(), "sans armure : 10 + Dex 1 + Con 2");
        assertEquals(List.of(
                new SheetAttack("Hache à deux mains", 4, 1, 12, 2, DamageType.SLASHING, 1.5, null, null),
                new SheetAttack("Hachette", 4, 1, 6, 2, DamageType.SLASHING, 1.5, 6.0, 18.0),
                new SheetAttack("Attaque à mains nues", 4, 0, 4, 3, DamageType.BLUDGEONING, 1.5, null, null)), fiche.attacks());
        assertTrue(fiche.features().startsWith(
                "Bottes d’arme : Hache à deux mains (Enchaînement), Hachette (Ouverture)\nRage (2 utilisations, 1 au repos court, toutes au repos long ; dégâts +2)"), fiche.features());
        assertTrue(fiche.features().contains("Défense sans armure : sans armure, CA 10 + Dex + Con ; bouclier permis"), fiche.features());
        assertFalse(fiche.features().contains("Style de combat"), fiche.features());
        assertEquals("Hache à deux mains, Hachette ×4, Paquetage d’explorateur, 15 po", fiche.inventory().text());
        assertFalse(fiche.proficiencies().contains("Outils"), fiche.proficiencies());
    }

    @Test
    void unBarbareMalAgile_garde10PlusDexPlusCon() {
        Build barbare = achatDePoints(classe(CharacterClass.BARBARIAN, ATHLETISME_PERCEPTION, Set.of(), null),
                valeurs(15, 8, 14, 8, 12, 10));

        assertEquals(11, construite(barbare).armorClass(), "10 − 1 + 2");
    }

    @Test
    void unMoine_frappeAvecArtsMartiaux_etChoisitSonOutil() {
        CharacterSheet fiche = construite(classe(CharacterClass.MONK, Set.of(Skill.ACROBATICS, Skill.STEALTH), Set.of(), Tool.LUTE));

        assertEquals(9, fiche.hitPointMax(), "d8 + Con 1");
        assertEquals(Set.of(Ability.STRENGTH, Ability.DEXTERITY), fiche.savingThrows());
        assertEquals(14, fiche.armorClass(), "10 + Dex 2 + Sag 2");
        assertEquals(List.of(
                new SheetAttack("Lance", 4, 1, 6, 2, DamageType.PIERCING, 1.5, 6.0, 18.0),
                new SheetAttack("Dague", 4, 1, 6, 2, DamageType.PIERCING, 1.5, 6.0, 18.0),
                new SheetAttack("Attaque à mains nues", 4, 1, 6, 2, DamageType.BLUDGEONING, 1.5, null, null)), fiche.attacks());
        assertTrue(fiche.proficiencies().endsWith("\nOutils : luth"), fiche.proficiencies());
        assertEquals("Lance, Dague ×5, luth, Paquetage d’explorateur, 11 po", fiche.inventory().text());
        assertTrue(fiche.features().startsWith("Arts martiaux (1d6)"), fiche.features());
        assertFalse(fiche.features().contains("Bottes"), fiche.features());
    }

    @Test
    void unMoineFort_frappeAvecSaForce() {
        Build moine = achatDePoints(classe(CharacterClass.MONK, Set.of(), Set.of(), null), valeurs(15, 13, 13, 8, 12, 8));
        CharacterSheet fiche = construite(moine);

        assertEquals(new SheetAttack("Lance", 4, 1, 6, 2, DamageType.PIERCING, 1.5, 6.0, 18.0), fiche.attacks().get(0));
        assertEquals(new SheetAttack("Attaque à mains nues", 4, 1, 6, 2, DamageType.BLUDGEONING, 1.5, null, null),
                fiche.attacks().get(2), "For 2 > Dex 1");
    }

    @Test
    void unMoineSansOutil_lEcritAChoisir() {
        CharacterSheet fiche = construite(classe(CharacterClass.MONK, Set.of(), Set.of(), null));

        assertTrue(fiche.proficiencies().endsWith("\nOutils : outil d’artisan ou instrument de musique (à choisir)"),
                fiche.proficiencies());
        assertTrue(fiche.inventory().text().contains("outil d’artisan ou instrument de musique (à choisir)"), fiche.inventory().text());
    }

    @Test
    void unRoublard_aSonExpertise_sonArmureDeCuir_etSesOutilsDeVoleur() {
        Set<Skill> competences = Set.of(Skill.STEALTH, Skill.SLEIGHT_OF_HAND, Skill.PERCEPTION, Skill.DECEPTION);
        CharacterSheet fiche = construite(classe(CharacterClass.ROGUE, competences,
                Set.of(Skill.STEALTH, Skill.SLEIGHT_OF_HAND), null));

        assertEquals(9, fiche.hitPointMax());
        assertEquals(Set.of(Ability.DEXTERITY, Ability.INTELLIGENCE), fiche.savingThrows());
        assertEquals(13, fiche.armorClass(), "cuir 11 + Dex 2");
        assertEquals(Proficiency.EXPERTISE, fiche.proficiency(Skill.STEALTH));
        assertEquals(Proficiency.EXPERTISE, fiche.proficiency(Skill.SLEIGHT_OF_HAND));
        assertEquals(Proficiency.PROFICIENT, fiche.proficiency(Skill.PERCEPTION));
        assertEquals(List.of(
                new SheetAttack("Dague", 4, 1, 4, 2, DamageType.PIERCING, 1.5, 6.0, 18.0),
                new SheetAttack("Épée courte", 4, 1, 6, 2, DamageType.PIERCING, 1.5, null, null),
                new SheetAttack("Arc court", 4, 1, 6, 2, DamageType.PIERCING, null, 24.0, 96.0),
                new SheetAttack("Attaque à mains nues", 3, 0, 4, 2, DamageType.BLUDGEONING, 1.5, null, null)), fiche.attacks());
        assertTrue(fiche.proficiencies().endsWith("\nOutils : outils de voleur"), fiche.proficiencies());
        assertTrue(fiche.features().contains("Expertise : Discrétion, Escamotage"), fiche.features());
        assertTrue(fiche.features().contains("Attaque sournoise (1d6)"), fiche.features());
        assertTrue(fiche.features().contains("Argot des voleurs"), fiche.features());
    }

    @Test
    void unRoublardCriminel_nAQuUneFoisSesOutilsDeVoleur() {
        Build roublard = classe(CharacterClass.ROGUE, Set.of(Skill.PERCEPTION), Set.of(), null)
                .withBackground(BackgroundChoice.of(Background.CRIMINAL));

        assertTrue(construite(roublard).proficiencies().endsWith("\nOutils : outils de voleur"),
                construite(roublard).proficiencies());
    }

    @Test
    void lOptionToutEnPieces_neDonneNiArmureNiArme() {
        CharacterSheet roublard = construite(equipe(classe(CharacterClass.ROGUE, Set.of(), Set.of(), null), StartingEquipment.ROGUE_B));
        assertEquals("100 po", roublard.inventory().text());
        assertEquals(12, roublard.armorClass(), "10 + Dex 2");
        assertEquals(List.of("Attaque à mains nues"), roublard.attacks().stream().map(SheetAttack::name).toList());

        CharacterSheet guerrier = construite(equipe(guerrier(StartingEquipment.FIGHTER_A, FightingStyle.DEFENSE),
                StartingEquipment.FIGHTER_C));
        assertEquals("155 po", guerrier.inventory().text());
        assertEquals(12, guerrier.armorClass(), "10 + Dex 2, Défense sans armure ne compte pas");
    }

    @Test
    void passerDeGuerrierAMoine_retireLeStyle_etGardeLesLangues() {
        Build avant = guerrier(StartingEquipment.FIGHTER_A, FightingStyle.DEFENSE);
        CharacterSheet fiche = construite(avant);
        CharacterSheet retouchee = new CharacterSheet(fiche.characterClass(), fiche.fightingStyle(), fiche.level(), fiche.species(),
                fiche.size(), fiche.background(), fiche.abilities(), fiche.proficiencyBonus(), fiche.savingThrows(), fiche.skills(),
                fiche.armorBonus(), fiche.hitPointMax(), fiche.hitPoints(), fiche.hitDice(), fiche.speed(), fiche.initiative(),
                fiche.otherAttacks(), fiche.features(), fiche.speciesTraits(), fiche.feats(), fiche.inventory(),
                fiche.proficiencies(), "Commun, Elfique", fiche.spellSlots());
        Build moine = classe(CharacterClass.MONK, Set.of(Skill.ACROBATICS, Skill.STEALTH), Set.of(), null);

        CharacterSheet apres = new SheetState(retouchee, avant, avant.abilities(), null).rebuild(moine);

        assertEquals(CharacterClass.MONK, apres.characterClass());
        assertEquals(null, apres.fightingStyle());
        assertFalse(apres.features().contains("Style de combat"), apres.features());
        assertEquals(14, apres.armorClass());
        assertEquals(9, apres.hitPointMax());
        assertEquals(Set.of(Ability.STRENGTH, Ability.DEXTERITY), apres.savingThrows());
        assertEquals("Commun, Elfique", apres.languages());
        assertEquals(Proficiency.NONE, apres.proficiency(Skill.ATHLETICS));
    }
}
