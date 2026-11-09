package mythic.engine;

import solovtt.engine.Dice;

import java.util.Optional;
import java.util.function.Function;

/**
 * Un événement aléatoire, déclenché par un double ou par une scène interrompue : son focus, le d100 qui l'a tiré, son
 * sens, deux mots d'action, quand le joueur a importé ses tables, et le tirage dans la liste que vise le focus.
 */
public record RandomEvent(EventFocus focus, int focusRoll, Optional<Meaning> meaning, Optional<ListDraw> draw) {

    /**
     * Tire un événement : le focus (d100), le sens dans les Actions si les tables existent (deux d100), puis une ligne de
     * la liste que vise le focus.
     *
     * @param lists la liste d'une sorte, lue seulement si le focus la vise
     */
    public static RandomEvent roll(Optional<MeaningTables> tables, Function<AdventureList.Kind, AdventureList> lists,
                                   Dice dice) {
        int focusRoll = dice.roll(100);
        EventFocus focus = EventFocus.of(focusRoll);
        Optional<Meaning> meaning = tables.map(t -> t.actions(dice));
        Optional<ListDraw> draw = focus.list().map(kind -> lists.apply(kind).draw(dice));
        return new RandomEvent(focus, focusRoll, meaning, draw);
    }

    /** Le focus qui s'applique : « Contexte actuel » quand la liste visée est vide, sinon le focus tiré. */
    public EventFocus effectiveFocus() {
        return draw.filter(ListDraw.Empty.class::isInstance).isPresent() ? EventFocus.CURRENT_CONTEXT : focus;
    }
}
