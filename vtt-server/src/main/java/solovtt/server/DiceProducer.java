package solovtt.server;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import solovtt.engine.Dice;

import java.security.SecureRandom;

/** Les dés du jeu, tirés au hasard. Un test les remplace en déclarant son propre bean {@link Dice}. */
public class DiceProducer {

    @Produces
    @Singleton
    @DefaultBean
    Dice dice() {
        return Dice.seeded(new SecureRandom().nextLong());
    }
}
