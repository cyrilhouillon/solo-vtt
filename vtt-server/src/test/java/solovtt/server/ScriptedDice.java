package solovtt.server;

import jakarta.inject.Singleton;
import solovtt.engine.Dice;

import java.util.ArrayDeque;
import java.util.Deque;

/** Dés des tests d'API : remplacent ceux de {@link DiceProducer} ; chaque test écrit les siens. */
@Singleton
public class ScriptedDice implements Dice {

    private final Deque<Integer> values = new ArrayDeque<>();

    public synchronized void script(int... rolls) {
        values.clear();
        for (int v : rolls) values.add(v);
    }

    @Override
    public synchronized int roll(int sides) {
        Integer v = values.poll();
        if (v == null) throw new IllegalStateException("Plus de dés scriptés disponibles (d" + sides + " demandé)");
        if (v < 1 || v > sides) throw new IllegalArgumentException("Valeur scriptée " + v + " impossible sur un d" + sides);
        return v;
    }
}
