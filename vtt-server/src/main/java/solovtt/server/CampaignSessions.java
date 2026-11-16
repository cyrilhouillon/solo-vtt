package solovtt.server;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import solovtt.engine.Dice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * La partie de chaque campagne, en mémoire : créée à la première demande en rejouant ce que la campagne a gardé, puis
 * gardée le temps du processus. Deux campagnes jouent côte à côte, sans se marcher dessus. Les coups reçus par un
 * personnage joueur posé suivent sa fiche, par le catalogue de la campagne.
 */
@ApplicationScoped
public class CampaignSessions {

    private final Dice dice;
    private final CampaignStore campaigns;
    /** Le suivi des fiches de chaque campagne. */
    private final Function<String, CharacterSheetFollower> sheets;
    private final Map<String, SceneSession> sessions = new ConcurrentHashMap<>();

    @Inject
    public CampaignSessions(Dice dice, CampaignStore campaigns, CampaignCatalog catalog) {
        this(dice, campaigns, campaignId -> (characterId, events) -> catalog.sustained(campaignId, characterId, events));
    }

    /** Des parties sans fiches à suivre : sert aux tests, sans Quarkus. */
    CampaignSessions(Dice dice, CampaignStore campaigns) {
        this(dice, campaigns, campaignId -> CharacterSheetFollower.NONE);
    }

    private CampaignSessions(Dice dice, CampaignStore campaigns, Function<String, CharacterSheetFollower> sheets) {
        this.dice = dice;
        this.campaigns = campaigns;
        this.sheets = sheets;
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
                combatId -> campaigns.notebook(key).createCombatEntry(combatId, "Combat " + campaigns.combats(key).ids().size()),
                sheets.apply(key)));
    }
}
