package mythic.engine;

import java.util.Optional;

/**
 * Un événement aléatoire, déclenché par un double : son focus, le d100 qui l'a tiré, son sens, deux mots d'action,
 * quand le joueur a importé ses tables, et le tirage dans la liste que vise le focus.
 */
public record RandomEvent(EventFocus focus, int focusRoll, Optional<Meaning> meaning, Optional<ListDraw> draw) {

    /** Un événement dont le focus ne tire dans aucune liste. */
    public RandomEvent(EventFocus focus, int focusRoll, Optional<Meaning> meaning) {
        this(focus, focusRoll, meaning, Optional.empty());
    }

    /** Le focus qui s'applique : « Contexte actuel » quand la liste visée est vide, sinon le focus tiré. */
    public EventFocus effectiveFocus() {
        return draw.filter(ListDraw.Empty.class::isInstance).isPresent() ? EventFocus.CURRENT_CONTEXT : focus;
    }
}
