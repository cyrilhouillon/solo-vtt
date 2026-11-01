package dnd.engine.core;

/**
 * Quelque chose en cours de résolution sur la pile du moteur (une attaque, un sort…).
 * Une résolution avance par étapes et peut s'interrompre pour deux raisons :
 * une décision à prendre, ou une autre résolution empilée par-dessus (ex. Contresort).
 *
 * @param <S> le type d'état du système de jeu
 * @param <E> le type d'événement du système de jeu
 */
public interface Resolution<S, E extends Event<S>> {

    Advance advance(Engine<S, E> engine);

    sealed interface Advance {
    }

    /** Terminé : le moteur dépile. */
    record Completed() implements Advance {
    }

    /** En attente d'une décision. */
    record Waiting(Decision decision) implements Advance {
    }

    /** Une autre résolution a été empilée : elle se résout d'abord, puis on reprend. */
    record Yielded() implements Advance {
    }
}
