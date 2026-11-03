package solovtt.server;

import java.util.Optional;

/**
 * Où la session garde sa partie entre deux lancements. La session ne connaît que ce port :
 * le support (fichier, base…) et son format restent dans l'adaptateur.
 */
public interface GameStore {

    /**
     * La partie gardée, vide s'il n'y en a aucune.
     *
     * @throws RuntimeException si une partie existe mais ne se lit pas
     */
    Optional<SavedGame> load();

    /** Remplace la partie gardée. */
    void save(SavedGame game);

    /** Met de côté la partie gardée, qui ne se rejoue plus : elle n'est jamais écrasée, et {@link #load()} n'en rend plus. */
    void setAside();

    /** Garde la dernière partie en mémoire, le temps du processus ; la mettre de côté l'oublie. */
    static GameStore inMemory() {
        var game = new java.util.concurrent.atomic.AtomicReference<SavedGame>();
        return new GameStore() {
            @Override
            public Optional<SavedGame> load() {
                return Optional.ofNullable(game.get());
            }

            @Override
            public void save(SavedGame saved) {
                game.set(saved);
            }

            @Override
            public void setAside() {
                game.set(null);
            }
        };
    }

    /** Ne garde rien : aucune partie à charger, sauvegarder ne fait rien. */
    static GameStore none() {
        return new GameStore() {
            @Override
            public Optional<SavedGame> load() {
                return Optional.empty();
            }

            @Override
            public void save(SavedGame game) {}

            @Override
            public void setAside() {}
        };
    }
}
