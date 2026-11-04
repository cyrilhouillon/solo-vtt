package solovtt.server;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Où vivent les campagnes. Chacune garde sa partie ({@link GameStore}) et ses cartes enregistrées ({@link MapStore}) ;
 * les images de fond restent communes ({@link ImageStore}).
 */
public interface CampaignStore {

    /** Les campagnes, triées par nom. */
    List<Campaign> list();

    /** Crée une campagne sous ce nom ; deux campagnes peuvent porter le même nom. */
    Campaign create(String name);

    /** La campagne de cet id, vide si aucune. */
    default Optional<Campaign> find(String id) {
        return list().stream().filter(c -> c.id().equals(id)).findFirst();
    }

    /**
     * La partie de la campagne.
     *
     * @throws IllegalArgumentException si aucune campagne n'a cet id
     */
    GameStore games(String id);

    /**
     * Les cartes enregistrées de la campagne.
     *
     * @throws IllegalArgumentException si aucune campagne n'a cet id
     */
    MapStore maps(String id);

    Comparator<Campaign> BY_NAME = Comparator.comparing(Campaign::name, String.CASE_INSENSITIVE_ORDER).thenComparing(Campaign::name);

    /** Garde les campagnes, leurs parties et leurs cartes en mémoire, le temps du processus. */
    static CampaignStore inMemory() {
        record Content(Campaign campaign, GameStore games, MapStore maps) {}
        Map<String, Content> campaigns = new ConcurrentHashMap<>();
        return new CampaignStore() {
            @Override
            public List<Campaign> list() {
                return campaigns.values().stream().map(Content::campaign).sorted(BY_NAME).toList();
            }

            @Override
            public Campaign create(String name) {
                var campaign = new Campaign(UUID.randomUUID().toString(), name);
                campaigns.put(campaign.id(), new Content(campaign, GameStore.inMemory(), MapStore.inMemory()));
                return campaign;
            }

            @Override
            public GameStore games(String id) {
                return content(id).games();
            }

            @Override
            public MapStore maps(String id) {
                return content(id).maps();
            }

            private Content content(String id) {
                Content content = campaigns.get(id);
                if (content == null) throw new IllegalArgumentException("Campagne inconnue : " + id);
                return content;
            }
        };
    }
}
