package solovtt.server;

import dnd.engine.catalog.CatalogEntry;
import dnd.engine.catalog.PlayerCharacters;
import dnd.engine.catalog.SrdCatalog;
import dnd.engine.events.GameEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Le catalogue de préparation d'une campagne : les personnages joueurs des modules actifs ({@link PlayerCharacters}, que
 * le cœur collecte sans en nommer aucun), puis les monstres du SRD. Poser un personnage en prend une copie à cet instant.
 */
@ApplicationScoped
public class CampaignCatalog {

    private final CampaignStore campaigns;
    private final InstalledModules modules;
    private final List<PlayerCharacters> characters;

    @Inject
    public CampaignCatalog(CampaignStore campaigns, InstalledModules modules, Instance<PlayerCharacters> characters) {
        this(campaigns, modules, characters.stream().toList());
    }

    /** Ces sources de personnages : sert aussi aux tests, sans Quarkus. */
    CampaignCatalog(CampaignStore campaigns, InstalledModules modules, List<PlayerCharacters> characters) {
        this.campaigns = campaigns;
        this.modules = modules;
        this.characters = List.copyOf(characters);
    }

    /** Les entrées de la campagne : ses personnages, puis les monstres. */
    public List<Dtos.CatalogEntryDto> entries(String campaignId) {
        List<Dtos.CatalogEntryDto> entries = new ArrayList<>();
        for (PlayerCharacters source : active(campaignId)) {
            for (PlayerCharacters.Listed c : source.list(campaignId)) {
                entries.add(new Dtos.CatalogEntryDto(c.id(), c.name(), CatalogEntry.Kind.CHARACTER));
            }
        }
        SrdCatalog.entries().stream().map(Dtos.CatalogEntryDto::of).forEach(entries::add);
        return entries;
    }

    /**
     * L'entrée {@code id} de la campagne : un monstre, ou la copie d'un personnage prise maintenant ; vide si elle n'existe
     * pas.
     *
     * @throws IllegalArgumentException si la fiche du personnage ne peut pas jouer (400)
     */
    public Optional<CatalogEntry> entry(String campaignId, String id) {
        Optional<CatalogEntry> monster = SrdCatalog.entries().stream().filter(e -> e.id().equals(id)).findFirst();
        if (monster.isPresent()) return monster;
        for (PlayerCharacters source : active(campaignId)) {
            Optional<PlayerCharacters.Taken> taken = source.take(campaignId, id);
            if (taken.isPresent()) return Optional.of(CatalogEntry.character(id, taken.get().name(), taken.get().profile()));
        }
        return Optional.empty();
    }

    /**
     * Fait suivre à la fiche du personnage {@code id} ce que sa copie posée vient de subir, par la première source active
     * qui le reconnaît.
     *
     * @throws IllegalStateException si aucune source active ne le reconnaît (fiche supprimée, module inactif)
     * @throws RuntimeException      si sa fiche n'a pas pu suivre, avec la raison
     */
    public void sustained(String campaignId, String id, List<GameEvent> events) {
        for (PlayerCharacters source : active(campaignId)) {
            if (source.sustained(campaignId, id, events)) return;
        }
        throw new IllegalStateException("fiche introuvable ou module inactif");
    }

    /** Le catalogue d'une campagne, pour une ressource qui pose ou charge une carte. */
    public Function<String, Optional<CatalogEntry>> of(String campaignId) {
        return id -> entry(campaignId, id);
    }

    private List<PlayerCharacters> active(String campaignId) {
        Campaign campaign = campaigns.find(campaignId)
                .orElseThrow(() -> new NotFoundException("Campagne inconnue : " + campaignId));
        List<String> active = modules.activeIn(campaign);
        return characters.stream().filter(c -> active.contains(c.moduleId())).toList();
    }
}
