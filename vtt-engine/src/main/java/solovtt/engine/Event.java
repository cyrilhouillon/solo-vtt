package solovtt.engine;

/**
 * Tout ce qui se passe dans une partie, quel que soit le système de jeu.
 * Un événement qui change l'état surcharge {@link #apply} ; les autres (jets de dés,
 * explications de règles) ne servent qu'au journal.
 *
 * <p>Rejouer le journal sur une copie de l'état initial doit redonner l'état final.</p>
 *
 * @param <S> le type d'état du système de jeu
 */
public interface Event<S> {

    default void apply(S state) {
    }
}
