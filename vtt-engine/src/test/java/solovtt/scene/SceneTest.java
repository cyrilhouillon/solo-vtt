package solovtt.scene;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneTest {

    private static Scene carte() {
        return new Scene(30, 20, DistanceUnit.METRE);
    }

    @Test
    void uneCarteNeuveEstVide() {
        var scene = carte();
        assertTrue(scene.tokens().isEmpty());
        assertEquals(DistanceUnit.METRE, scene.unit());
    }

    @Test
    void laDistanceEstEuclidienneDeCentreACentre_dansLUniteDeLaCarte() {
        var scene = new Scene(100, 100, DistanceUnit.FOOT);
        scene.place(new Token("a", "x", new Position(0, 0)));
        scene.place(new Token("b", "y", new Position(3, 4)));

        assertEquals(5.0, scene.distance("a", "b"), 1e-9);
        assertEquals(DistanceUnit.FOOT, scene.unit());
    }

    @Test
    void unJetonHorsDeLaCarteEstRefuse() {
        var scene = carte();
        var e = assertThrows(IllegalArgumentException.class,
                () -> scene.place(new Token("a", "x", new Position(31, 5))));
        assertTrue(e.getMessage().contains("hors de la carte"));
    }

    @Test
    void deuxJetonsNePeuventPasPorterLeMemeId() {
        var scene = carte();
        scene.place(new Token("a", "x", new Position(1, 1)));
        assertThrows(IllegalArgumentException.class, () -> scene.place(new Token("a", "y", new Position(2, 2))));
    }

    @Test
    void laDistanceAvecUnJetonInconnuEstRefusee() {
        var scene = carte();
        scene.place(new Token("a", "x", new Position(1, 1)));
        var e = assertThrows(IllegalArgumentException.class, () -> scene.distance("a", "fantome"));
        assertTrue(e.getMessage().contains("Jeton inconnu"));
    }

    @Test
    void retirerUnJetonLeSortDeLaCarte() {
        var scene = carte();
        scene.place(new Token("a", "x", new Position(1, 1)));
        scene.remove("a");
        assertTrue(scene.tokens().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> scene.remove("a"));
    }

    @Test
    void uneCarteDeDimensionNulleEstRefusee() {
        assertThrows(IllegalArgumentException.class, () -> new Scene(0, 10, DistanceUnit.METRE));
    }

    @Test
    void deplacerUnJetonChangeSaPosition_etGardeSonEntite() {
        var scene = carte();
        scene.place(new Token("a", "x", new Position(1, 1)));
        scene.move("a", new Position(10, 5));
        assertEquals(new Position(10, 5), scene.token("a").position());
        assertEquals("x", scene.token("a").entityId());
    }

    @Test
    void deplacerHorsDeLaCarte_ouUnJetonInconnu_estRefuseSansBouger() {
        var scene = carte();
        scene.place(new Token("a", "x", new Position(1, 1)));
        var e = assertThrows(IllegalArgumentException.class, () -> scene.move("a", new Position(31, 5)));
        assertTrue(e.getMessage().contains("hors de la carte"));
        assertEquals(new Position(1, 1), scene.token("a").position());
        assertThrows(IllegalArgumentException.class, () -> scene.move("fantome", new Position(2, 2)));
    }
}
