package dnd.engine.sheet;

import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildTest {

    private static Map<Ability, Integer> valeurs(int force, int dex, int con, int intel, int sag, int cha) {
        Map<Ability, Integer> abilities = new EnumMap<>(Ability.class);
        abilities.put(Ability.STRENGTH, force);
        abilities.put(Ability.DEXTERITY, dex);
        abilities.put(Ability.CONSTITUTION, con);
        abilities.put(Ability.INTELLIGENCE, intel);
        abilities.put(Ability.WISDOM, sag);
        abilities.put(Ability.CHARISMA, cha);
        return abilities;
    }

    private static Build avec(AbilityMethod method, Map<Ability, Integer> scores) {
        Build guerrier = Build.fighter();
        return new Build(guerrier.characterClass(), method, scores, guerrier.skills(), guerrier.fightingStyle(),
                guerrier.equipment(), guerrier.masteries(), null);
    }

    private static Build avecCompetences(Skill... skills) {
        Build guerrier = Build.fighter();
        return new Build(guerrier.characterClass(), guerrier.method(), guerrier.scores(), Set.of(skills),
                guerrier.fightingStyle(), guerrier.equipment(), guerrier.masteries(), null);
    }

    private static Build avecBottes(SrdWeapon... armes) {
        Build guerrier = Build.fighter();
        return new Build(guerrier.characterClass(), guerrier.method(), guerrier.scores(), guerrier.skills(),
                guerrier.fightingStyle(), guerrier.equipment(), List.of(armes), null);
    }

    private static String raison(Build build, List<AbilityRoll> jets) {
        return assertThrows(IllegalArgumentException.class, () -> build.validate(jets)).getMessage();
    }

    @Test
    void leGuerrierSuggere_estValide_avecAthletismeEtPerception() {
        Build guerrier = avecCompetences(Skill.ATHLETICS, Skill.PERCEPTION);

        assertEquals(valeurs(15, 14, 13, 8, 10, 12), guerrier.scores());
        assertEquals(AbilityMethod.STANDARD_ARRAY, guerrier.method());
        assertEquals(FightingStyle.DEFENSE, guerrier.fightingStyle());
        assertEquals(StartingEquipment.FIGHTER_A, guerrier.equipment());
        assertEquals(List.of(SrdWeapon.GREATSWORD, SrdWeapon.FLAIL, SrdWeapon.JAVELIN), guerrier.masteries());
        assertDoesNotThrow(() -> guerrier.validate(List.of()));
        assertDoesNotThrow(() -> Build.fighter().validate(null), "les compétences restent à choisir");
    }

    @Test
    void standard_quiNEstPasUnePermutationDu15_14_13_12_10_8_estRefuse() {
        String raison = raison(avec(AbilityMethod.STANDARD_ARRAY, valeurs(15, 15, 13, 12, 10, 8)), List.of());

        assertTrue(raison.contains("15, 14, 13, 12, 10 et 8"), raison);
    }

    @Test
    void achatDePoints_aPlusDe27Points_estRefuseAvecLeCout() {
        assertDoesNotThrow(() -> avec(AbilityMethod.POINT_BUY, valeurs(15, 14, 13, 8, 10, 12)).validate(List.of()),
                "la répartition suggérée coûte 27 points");

        String raison = raison(avec(AbilityMethod.POINT_BUY, valeurs(15, 14, 13, 8, 10, 13)), List.of());

        assertTrue(raison.contains("28 points"), raison);
    }

    @Test
    void achatDePoints_horsDe8A15_estRefuse() {
        String raison = raison(avec(AbilityMethod.POINT_BUY, valeurs(16, 8, 8, 8, 8, 8)), List.of());

        assertTrue(raison.contains("entre 8 et 15"), raison);
        assertThrows(IllegalArgumentException.class,
                () -> avec(AbilityMethod.POINT_BUY, valeurs(7, 8, 8, 8, 8, 8)).validate(List.of()));
    }

    @Test
    void des_sansJets_ouValeursQuiNeSontPasLesTotaux_sontRefuses() {
        Map<Ability, Integer> totaux = valeurs(18, 18, 18, 18, 18, 18);
        List<AbilityRoll> jets = new ArrayList<>();
        for (int i = 0; i < 6; i++) jets.add(new AbilityRoll(List.of(6, 6, 6, 1)));

        assertThrows(IllegalArgumentException.class, () -> avec(AbilityMethod.ROLLED, totaux).validate(List.of()));
        assertThrows(IllegalArgumentException.class, () -> avec(AbilityMethod.ROLLED, totaux).validate(null));
        String raison = raison(avec(AbilityMethod.ROLLED, valeurs(18, 18, 18, 18, 18, 17)), jets);
        assertTrue(raison.contains("totaux"), raison);
        assertDoesNotThrow(() -> avec(AbilityMethod.ROLLED, totaux).validate(jets));
    }

    @Test
    void troisCompetences_ouUneHorsDeLaListeDuGuerrier_sontRefusees() {
        String trop = raison(avecCompetences(Skill.ATHLETICS, Skill.PERCEPTION, Skill.SURVIVAL), List.of());
        String horsListe = raison(avecCompetences(Skill.ATHLETICS, Skill.ARCANA), List.of());

        assertTrue(trop.contains("2 compétences au plus"), trop);
        assertTrue(horsListe.contains("Arcanes"), horsListe);
    }

    @Test
    void deuxBottesIdentiques_ouQuatreBottes_sontRefusees() {
        String doublon = raison(avecBottes(SrdWeapon.GREATSWORD, SrdWeapon.GREATSWORD, SrdWeapon.JAVELIN), List.of());
        String quatre = raison(avecBottes(SrdWeapon.GREATSWORD, SrdWeapon.FLAIL, SrdWeapon.JAVELIN, SrdWeapon.LONGBOW), List.of());

        assertTrue(doublon.contains("3 types d'arme différents"), doublon);
        assertTrue(quatre.contains("3 types d'arme différents"), quatre);
    }

    @Test
    void desScriptes_6_6_6_1_sixFois_donnentSixJetsDe18_le1Ecarte() {
        Dice des = Dice.scripted(6, 6, 6, 1, 6, 6, 6, 1, 6, 6, 6, 1, 6, 6, 6, 1, 6, 6, 6, 1, 6, 6, 6, 1);

        for (int i = 0; i < 6; i++) {
            AbilityRoll jet = AbilityRoll.roll(des);
            assertEquals(List.of(6, 6, 6, 1), jet.dice());
            assertEquals(18, jet.total());
        }
    }

    @Test
    void uneEspeceImpossible_refuseLaConstruction() {
        Build guerrier = avecCompetences(Skill.ATHLETICS, Skill.PERCEPTION);
        assertDoesNotThrow(() -> guerrier.withSpecies(SpeciesChoice.of(Species.ELF)).validate(List.of()));
        String raison = raison(guerrier.withSpecies(new SpeciesChoice(Species.ELF, null, null, Skill.PERCEPTION, null)), List.of());
        assertEquals("Perception déjà maîtrisée par la classe", raison);
    }
}
