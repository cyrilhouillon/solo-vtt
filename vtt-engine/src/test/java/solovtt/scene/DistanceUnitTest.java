package solovtt.scene;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DistanceUnitTest {

    @Test
    void uneCaseDeUnMetreCinqVautCinqPieds() {
        assertEquals(5.0, DistanceUnit.METRE.convert(1.5, DistanceUnit.FOOT), 1e-9);
        assertEquals(1.5, DistanceUnit.FOOT.convert(5, DistanceUnit.METRE), 1e-9);
    }

    @Test
    void neufMetresFontTrentePieds() {
        assertEquals(30.0, DistanceUnit.METRE.convert(9, DistanceUnit.FOOT), 1e-9);
    }

    @Test
    void convertirDansLaMemeUniteNeChangeRien() {
        assertEquals(12.0, DistanceUnit.FOOT.convert(12, DistanceUnit.FOOT), 1e-9);
    }
}
