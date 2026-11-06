package mythic.engine;

import java.util.Optional;

/**
 * Un événement aléatoire, déclenché par un double : son focus, le d100 qui l'a tiré, et son sens, deux mots d'action,
 * quand le joueur a importé ses tables.
 */
public record RandomEvent(EventFocus focus, int focusRoll, Optional<Meaning> meaning) {
}
