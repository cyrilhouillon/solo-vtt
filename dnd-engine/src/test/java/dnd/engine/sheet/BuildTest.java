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
                guerrier.equipment(), guerrier.masteries(), null, null, Set.of(), null);
    }

    private static Build avecCompetences(Skill... skills) {
        Build guerrier = Build.fighter();
        return new Build(guerrier.characterClass(), guerrier.method(), guerrier.scores(), Set.of(skills),
                guerrier.fightingStyle(), guerrier.equipment(), guerrier.masteries(), null, null, Set.of(), null);
    }

    private static Build avecBottes(SrdWeapon... armes) {
        Build guerrier = Build.fighter();
        return new Build(guerrier.characterClass(), guerrier.method(), guerrier.scores(), guerrier.skills(),
                guerrier.fightingStyle(), guerrier.equipment(), List.of(armes), null, null, Set.of(), null);
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
        String raison = raison(guerrier.withSpecies(new SpeciesChoice(Species.ELF, null, null, Skill.PERCEPTION, null, null)), List.of());
        assertEquals("Perception déjà maîtrisée par la classe", raison);
    }

    private static Build de(CharacterClass classe, FightingStyle style, List<SrdWeapon> bottes, Set<Skill> competences,
                            Set<Skill> expertise, Tool outil) {
        Build base = Build.suggested(classe);
        return new Build(classe, base.method(), base.scores(), competences, style, base.equipment(), bottes, null, null,
                expertise, outil);
    }

    private static Build suggeree(CharacterClass classe) {
        Build base = Build.suggested(classe);
        return de(classe, base.fightingStyle(), base.masteries(), Set.of(), Set.of(), null);
    }

    @Test
    void lesConstructionsSuggerees_sontValides() {
        for (CharacterClass classe : CharacterClass.values()) {
            assertDoesNotThrow(() -> Build.suggested(classe).validate(List.of()), classe.label());
        }
        assertEquals(List.of(SrdWeapon.GREATAXE, SrdWeapon.HANDAXE), Build.suggested(CharacterClass.BARBARIAN).masteries());
        assertEquals(List.of(), Build.suggested(CharacterClass.MONK).masteries());
        assertEquals(null, Build.suggested(CharacterClass.ROGUE).fightingStyle());
    }

    @Test
    void unStylePourUnBarbare_ouAucunPourUnGuerrier_sontRefuses() {
        Build barbare = suggeree(CharacterClass.BARBARIAN);
        assertEquals("Barbare : aucun Style de combat au niveau 1",
                raison(de(CharacterClass.BARBARIAN, FightingStyle.DEFENSE, barbare.masteries(), Set.of(), Set.of(), null), List.of()));
        Build guerrier = Build.fighter();
        assertEquals("Choisir un Style de combat",
                raison(de(CharacterClass.FIGHTER, null, guerrier.masteries(), Set.of(), Set.of(), null), List.of()));
    }

    @Test
    void uneBotteHorsDesArmesDeLaClasse_estRefusee() {
        assertEquals("Barbare : pas de Botte d'arme pour Arc long", raison(de(CharacterClass.BARBARIAN, null,
                List.of(SrdWeapon.GREATAXE, SrdWeapon.LONGBOW), Set.of(), Set.of(), null), List.of()));
        assertEquals("Roublard : pas de Botte d'arme pour Hache à deux mains", raison(de(CharacterClass.ROGUE, null,
                List.of(SrdWeapon.DAGGER, SrdWeapon.GREATAXE), Set.of(), Set.of(), null), List.of()));
        assertEquals("Bottes d'arme : 0 types d'arme différents", raison(de(CharacterClass.MONK, null,
                List.of(SrdWeapon.DAGGER), Set.of(), Set.of(), null), List.of()));
        assertDoesNotThrow(() -> de(CharacterClass.ROGUE, null, List.of(SrdWeapon.RAPIER, SrdWeapon.HAND_CROSSBOW),
                Set.of(), Set.of(), null).validate(List.of()));
    }

    @Test
    void uneExpertiseImpossible_estRefusee() {
        Build guerrier = Build.fighter();
        assertEquals("Guerrier : aucune Expertise au niveau 1", raison(de(CharacterClass.FIGHTER, FightingStyle.DEFENSE,
                guerrier.masteries(), Set.of(Skill.ATHLETICS), Set.of(Skill.ATHLETICS), null), List.of()));

        List<SrdWeapon> bottes = suggeree(CharacterClass.ROGUE).masteries();
        Set<Skill> quatre = Set.of(Skill.STEALTH, Skill.SLEIGHT_OF_HAND, Skill.PERCEPTION, Skill.DECEPTION);
        assertEquals("Roublard : 2 Expertises au plus", raison(de(CharacterClass.ROGUE, null, bottes, quatre,
                Set.of(Skill.STEALTH, Skill.SLEIGHT_OF_HAND, Skill.PERCEPTION), null), List.of()));
        assertEquals("Expertise : Athlétisme n’est pas maîtrisée", raison(de(CharacterClass.ROGUE, null, bottes, quatre,
                Set.of(Skill.ATHLETICS), null), List.of()));
    }

    @Test
    void uneExpertiseSurUneCompetenceDeLHistorique_estAcceptee() {
        Build roublard = de(CharacterClass.ROGUE, null, suggeree(CharacterClass.ROGUE).masteries(), Set.of(Skill.PERCEPTION),
                Set.of(Skill.STEALTH, Skill.PERCEPTION), null).withBackground(BackgroundChoice.of(Background.CRIMINAL));

        assertDoesNotThrow(() -> roublard.validate(List.of()));
    }

    @Test
    void unOutilHorsDesChoixDeLaClasse_estRefuse() {
        assertEquals("Roublard : aucun outil à choisir", raison(de(CharacterClass.ROGUE, null,
                suggeree(CharacterClass.ROGUE).masteries(), Set.of(), Set.of(), Tool.LUTE), List.of()));
        assertEquals("Moine : outils de voleur hors des outils au choix", raison(de(CharacterClass.MONK, null, List.of(),
                Set.of(), Set.of(), Tool.THIEVES_TOOLS), List.of()));
        assertDoesNotThrow(() -> de(CharacterClass.MONK, null, List.of(), Set.of(), Set.of(), Tool.SMITHS_TOOLS).validate(List.of()));
    }
}
