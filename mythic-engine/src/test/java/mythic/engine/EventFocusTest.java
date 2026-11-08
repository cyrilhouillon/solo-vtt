package mythic.engine;

import org.junit.jupiter.api.Test;

import java.util.Optional;

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
    void lesFocusDePnj_tirentDansPersonnages_ceuxDeFil_dansFils_lesAutres_nullePart() {
        var personnages = Optional.of(AdventureList.Kind.CHARACTERS);
        var fils = Optional.of(AdventureList.Kind.THREADS);
        assertEquals(personnages, EventFocus.NPC_ACTION.list());
        assertEquals(personnages, EventFocus.NPC_NEGATIVE.list());
        assertEquals(personnages, EventFocus.NPC_POSITIVE.list());
        assertEquals(fils, EventFocus.MOVE_TOWARD_A_THREAD.list());
        assertEquals(fils, EventFocus.MOVE_AWAY_FROM_A_THREAD.list());
        assertEquals(fils, EventFocus.CLOSE_A_THREAD.list());
        for (EventFocus aucune : new EventFocus[]{EventFocus.REMOTE_EVENT, EventFocus.AMBIGUOUS_EVENT, EventFocus.NEW_NPC,
                EventFocus.PC_NEGATIVE, EventFocus.PC_POSITIVE, EventFocus.CURRENT_CONTEXT}) {
            assertEquals(Optional.empty(), aucune.list(), aucune.name());
        }
    }

    @Test
    void unJetHorsDuD100_estRefuse() {
        assertThrows(IllegalArgumentException.class, () -> EventFocus.of(0));
        assertThrows(IllegalArgumentException.class, () -> EventFocus.of(101));
    }
}
