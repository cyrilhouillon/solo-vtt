package solovtt.engine;

import java.util.Optional;

/**
 * Une règle qui intervient dans une phase précise d'un pipeline.
 *
 * <p>Les règles de base du jeu (lancer le d20, appliquer les dégâts…) sont elles aussi
 * des listeners : il n'y a pas de différence de nature entre « la règle » et
 * « l'exception à la règle », ce qui permet d'ajouter n'importe quel effet sans toucher au moteur.</p>
 *
 * @param <S> le type d'état du système de jeu
 * @param <E> le type d'événement du système de jeu
 * @param <P> l'enum des phases du pipeline concerné (ex. AttackPhase)
 * @param <C> le contexte mutable partagé par les listeners de ce pipeline
 */
public interface Listener<S, E extends Event<S>, P extends Enum<P>, C> {

    P phase();

    /**
     * Départage les listeners d'une MÊME phase (plus petit = plus tôt).
     * À égalité, l'ordre d'enregistrement est conservé. Si tu as besoin de priorités
     * pour séparer des étapes que les règles distinguent, c'est sans doute une phase qui manque.
     */
    default int priority() {
        return 0;
    }

    /** Évalué au moment où vient le tour du listener, pas au début du pipeline. */
    default boolean appliesTo(C ctx, S state) {
        return true;
    }

    /**
     * Applique la règle. Retourne une décision pour suspendre le moteur
     * jusqu'à ce que le joueur concerné ait choisi.
     */
    Optional<Decision> handle(C ctx, Engine<S, E> engine);
}
