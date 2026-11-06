package mythic.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventFocusTest {

    @Test
    void chaqueTranche_donneSonFocus_bornesComprises() {
        int[][] bornes = {{1, 5}, {6, 10}, {11, 20}, {21, 40}, {41, 45}, {46, 50}, {51, 55}, {56, 65}, {66, 70}, {71, 80}, {81, 85}, {86, 100}};
        EventFocus[] focus = EventFocus.values();
        assertEquals(bornes.length, focus.length);
        for (int i = 0; i < bornes.length; i++) {
            assertEquals(focus[i], EventFocus.of(bornes[i][0]), "début de " + focus[i]);
            assertEquals(focus[i], EventFocus.of(bornes[i][1]), "fin de " + focus[i]);
        }
    }

    @Test
    void unJetHorsDuD100_estRefuse() {
        assertThrows(IllegalArgumentException.class, () -> EventFocus.of(0));
        assertThrows(IllegalArgumentException.class, () -> EventFocus.of(101));
    }
}
