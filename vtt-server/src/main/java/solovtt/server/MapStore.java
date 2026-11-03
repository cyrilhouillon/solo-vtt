package solovtt.server;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Où vivent les cartes enregistrées, hors de la partie : la partie en copie une au chargement, sans la référencer.
 * Une carte se retrouve par son nom ; enregistrer sous un nom déjà pris la remplace.
 */
public interface MapStore {

    /**
     * Les cartes enregistrées, triées par nom.
     *
     * @throws RuntimeException si les cartes existent mais ne se lisent pas
     */
    List<SavedMap> list();

    /** La carte enregistrée sous ce nom, vide si aucune. */
    default Optional<SavedMap> find(String name) {
        return list().stream().filter(m -> m.name().equals(name)).findFirst();
    }

    /** Enregistre la carte ; celle qui portait le même nom est remplacée. */
    void save(SavedMap map);

    Comparator<SavedMap> BY_NAME = Comparator.comparing(SavedMap::name, String.CASE_INSENSITIVE_ORDER).thenComparing(SavedMap::name);

    /** Garde les cartes en mémoire, le temps du processus. */
    static MapStore inMemory() {
        Map<String, SavedMap> maps = new ConcurrentHashMap<>();
        return new MapStore() {
            @Override
            public List<SavedMap> list() {
                return maps.values().stream().sorted(BY_NAME).toList();
            }

            @Override
            public void save(SavedMap map) {
                maps.put(map.name(), map);
            }
        };
    }
}
