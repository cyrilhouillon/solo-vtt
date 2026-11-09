package mythic.engine;

import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;

import java.util.List;

import static mythic.engine.SceneAdjustment.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SceneAdjustmentTest {

    @Test
    void unD10De1A6_donneUnSeulAjustement() {
        assertEquals(new Rolled(List.of(ADD_A_CHARACTER), List.of(2)), SceneAdjustment.roll(Dice.scripted(2)));
        assertEquals(new Rolled(List.of(ADD_AN_OBJECT), List.of(6)), SceneAdjustment.roll(Dice.scripted(6)));
    }

    @Test
    void chaqueResultat_suitLaTable() {
        assertEquals(List.of(REMOVE_A_CHARACTER, ADD_A_CHARACTER, REDUCE_AN_ACTIVITY, INCREASE_AN_ACTIVITY,
                REMOVE_AN_OBJECT, ADD_AN_OBJECT), List.of(1, 2, 3, 4, 5, 6).stream().map(r -> SceneAdjustment.of(r).orElseThrow()).toList());
        for (int roll = 7; roll <= 10; roll++) assertEquals(java.util.Optional.empty(), SceneAdjustment.of(roll));
    }

    @Test
    void deSeptADix_deuxAjustements() {
        assertEquals(new Rolled(List.of(REMOVE_A_CHARACTER, ADD_AN_OBJECT), List.of(8, 1, 6)),
                SceneAdjustment.roll(Dice.scripted(8, 1, 6)));
    }

    @Test
    void unNouveauSeptADix_estRelance_etUnDoublonCompteDeuxFois() {
        assertEquals(new Rolled(List.of(ADD_A_CHARACTER, ADD_A_CHARACTER), List.of(9, 2, 10, 2)),
                SceneAdjustment.roll(Dice.scripted(9, 2, 10, 2)));
    }

    @Test
    void unSecondAjustementContraire_estIgnore_seulLePremierCompte() {
        assertEquals(new Rolled(List.of(ADD_A_CHARACTER), List.of(7, 2, 1)), SceneAdjustment.roll(Dice.scripted(7, 2, 1)));
        assertEquals(new Rolled(List.of(INCREASE_AN_ACTIVITY), List.of(7, 4, 3)), SceneAdjustment.roll(Dice.scripted(7, 4, 3)));
    }

    @Test
    void lesContraires_vontParPaires() {
        assertEquals(ADD_A_CHARACTER, REMOVE_A_CHARACTER.opposite());
        assertEquals(REDUCE_AN_ACTIVITY, INCREASE_AN_ACTIVITY.opposite());
        assertEquals(REMOVE_AN_OBJECT, ADD_AN_OBJECT.opposite());
    }
}
