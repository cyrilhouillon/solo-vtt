package dnd.engine.sheet;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackgroundChoiceTest {

    private static final Set<Skill> ACROBATIES_PERCEPTION = Set.of(Skill.ACROBATICS, Skill.PERCEPTION);

    /** Guerrier For 15, Dex 14, Con 13, Int 8, Sag 10, Cha 12, équipement A, Défense, Acrobaties et Perception. */
    private static Build guerrier(Set<Skill> competences) {
        Build base = Build.fighter();
        return new Build(base.characterClass(), base.method(), base.scores(), competences, base.fightingStyle(),
                base.equipment(), base.masteries(), null, null);
    }

    private static Map<Ability, Integer> augmentations(Object... paires) {
        Map<Ability, Integer> map = new EnumMap<>(Ability.class);
        for (int i = 0; i < paires.length; i += 2) map.put((Ability) paires[i], (Integer) paires[i + 1]);
        return map;
    }

    private static BackgroundChoice soldat(BackgroundEquipment equipement, GamingSet boite) {
        return new BackgroundChoice(Background.SOLDIER, augmentations(Ability.STRENGTH, 2, Ability.CONSTITUTION, 1),
                equipement, boite, null);
    }

    private static CharacterSheet construite(Build build) {
        build.validate(List.of());
        return SheetState.blank().rebuild(build);
    }

    private static String refus(Build build) {
        return assertThrows(IllegalArgumentException.class, () -> build.validate(List.of())).getMessage();
    }

    private static List<String> attaques(CharacterSheet fiche) {
        return fiche.attacks().stream().map(SheetAttack::name).toList();
    }

    @Test
    void lesQuatreHistoriques_ontLeursDonneesDuSrd() {
        assertEquals(Set.of(Ability.INTELLIGENCE, Ability.WISDOM, Ability.CHARISMA), Background.ACOLYTE.abilities());
        assertEquals(OriginFeat.MAGIC_INITIATE, Background.ACOLYTE.feat());
        assertEquals(SpellList.CLERIC, Background.ACOLYTE.spellList());
        assertEquals(Set.of(Skill.INSIGHT, Skill.RELIGION), Background.ACOLYTE.skills());
        assertEquals(OriginFeat.ALERT, Background.CRIMINAL.feat());
        assertEquals("outils de voleur", Background.CRIMINAL.tool());
        assertEquals(SpellList.WIZARD, Background.SAGE.spellList());
        assertEquals(Set.of(Skill.ARCANA, Skill.HISTORY), Background.SAGE.skills());
        assertEquals(OriginFeat.SAVAGE_ATTACKER, Background.SOLDIER.feat());
        assertTrue(Background.SOLDIER.choosesGamingSet());
        assertEquals("50 po", Background.SAGE.equipmentText(BackgroundEquipment.B));
        assertTrue(Background.CRIMINAL.equipmentText(BackgroundEquipment.A).endsWith("tenue de voyage, 16 po"));
    }

    @Test
    void unSoldat_augmenteForceEtConstitution_etRemplitLaFiche() {
        CharacterSheet fiche = construite(guerrier(ACROBATIES_PERCEPTION).withBackground(soldat(BackgroundEquipment.A, GamingSet.DICE)));

        assertEquals("Soldat", fiche.background());
        assertEquals(17, fiche.abilities().get(Ability.STRENGTH));
        assertEquals(14, fiche.abilities().get(Ability.CONSTITUTION));
        assertEquals(12, fiche.hitPointMax(), "d10 + Con 2");
        assertEquals(Proficiency.PROFICIENT, fiche.proficiency(Skill.ATHLETICS));
        assertEquals(Proficiency.PROFICIENT, fiche.proficiency(Skill.INTIMIDATION));
        assertTrue(fiche.proficiencies().endsWith("\nOutils : boîte de jeux (dés)"), fiche.proficiencies());
        assertTrue(fiche.equipment().endsWith("\nArc court, 20 flèches, carquois, lance, boîte de jeux (même type), "
                + "trousse de soins, tenue de voyage, 14 po"), fiche.equipment());
        assertEquals(List.of("Épée à deux mains", "Fléau d’armes", "Javeline", "Arc court", "Lance"), attaques(fiche));
        assertEquals(5, fiche.attacks().get(0).bonus(), "maîtrise 2 + For 3");
        assertTrue(fiche.feats().startsWith("Sauvagerie martiale — "), fiche.feats());
        assertEquals(2, fiche.initiative());
    }

    @Test
    void unCriminel_estVigilant_etNeDoublePasSaDague() {
        Build build = guerrier(ACROBATIES_PERCEPTION).withBackground(new BackgroundChoice(Background.CRIMINAL,
                augmentations(Ability.DEXTERITY, 1, Ability.CONSTITUTION, 1, Ability.INTELLIGENCE, 1),
                BackgroundEquipment.A, null, null));
        CharacterSheet fiche = construite(build);

        assertEquals(4, fiche.initiative(), "Dex 15 : +2, et bonus de maîtrise 2");
        assertEquals(1, attaques(fiche).stream().filter("Dague"::equals).count());
        assertEquals(Proficiency.PROFICIENT, fiche.proficiency(Skill.STEALTH));
        assertEquals(Proficiency.PROFICIENT, fiche.proficiency(Skill.SLEIGHT_OF_HAND));
        assertTrue(fiche.proficiencies().endsWith("\nOutils : outils de voleur"), fiche.proficiencies());
    }

    @Test
    void unAcolyteAvecLEquipementB_aSonDonEtCinquantePo_sansAttaqueDePlus() {
        CharacterSheet fiche = construite(guerrier(ACROBATIES_PERCEPTION).withBackground(new BackgroundChoice(Background.ACOLYTE,
                augmentations(Ability.WISDOM, 2, Ability.CHARISMA, 1), BackgroundEquipment.B, null, Ability.WISDOM)));

        assertEquals("Initié à la magie (Clerc) — incantation Sagesse ; 2 sorts mineurs et 1 sort du 1er niveau (à choisir)",
                fiche.feats());
        assertTrue(fiche.equipment().endsWith("\n50 po"), fiche.equipment());
        assertEquals(3, fiche.attacks().size());
        assertEquals(12, fiche.abilities().get(Ability.WISDOM));
    }

    @Test
    void unSoldatSansBoite_laLaisseAChoisir() {
        CharacterSheet fiche = construite(guerrier(ACROBATIES_PERCEPTION).withBackground(soldat(BackgroundEquipment.A, null)));

        assertTrue(fiche.proficiencies().endsWith("Outils : boîte de jeux (à choisir)"), fiche.proficiencies());
    }

    @Test
    void unHumainPolyvalent_choisitSonDon() {
        OriginFeatChoice doue = new OriginFeatChoice(OriginFeat.SKILLED, null, null, Set.of(Skill.HISTORY, Skill.MEDICINE, Skill.RELIGION));
        Build build = guerrier(ACROBATIES_PERCEPTION)
                .withSpecies(SpeciesChoice.of(Species.HUMAN).withOriginFeat(doue))
                .withBackground(soldat(BackgroundEquipment.A, GamingSet.DICE));
        CharacterSheet fiche = construite(build);

        assertEquals(Proficiency.PROFICIENT, fiche.proficiency(Skill.HISTORY));
        assertEquals(Proficiency.PROFICIENT, fiche.proficiency(Skill.MEDICINE));
        assertEquals(Proficiency.PROFICIENT, fiche.proficiency(Skill.RELIGION));
        assertTrue(fiche.feats().endsWith("\nDoué : maîtrise de Histoire, Médecine, Religion"), fiche.feats());
        assertTrue(fiche.speciesTraits().contains("Polyvalent : Doué"), fiche.speciesTraits());

        CharacterSheet vigilant = construite(guerrier(ACROBATIES_PERCEPTION)
                .withSpecies(SpeciesChoice.of(Species.HUMAN).withOriginFeat(OriginFeatChoice.of(OriginFeat.ALERT))));
        assertEquals(4, vigilant.initiative());
        assertEquals("Vigilant — " + OriginFeat.ALERT.text(), vigilant.feats());
    }

    @Test
    void passerDeSoldatASage_recalculeCeQuiEnDerive_etGardeUneRetoucheInchangee() {
        Build soldat = guerrier(ACROBATIES_PERCEPTION).withBackground(soldat(BackgroundEquipment.A, GamingSet.DICE));
        Build sage = guerrier(ACROBATIES_PERCEPTION).withBackground(new BackgroundChoice(Background.SAGE,
                augmentations(Ability.CONSTITUTION, 2, Ability.INTELLIGENCE, 1), BackgroundEquipment.A, null, Ability.INTELLIGENCE));
        CharacterSheet fiche = construite(soldat);
        CharacterSheet retouchee = new CharacterSheet(fiche.className(), fiche.level(), fiche.species(), fiche.size(),
                fiche.background(), fiche.abilities(), fiche.proficiencyBonus(), fiche.savingThrows(), fiche.skills(),
                19, fiche.hitPointMax(), fiche.hitPoints(), fiche.hitDice(), fiche.speed(), fiche.initiative(), fiche.attacks(),
                fiche.features(), fiche.speciesTraits(), fiche.feats(), fiche.equipment(), fiche.proficiencies(),
                fiche.languages(), fiche.spellSlots());
        SheetState etat = SheetHistory.replay(List.of(new SheetEvent.SheetCreated(),
                new SheetEvent.SheetBuilt(soldat, fiche), new SheetEvent.SheetEdited(retouchee)));

        CharacterSheet recalculee = etat.rebuild(sage);

        assertEquals("Sage", recalculee.background());
        assertEquals(15, recalculee.abilities().get(Ability.STRENGTH));
        assertEquals(15, recalculee.abilities().get(Ability.CONSTITUTION));
        assertEquals(9, recalculee.abilities().get(Ability.INTELLIGENCE));
        assertEquals(12, recalculee.hitPointMax(), "d10 + Con 2, inchangé");
        assertEquals(Proficiency.NONE, recalculee.proficiency(Skill.ATHLETICS));
        assertEquals(Proficiency.PROFICIENT, recalculee.proficiency(Skill.ARCANA));
        assertEquals(Proficiency.PROFICIENT, recalculee.proficiency(Skill.HISTORY));
        assertEquals(List.of("Épée à deux mains", "Fléau d’armes", "Javeline", "Bâton de combat"), attaques(recalculee));
        assertTrue(recalculee.feats().startsWith("Initié à la magie (Magicien) — incantation Intelligence"), recalculee.feats());
        assertEquals(19, recalculee.armorClass(), "CA dérivée 17 → 17 : la retouche reste");
    }

    @Test
    void unHistoriqueSaisiALaMain_resteSansHistoriqueDansLaConstruction() {
        CharacterSheet saisie = new CharacterSheet("", 1, "", "", "Marin", CharacterSheet.blank().abilities(), 2, Set.of(), Map.of(),
                10, 0, 0, "", 9, 0, List.of(), "", "", "Chanceux", "", "", "", List.of());
        SheetState etat = SheetHistory.replay(List.of(new SheetEvent.SheetCreated(), new SheetEvent.SheetEdited(saisie)));

        CharacterSheet construite = etat.rebuild(guerrier(ACROBATIES_PERCEPTION));

        assertEquals("Marin", construite.background());
        assertEquals("Chanceux", construite.feats());
    }

    @Test
    void unHistoriqueChoisi_seChangeMaisNeSeRetirePas() {
        Build soldat = guerrier(ACROBATIES_PERCEPTION).withBackground(soldat(BackgroundEquipment.A, GamingSet.DICE));
        SheetState etat = SheetHistory.replay(List.of(new SheetEvent.SheetCreated(),
                new SheetEvent.SheetBuilt(soldat, construite(soldat))));

        var refus = assertThrows(IllegalArgumentException.class, () -> etat.rebuild(guerrier(ACROBATIES_PERCEPTION)));
        assertEquals("Historique : il se change, mais ne se retire pas", refus.getMessage());
    }

    @Test
    void desAugmentationsImpossibles_sontRefuseesAvecLaRaison() {
        Build base = guerrier(ACROBATIES_PERCEPTION);
        assertEquals("Soldat : Intelligence n’est pas une caractéristique de l’historique", refus(base.withBackground(
                new BackgroundChoice(Background.SOLDIER, augmentations(Ability.INTELLIGENCE, 2, Ability.STRENGTH, 1),
                        BackgroundEquipment.A, null, null))));
        String deuxEtDeux = "Soldat : augmenter une caractéristique de 2 et une autre de 1, ou les trois de 1";
        assertEquals(deuxEtDeux, refus(base.withBackground(new BackgroundChoice(Background.SOLDIER,
                augmentations(Ability.STRENGTH, 2, Ability.DEXTERITY, 2), BackgroundEquipment.A, null, null))));
        assertEquals(deuxEtDeux, refus(base.withBackground(new BackgroundChoice(Background.SOLDIER,
                augmentations(Ability.STRENGTH, 3), BackgroundEquipment.A, null, null))));
        assertEquals(deuxEtDeux, refus(base.withBackground(new BackgroundChoice(Background.SOLDIER,
                augmentations(Ability.STRENGTH, 1, Ability.DEXTERITY, 1), BackgroundEquipment.A, null, null))));
        assertEquals(deuxEtDeux, refus(base.withBackground(new BackgroundChoice(Background.SOLDIER,
                Map.of(), BackgroundEquipment.A, null, null))));
    }

    @Test
    void desSousChoixImpossibles_sontRefusesAvecLaRaison() {
        Build base = guerrier(ACROBATIES_PERCEPTION);
        Map<Ability, Integer> acolyte = augmentations(Ability.WISDOM, 2, Ability.CHARISMA, 1);
        assertEquals("Acolyte : aucune boîte de jeux à choisir", refus(base.withBackground(
                new BackgroundChoice(Background.ACOLYTE, acolyte, BackgroundEquipment.A, GamingSet.DICE, null))));
        assertEquals("Caractéristique d’incantation : Intelligence, Sagesse ou Charisme", refus(base.withBackground(
                new BackgroundChoice(Background.ACOLYTE, acolyte, BackgroundEquipment.A, null, Ability.STRENGTH))));
        assertEquals("Vigilant : aucune caractéristique d’incantation à choisir", refus(base.withBackground(
                new BackgroundChoice(Background.CRIMINAL, augmentations(Ability.DEXTERITY, 2, Ability.CONSTITUTION, 1),
                        BackgroundEquipment.A, null, Ability.WISDOM))));
        assertEquals("Acolyte : choisir l’équipement A ou B", refus(base.withBackground(
                new BackgroundChoice(Background.ACOLYTE, acolyte, null, null, null))));
        assertEquals("Choisir un historique", refus(base.withBackground(
                new BackgroundChoice(null, acolyte, BackgroundEquipment.A, null, null))));
        assertEquals("Nain : aucun don d’origines à choisir", refus(base.withSpecies(
                SpeciesChoice.of(Species.DWARF).withOriginFeat(OriginFeatChoice.of(OriginFeat.ALERT)))));
    }

    @Test
    void uneCompetenceDejaDonneeParLHistorique_neSeChoisitPasAilleurs() {
        BackgroundChoice soldat = soldat(BackgroundEquipment.A, GamingSet.DICE);
        assertEquals("Athlétisme déjà donnée par l’historique",
                refus(guerrier(Set.of(Skill.ATHLETICS, Skill.PERCEPTION)).withBackground(soldat)));
        assertEquals("Athlétisme déjà donnée par l’historique", refus(guerrier(ACROBATIES_PERCEPTION).withBackground(soldat)
                .withSpecies(new SpeciesChoice(Species.HUMAN, null, null, Skill.ATHLETICS, null, null))));
        assertEquals("Doué : Perception déjà maîtrisée", refus(guerrier(ACROBATIES_PERCEPTION).withSpecies(SpeciesChoice.of(Species.HUMAN)
                .withOriginFeat(new OriginFeatChoice(OriginFeat.SKILLED, null, null, Set.of(Skill.PERCEPTION))))));
        assertEquals("Doué : Intimidation déjà maîtrisée", refus(guerrier(ACROBATIES_PERCEPTION).withBackground(soldat)
                .withSpecies(SpeciesChoice.of(Species.HUMAN)
                        .withOriginFeat(new OriginFeatChoice(OriginFeat.SKILLED, null, null, Set.of(Skill.INTIMIDATION))))));
        assertEquals("Doué : Histoire déjà maîtrisée", refus(guerrier(ACROBATIES_PERCEPTION).withSpecies(
                new SpeciesChoice(Species.HUMAN, null, null, Skill.HISTORY, null,
                        new OriginFeatChoice(OriginFeat.SKILLED, null, null, Set.of(Skill.HISTORY))))));
        assertDoesNotThrow(() -> guerrier(ACROBATIES_PERCEPTION).withBackground(soldat).validate(List.of()));
        assertFalse(Build.fighter().withBackground(BackgroundChoice.of(Background.SAGE)).abilities().isEmpty());
    }
    @Test
    void polyvalent_neReprendPasLeDonDeLHistorique_saufDoueOuUneAutreListe() {
        Build base = guerrier(ACROBATIES_PERCEPTION);
        BackgroundChoice criminel = new BackgroundChoice(Background.CRIMINAL,
                augmentations(Ability.DEXTERITY, 2, Ability.CONSTITUTION, 1), BackgroundEquipment.A, null, null);
        assertEquals("Polyvalent : Vigilant déjà donné par l’historique", refus(base.withBackground(criminel)
                .withSpecies(SpeciesChoice.of(Species.HUMAN).withOriginFeat(OriginFeatChoice.of(OriginFeat.ALERT)))));
        assertEquals("Polyvalent : Sauvagerie martiale déjà donné par l’historique", refus(base
                .withBackground(soldat(BackgroundEquipment.A, GamingSet.DICE))
                .withSpecies(SpeciesChoice.of(Species.HUMAN).withOriginFeat(OriginFeatChoice.of(OriginFeat.SAVAGE_ATTACKER)))));

        BackgroundChoice acolyte = new BackgroundChoice(Background.ACOLYTE,
                augmentations(Ability.WISDOM, 2, Ability.CHARISMA, 1), BackgroundEquipment.A, null, null);
        assertEquals("Polyvalent : Initié à la magie (Clerc) déjà donné par l’historique ; choisir une autre liste",
                refus(base.withBackground(acolyte).withSpecies(SpeciesChoice.of(Species.HUMAN).withOriginFeat(
                        new OriginFeatChoice(OriginFeat.MAGIC_INITIATE, SpellList.CLERIC, null, Set.of())))));
        assertDoesNotThrow(() -> base.withBackground(acolyte).withSpecies(SpeciesChoice.of(Species.HUMAN).withOriginFeat(
                new OriginFeatChoice(OriginFeat.MAGIC_INITIATE, SpellList.DRUID, null, Set.of()))).validate(List.of()));
        assertDoesNotThrow(() -> base.withBackground(acolyte).withSpecies(SpeciesChoice.of(Species.HUMAN).withOriginFeat(
                OriginFeatChoice.of(OriginFeat.MAGIC_INITIATE))).validate(List.of()));
    }

    @Test
    void unDonDePolyvalentRetire_videLesDons_commeLInitiative() {
        Build vigilant = guerrier(ACROBATIES_PERCEPTION)
                .withSpecies(SpeciesChoice.of(Species.HUMAN).withOriginFeat(OriginFeatChoice.of(OriginFeat.ALERT)));
        SheetState etat = SheetHistory.replay(List.of(new SheetEvent.SheetCreated(),
                new SheetEvent.SheetBuilt(vigilant, construite(vigilant))));

        for (Build sansDon : List.of(guerrier(ACROBATIES_PERCEPTION).withSpecies(SpeciesChoice.of(Species.HUMAN)),
                guerrier(ACROBATIES_PERCEPTION).withSpecies(SpeciesChoice.of(Species.DWARF)))) {
            CharacterSheet fiche = etat.rebuild(sansDon);
            assertEquals("", fiche.feats());
            assertEquals(2, fiche.initiative());
        }
    }
}
