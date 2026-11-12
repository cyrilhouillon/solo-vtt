package dnd.engine.sheet;

import dnd.engine.state.DamageType;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CharacterSheetTest {

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

    private static CharacterSheet roublarde() {
        return new CharacterSheet("Roublard", 1, "Halfelin", "P (petite)", "Criminel", valeurs(8, 14, 12, 13, 12, 10), 2,
                Set.of(Ability.DEXTERITY, Ability.INTELLIGENCE),
                Map.of(Skill.STEALTH, Proficiency.EXPERTISE, Skill.PERCEPTION, Proficiency.PROFICIENT),
                14, 9, 9, "1d8", 7.5, 2,
                List.of(new SheetAttack("Rapière", 4, 1, 8, 2, DamageType.PIERCING, 1.5, null, null)),
                "Attaque sournoise", "Chance", "Armure de cuir", "Armes courantes", "Commun", List.of(new SpellSlots(1, 2, 1)));
    }

    @Test
    void uneFicheCreee_estVide_niveau1_valeursA10_maitrisePlus2() {
        CharacterSheet fiche = SheetHistory.replay(List.of(new SheetEvent.SheetCreated())).values();

        assertEquals(CharacterSheet.blank(), fiche);
        assertEquals(1, fiche.level());
        assertEquals(2, fiche.proficiencyBonus());
        for (Ability ability : Ability.values()) assertEquals(0, fiche.modifier(ability));
        assertEquals("", fiche.className());
        assertEquals(List.of(), fiche.attacks());
        assertEquals(10, fiche.passivePerception());
    }

    @Test
    void lesDerives_suiventLesFormulesDuSrd() {
        CharacterSheet fiche = roublarde();

        assertEquals(2, fiche.modifier(Ability.DEXTERITY));
        assertEquals(-1, fiche.modifier(Ability.STRENGTH));
        assertEquals(1, fiche.modifier(Ability.INTELLIGENCE), "13 donne +1");
        assertEquals(4, fiche.savingThrow(Ability.DEXTERITY), "maîtrisée : +2 +2");
        assertEquals(-1, fiche.savingThrow(Ability.STRENGTH), "non maîtrisée : le modificateur seul");
        assertEquals(6, fiche.skill(Skill.STEALTH), "expertise : +2 + 2 × 2");
        assertEquals(3, fiche.skill(Skill.PERCEPTION), "maîtrise : +1 +2");
        assertEquals(-1, fiche.skill(Skill.ATHLETICS), "non maîtrisée : Force −1");
        assertEquals(Proficiency.NONE, fiche.proficiency(Skill.ARCANA));
        assertEquals(13, fiche.passivePerception());
    }

    @Test
    void unModificateur_arrondiALInferieur() {
        CharacterSheet fiche = new CharacterSheet("", 1, "", "", "", valeurs(9, 1, 30, 11, 10, 7), 2, Set.of(), Map.of(),
                10, 0, 0, "", 9, 0, List.of(), "", "", "", "", "", List.of());

        assertEquals(-1, fiche.modifier(Ability.STRENGTH));
        assertEquals(-5, fiche.modifier(Ability.DEXTERITY));
        assertEquals(10, fiche.modifier(Ability.CONSTITUTION));
        assertEquals(0, fiche.modifier(Ability.INTELLIGENCE));
        assertEquals(-2, fiche.modifier(Ability.CHARISMA));
    }

    @Test
    void laDerniereSaisie_estLEtatDeLaFiche() {
        CharacterSheet premiere = roublarde();
        CharacterSheet seconde = CharacterSheet.blank();

        assertEquals(seconde, SheetHistory.replay(List.of(new SheetEvent.SheetCreated(),
                new SheetEvent.SheetEdited(premiere), new SheetEvent.SheetEdited(seconde))).values());
        assertEquals(premiere, SheetHistory.replay(List.of(new SheetEvent.SheetCreated(), new SheetEvent.SheetEdited(premiere))).values());
    }

    @Test
    void uneHistoireSansCreation_ouAvecDeuxCreations_estRefusee() {
        assertThrows(IllegalArgumentException.class, () -> SheetHistory.replay(List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> SheetHistory.replay(List.of(new SheetEvent.SheetEdited(CharacterSheet.blank()))));
        assertThrows(IllegalArgumentException.class,
                () -> SheetHistory.replay(List.of(new SheetEvent.SheetCreated(), new SheetEvent.SheetCreated())));
    }

    @Test
    void uneValeurHorsBornes_estRefuseeAvecSaRaison() {
        var trop = assertThrows(IllegalArgumentException.class, () -> new CharacterSheet("", 1, "", "", "",
                valeurs(10, 31, 10, 10, 10, 10), 2, Set.of(), Map.of(), 10, 0, 0, "", 9, 0, List.of(), "", "", "", "", "", List.of()));
        assertEquals("Dextérité entre 1 et 30 : 31", trop.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new CharacterSheet("", 1, "", "", "",
                valeurs(0, 10, 10, 10, 10, 10), 2, Set.of(), Map.of(), 10, 0, 0, "", 9, 0, List.of(), "", "", "", "", "", List.of()));
        var niveau = assertThrows(IllegalArgumentException.class, () -> new CharacterSheet("", 21, "", "", "",
                valeurs(10, 10, 10, 10, 10, 10), 2, Set.of(), Map.of(), 10, 0, 0, "", 9, 0, List.of(), "", "", "", "", "", List.of()));
        assertEquals("Niveau entre 1 et 20 : 21", niveau.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new CharacterSheet("", 0, "", "", "",
                valeurs(10, 10, 10, 10, 10, 10), 2, Set.of(), Map.of(), 10, 0, 0, "", 9, 0, List.of(), "", "", "", "", "", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CharacterSheet("", 1, "", "", "",
                valeurs(10, 10, 10, 10, 10, 10), 2, Set.of(), Map.of(), -1, 0, 0, "", 9, 0, List.of(), "", "", "", "", "", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CharacterSheet("", 1, "", "", "",
                valeurs(10, 10, 10, 10, 10, 10), 2, Set.of(), Map.of(), 10, 5, -1, "", 9, 0, List.of(), "", "", "", "", "", List.of()));
        var manquante = assertThrows(IllegalArgumentException.class, () -> new CharacterSheet("", 1, "", "", "",
                Map.of(Ability.STRENGTH, 10), 2, Set.of(), Map.of(), 10, 0, 0, "", 9, 0, List.of(), "", "", "", "", "", List.of()));
        assertEquals("Valeur manquante : Dextérité", manquante.getMessage());
        var emplacements = assertThrows(IllegalArgumentException.class, () -> new SpellSlots(1, 2, 3));
        assertEquals("Emplacements dépensés au niveau 1 entre 0 et 2 : 3", emplacements.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new CharacterSheet("", 1, "", "", "",
                valeurs(10, 10, 10, 10, 10, 10), 2, Set.of(), Map.of(), 10, 0, 0, "", 9, 0, List.of(), "", "", "", "", "",
                List.of(new SpellSlots(1, 2, 0), new SpellSlots(1, 3, 0))));
        assertThrows(IllegalArgumentException.class, () -> new SheetAttack("  ", 4, 1, 8, 2, DamageType.PIERCING, 1.5, null, null));
    }
}
