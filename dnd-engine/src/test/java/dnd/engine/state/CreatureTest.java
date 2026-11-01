package dnd.engine.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatureTest {

    @Test
    void sansVitesseDonnee_uneCreatureSeDeplaceDeNeufMetres() {
        assertEquals(9, Creature.builder("c", "C").build().speed());
    }

    @Test
    void laVitesseSurvitALaCopie() {
        var loup = Creature.builder("loup", "Loup").speed(12).build();
        assertEquals(12, loup.copy().speed());
    }
}
