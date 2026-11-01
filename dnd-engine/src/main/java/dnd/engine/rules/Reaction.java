package dnd.engine.rules;

import dnd.engine.events.GameEvent;
import dnd.engine.state.Creature;
import dnd.engine.state.GameState;
import solovtt.engine.Engine;

/**
 * Une réaction (SRD 5.2.1, glossaire « Reaction ») : ce qu'une créature peut faire quand un déclencheur survient,
 * pendant son tour ou celui d'un autre. Une seule réaction jusqu'au début de son prochain tour.
 *
 * <p>Une réaction n'est pas un listener : elle s'enregistre auprès de la fenêtre de son déclencheur
 * ({@link AttackHitReactions} pour une attaque qui touche), qui rassemble les réactions possibles de chaque
 * créature en une seule question. Le joueur voit ainsi toutes ses options ensemble (Bouclier ou Contresort).</p>
 *
 * @param <T> ce qui déclenche la réaction (le contexte d'une attaque, d'un sort…)
 */
public interface Reaction<T> {

    /** Le nom de la réaction, repris dans le journal (« utilise sa réaction : Bouclier »). */
    String name();

    /** L'option proposée au joueur ; elle sert aussi de réponse à la décision. */
    String option();

    /**
     * La réaction est-elle possible, et utile, pour cette créature face à ce déclencheur ?
     * La fenêtre vérifie déjà que la créature a sa réaction disponible.
     */
    boolean offeredTo(Creature reactor, T trigger, GameState state);

    /** Les effets de la réaction ; la fenêtre a déjà émis la réaction consommée. */
    void apply(Creature reactor, T trigger, Engine<GameState, GameEvent> engine);
}
