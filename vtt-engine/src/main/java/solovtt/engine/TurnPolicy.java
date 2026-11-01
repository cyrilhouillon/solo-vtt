package solovtt.engine;

/**
 * La façon dont un système enchaîne les tours : qui peut jouer au début, puis après chaque fin de tour.
 *
 * <p>D&D 2024 : des groupes d'initiative, du plus haut au plus bas ; chaque membre d'un groupe joue
 * une fois par round, puis vient le groupe suivant, et après le dernier, un nouveau round.
 * Daggerheart : soit les joueurs, soit le MJ, selon ce qui vient de se passer. Le moteur ne connaît
 * que le résultat, un {@link TurnOrder}.</p>
 *
 * @param <S> le type d'état du système de jeu
 */
public interface TurnPolicy<S> {

    /** Le premier tour du combat (round 1). */
    TurnOrder first(S state);

    /** Le tour qui suit la fin de {@code ended} ; le round augmente si un nouveau round commence. */
    TurnOrder afterTurn(S state, TurnOrder ended);
}
