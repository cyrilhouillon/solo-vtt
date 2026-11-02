package solovtt.server;

import java.util.Optional;

/** Un {@link GameStore} en mémoire, écrit à la main pour les tests de session : garde la dernière partie et compte les appels. */
class InMemoryGameStore implements GameStore {

    private SavedGame game;
    private RuntimeException loadFailure;
    int saves;
    int setAsides;

    InMemoryGameStore() {}

    InMemoryGameStore(SavedGame game) {
        this.game = game;
    }

    /** Un store dont la partie ne se lit pas. */
    static InMemoryGameStore unreadable() {
        var store = new InMemoryGameStore();
        store.loadFailure = new IllegalStateException("partie illisible");
        return store;
    }

    SavedGame game() {
        return game;
    }

    @Override
    public Optional<SavedGame> load() {
        if (loadFailure != null) throw loadFailure;
        return Optional.ofNullable(game);
    }

    @Override
    public void save(SavedGame game) {
        this.game = game;
        saves++;
    }

    @Override
    public void setAside() {
        game = null;
        loadFailure = null;
        setAsides++;
    }
}
