package dnd.engine.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Source de hasard injectable. Le moteur ne touche jamais à Random directement :
 * en test on injecte des dés scriptés, en jeu des dés à graine (rejouables).
 */
public interface Dice {

    int roll(int sides);

    static Dice seeded(long seed) {
        Random random = new Random(seed);
        return sides -> 1 + random.nextInt(sides);
    }

    /** Dés pré-déterminés, consommés dans l'ordre. Idéal pour les tests de règles. */
    static Dice scripted(int... values) {
        Deque<Integer> queue = new ArrayDeque<>();
        for (int v : values) queue.add(v);
        return sides -> {
            Integer v = queue.poll();
            if (v == null) throw new IllegalStateException("Plus de dés scriptés disponibles (d" + sides + " demandé)");
            if (v < 1 || v > sides) throw new IllegalArgumentException("Valeur scriptée " + v + " impossible sur un d" + sides);
            return v;
        };
    }
}
