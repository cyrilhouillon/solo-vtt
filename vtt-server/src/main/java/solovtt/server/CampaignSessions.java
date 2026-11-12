package solovtt.server;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import solovtt.engine.Dice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * La partie de chaque campagne, en mémoire : créée à la première demande en rejouant ce que la campagne a gardé, puis
 * gardée le temps du processus. Deux campagnes jouent côte à côte, sans se marcher dessus.
 */
@ApplicationScoped
public class CampaignSessions {

    private final Dice dice;
    private final CampaignStore campaigns;
    private final Map<String, SceneSession> sessions = new ConcurrentHashMap<>();

    public CampaignSessions(Dice dice, CampaignStore campaigns) {
        this.dice = dice;
        this.campaigns = campaigns;
    }

    /**
     * La partie de la campagne {@code id}.
     *
     * @throws NotFoundException si aucune campagne n'a cet id (404)
     */
    public SceneSession of(String id) {
        SceneSession session = sessions.get(id);
        if (session != null) return session;
        if (campaigns.find(id).isEmpty()) throw new NotFoundException("Campagne inconnue : " + id);
        return sessions.computeIfAbsent(id, key -> new SceneSession(dice, campaigns.games(key), campaigns.maps(key), campaigns.combats(key),
                combatId -> campaigns.notebook(key).createCombatEntry(combatId, "Combat " + campaigns.combats(key).ids().size())));
    }
}
