package mythic.engine;

import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SceneCheckTest {

    private static final ChaosFactor CHAOS_5 = ChaosFactor.of(5);
    private static final Optional<MeaningTables> SANS_TABLES = Optional.empty();
    private static final Function<AdventureList.Kind, AdventureList> SANS_LISTES = AdventureList::empty;
    private static final Function<AdventureList.Kind, AdventureList> PERSONNAGES = kind ->
            new AdventureList(kind, kind == AdventureList.Kind.CHARACTERS ? List.of(true, true) : List.of());

    @Test
    void auDessusDuChaos_laSceneEstAttendue_sansDeDePlus() {
        assertEquals(new SceneCheck(6, CHAOS_5, SceneOutcome.EXPECTED, List.of(), List.of(), Optional.empty()),
                SceneCheck.test(CHAOS_5, SANS_TABLES, SANS_LISTES, Dice.scripted(6)));
    }

    @Test
    void impairSousLeChaos_laSceneEstModifiee_avecSonAjustement() {
        assertEquals(new SceneCheck(3, CHAOS_5, SceneOutcome.ALTERED, List.of(SceneAdjustment.ADD_A_CHARACTER), List.of(2),
                Optional.empty()), SceneCheck.test(CHAOS_5, SANS_TABLES, SANS_LISTES, Dice.scripted(3, 2)));
    }

    @Test
    void uneSceneModifiee_peutAvoirDeuxAjustements() {
        var scene = SceneCheck.test(CHAOS_5, SANS_TABLES, SANS_LISTES, Dice.scripted(5, 8, 1, 6));

        assertEquals(List.of(SceneAdjustment.REMOVE_A_CHARACTER, SceneAdjustment.ADD_AN_OBJECT), scene.adjustments());
        assertEquals(List.of(8, 1, 6), scene.adjustmentRolls());
    }

    @Test
    void pairSousLeChaos_laSceneEstInterrompue_parUnEvenementComplet() {
        var scene = SceneCheck.test(CHAOS_5, Optional.of(TablesDeTest.tables()), PERSONNAGES, Dice.scripted(4, 25, 4, 90, 3));

        assertEquals(SceneOutcome.INTERRUPTED, scene.outcome());
        assertEquals(List.of(), scene.adjustments());
        assertEquals(Optional.of(new RandomEvent(EventFocus.NPC_ACTION, 25,
                Optional.of(new Meaning("Actions", "action1-004", 4, "action2-090", 90)),
                Optional.of(new ListDraw.Line(AdventureList.Kind.CHARACTERS, 2, null, null, 3, false)))), scene.event());
    }

    @Test
    void interrompueSansTables_pasDeSens_niDeD100DeSens() {
        var scene = SceneCheck.test(CHAOS_5, SANS_TABLES, SANS_LISTES, Dice.scripted(2, 1));

        assertEquals(Optional.of(new RandomEvent(EventFocus.REMOTE_EVENT, 1, Optional.empty(), Optional.empty())), scene.event());
    }

    @Test
    void auChaos9_unDixResteAttendu() {
        assertEquals(SceneOutcome.EXPECTED, SceneCheck.test(ChaosFactor.of(9), SANS_TABLES, SANS_LISTES, Dice.scripted(10)).outcome());
    }

    @Test
    void egalAuChaos_compteCommeDessous() {
        assertEquals(SceneOutcome.ALTERED, SceneCheck.test(ChaosFactor.of(1), SANS_TABLES, SANS_LISTES, Dice.scripted(1, 2)).outcome());
        assertEquals(SceneOutcome.INTERRUPTED, SceneCheck.test(ChaosFactor.of(4), SANS_TABLES, SANS_LISTES, Dice.scripted(4, 1)).outcome());
    }
}
