package solovtt.server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Où vivent les combats d'une campagne. Un combat est un agrégat : la partie ({@link SavedGame}) de la création de sa carte
 * jusqu'à sa fin, écrite à chaque commande tant qu'il est en cours, puis figée. Il ignore le Carnet : c'est l'entrée du
 * Carnet qui le référence ({@link Page#combatId()}) et qui porte son nom.
 */
public interface CombatStore {

    /**
     * La partie du combat {@code combatId}, à lire ou à écrire ; vide à la lecture tant qu'elle n'est pas écrite.
     *
     * @throws IllegalArgumentException si l'id n'est pas un id de combat
     */
    GameStore game(String combatId);

    /** Les ids des combats écrits. */
    List<String> ids();

    /** Un id de combat : un UUID, jamais un chemin. */
    static String checkId(String combatId) {
        if (combatId == null || !combatId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            throw new IllegalArgumentException("Id de combat invalide : " + combatId);
        }
        return combatId;
    }

    /** Garde les combats en mémoire, le temps du processus. */
    static CombatStore inMemory() {
        Map<String, GameStore> games = new ConcurrentHashMap<>();
        return new CombatStore() {
            @Override
            public GameStore game(String combatId) {
                return games.computeIfAbsent(checkId(combatId), key -> GameStore.inMemory());
            }

            @Override
            public List<String> ids() {
                return games.entrySet().stream().filter(e -> e.getValue().load().isPresent()).map(Map.Entry::getKey).toList();
            }
        };
    }
}
