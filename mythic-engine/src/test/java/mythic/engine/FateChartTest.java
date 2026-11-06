package mythic.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FateChartTest {

    @Test
    void probable_auChaos5_seuils13_65_94_bornesIncluses() {
        var cell = FateChart.cell(Odds.LIKELY, ChaosFactor.of(5));
        assertEquals(new FateChart.Cell(13, 65, 94), cell);
        assertEquals(Answer.EXCEPTIONAL_YES, cell.answer(13));
        assertEquals(Answer.YES, cell.answer(14));
        assertEquals(Answer.YES, cell.answer(65));
        assertEquals(Answer.NO, cell.answer(66));
        assertEquals(Answer.NO, cell.answer(93));
        assertEquals(Answer.EXCEPTIONAL_NO, cell.answer(94));
    }

    @Test
    void cinquanteCinquante_auChaos5_estLeCentreDeLaTable() {
        assertEquals(new FateChart.Cell(10, 50, 91), FateChart.cell(Odds.FIFTY_FIFTY, ChaosFactor.of(5)));
    }

    @Test
    void impossible_auChaos1_sansOuiExceptionnel_ouiSeulementSur1() {
        var cell = FateChart.cell(Odds.IMPOSSIBLE, ChaosFactor.of(1));
        assertEquals(Answer.YES, cell.answer(1));
        assertEquals(Answer.NO, cell.answer(2));
        assertEquals(Answer.EXCEPTIONAL_NO, cell.answer(81));
    }

    @Test
    void certain_auChaos9_sansNonExceptionnel_100estNon() {
        var cell = FateChart.cell(Odds.CERTAIN, ChaosFactor.of(9));
        assertEquals(Answer.EXCEPTIONAL_YES, cell.answer(20));
        assertEquals(Answer.YES, cell.answer(99));
        assertEquals(Answer.NO, cell.answer(100));
    }

    @Test
    void chaqueCase_aDesSeuilsOrdonnes() {
        for (Odds odds : Odds.values()) {
            for (int chaos = 1; chaos <= 9; chaos++) {
                var cell = FateChart.cell(odds, ChaosFactor.of(chaos));
                String where = odds + " chaos " + chaos;
                assertTrue(cell.exceptionalYes() < cell.yes(), where);
                assertTrue(cell.yes() < cell.exceptionalNo(), where);
            }
        }
    }

    @Test
    void unCranDeProbabilite_vautUnCranDeChaos() {
        // La table glisse en diagonale : « Probable » au chaos 4 vaut « 50/50 » au chaos 5.
        assertEquals(FateChart.cell(Odds.FIFTY_FIFTY, ChaosFactor.of(5)), FateChart.cell(Odds.LIKELY, ChaosFactor.of(4)));
        assertEquals(FateChart.cell(Odds.VERY_LIKELY, ChaosFactor.of(3)), FateChart.cell(Odds.LIKELY, ChaosFactor.of(4)));
    }
}
