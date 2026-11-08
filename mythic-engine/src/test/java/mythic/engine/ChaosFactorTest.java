package mythic.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChaosFactorTest {

    @Test
    void unePartieCommenceAuChaos5() {
        assertEquals(5, ChaosFactor.INITIAL.value());
    }

    @Test
    void leChaosVaDe1A9() {
        assertThrows(IllegalArgumentException.class, () -> ChaosFactor.of(0));
        assertThrows(IllegalArgumentException.class, () -> ChaosFactor.of(10));
        assertEquals(1, ChaosFactor.of(1).value());
        assertEquals(9, ChaosFactor.of(9).value());
    }

    @Test
    void monterOuBaisser_resteEntre1Et9() {
        assertEquals(ChaosFactor.of(6), ChaosFactor.of(5).raise());
        assertEquals(ChaosFactor.of(4), ChaosFactor.of(5).lower());
        assertEquals(ChaosFactor.of(9), ChaosFactor.of(9).raise());
        assertEquals(ChaosFactor.of(1), ChaosFactor.of(1).lower());
    }

    @Test
    void enFinDeScene_leControleGardeBaisse_etPerduMonte_entre1Et9() {
        assertEquals(ChaosFactor.of(4), ChaosFactor.of(5).afterScene(true));
        assertEquals(ChaosFactor.of(6), ChaosFactor.of(5).afterScene(false));
        assertEquals(ChaosFactor.of(1), ChaosFactor.of(1).afterScene(true));
        assertEquals(ChaosFactor.of(9), ChaosFactor.of(9).afterScene(false));
    }
}
