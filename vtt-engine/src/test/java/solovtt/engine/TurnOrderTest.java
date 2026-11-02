package solovtt.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TurnOrderTest {

    @Test
    void plusieursCandidats_personneNEstActifAvantLeChoix() {
        TurnOrder order = TurnOrder.offer(1, List.of("a", "b"));

        assertEquals(1, order.round());
        assertEquals(List.of("a", "b"), order.candidates());
        assertTrue(order.awaitingChoice());
        assertTrue(order.activeId().isEmpty());
    }

    @Test
    void choisirUnCandidat_leRendActif_sansModifierLOriginal() {
        TurnOrder order = TurnOrder.offer(1, List.of("a", "b"));

        TurnOrder chosen = order.choose("a");

        assertEquals("a", chosen.activeId().orElseThrow());
        assertFalse(chosen.awaitingChoice());
        assertTrue(order.awaitingChoice());
    }

    @Test
    void unSeulCandidat_estActifDOffice() {
        TurnOrder order = TurnOrder.offer(3, List.of("c"));

        assertEquals("c", order.activeId().orElseThrow());
        assertEquals(3, order.round());
    }

    @Test
    void choisirHorsDesCandidats_ouQuandQuelquUnJoue_estRefuse() {
        TurnOrder order = TurnOrder.offer(1, List.of("a", "b"));

        assertThrows(IllegalArgumentException.class, () -> order.choose("x"));
        assertThrows(IllegalStateException.class, () -> order.choose("a").choose("b"));
    }

    @Test
    void sansCandidat_enDouble_ouRoundNul_estRefuse() {
        assertThrows(IllegalArgumentException.class, () -> TurnOrder.offer(1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> TurnOrder.offer(1, List.of("a", "a")));
        assertThrows(IllegalArgumentException.class, () -> TurnOrder.offer(0, List.of("a")));
    }

    /** Un système sans initiative, à la Daggerheart : les joueurs (au choix), puis le MJ, puis les joueurs… */
    record Table(List<String> players, String gm) {}

    static final class PlayersThenGm implements TurnPolicy<Table> {
        public TurnOrder first(Table t) {
            return TurnOrder.offer(1, t.players());
        }

        public TurnOrder afterTurn(Table t, TurnOrder ended) {
            boolean gmPlayed = ended.activeId().orElseThrow().equals(t.gm());
            return gmPlayed ? TurnOrder.offer(ended.round() + 1, t.players()) : TurnOrder.offer(ended.round(), List.of(t.gm()));
        }
    }

    @Test
    void unSystemeSansInitiative_seDecritParSaPolitique() {
        Table table = new Table(List.of("barde", "rodeur"), "mj");
        TurnPolicy<Table> policy = new PlayersThenGm();

        TurnOrder players = policy.first(table);
        assertEquals(List.of("barde", "rodeur"), players.candidates());
        assertTrue(players.awaitingChoice());

        TurnOrder gm = policy.afterTurn(table, players.choose("rodeur"));
        assertEquals("mj", gm.activeId().orElseThrow());
        assertEquals(1, gm.round());

        TurnOrder next = policy.afterTurn(table, gm);
        assertEquals(2, next.round());
        assertEquals(List.of("barde", "rodeur"), next.candidates());
    }
}
