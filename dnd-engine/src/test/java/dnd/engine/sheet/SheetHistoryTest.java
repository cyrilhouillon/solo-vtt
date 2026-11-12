package dnd.engine.sheet;

import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;

import java.util.List;
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
                FightingStyle.ARCHERY, StartingEquipment.FIGHTER_B, StartingEquipment.FIGHTER_B.weapons());

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
}
