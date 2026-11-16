package dnd.server;

import dnd.engine.catalog.PlayerCharacters;
import dnd.engine.events.GameEvent;
import dnd.engine.sheet.SheetEvent;
import dnd.engine.sheet.SheetHistory;
import dnd.engine.sheet.SheetState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ClientErrorException;
import solovtt.module.ModuleData;
import solovtt.module.NotebookPages;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Les personnages joueurs d'une campagne, pour le catalogue de préparation : chaque fiche vivante, sous le titre de sa page,
 * et sa copie à la pose, tirée de sa fiche à cet instant. L'id d'un personnage est celui de sa page.
 */
@ApplicationScoped
public class SheetCharacters implements PlayerCharacters {

    private static final System.Logger LOG = System.getLogger(SheetCharacters.class.getName());
    private static final Comparator<Listed> BY_NAME =
            Comparator.comparing(Listed::name, Collator.getInstance(Locale.FRENCH)).thenComparing(Listed::id);

    private final NotebookPages pages;
    private final SheetStore store;
    /** Écrit les coups reçus sous le verrou des saisies de fiche. */
    private final SheetResource sheets;

    @Inject
    public SheetCharacters(ModuleData data, NotebookPages pages, SheetResource sheets) {
        this.pages = pages;
        this.store = new SheetStore(data, SheetResource.MODULE_ID);
        this.sheets = sheets;
    }

    @Override
    public String moduleId() {
        return SheetResource.MODULE_ID;
    }

    /** Les fiches vivantes ; un document abîmé n'en liste aucune, pour que le catalogue garde ses monstres. */
    @Override
    public List<Listed> list(String campaignId) {
        SheetStore.State state;
        try {
            state = store.read(campaignId);
        } catch (ClientErrorException e) {
            LOG.log(System.Logger.Level.WARNING, "Personnages de " + campaignId + " absents du catalogue : " + e.getMessage());
            return List.of();
        }
        return state.sheets().keySet().stream()
                .filter(id -> pages.isEntry(campaignId, moduleId(), id))
                .flatMap(id -> pages.pageTitle(campaignId, id).map(title -> new Listed(id, title)).stream())
                .sorted(BY_NAME)
                .toList();
    }

    /**
     * @throws ClientErrorException (409) si la fiche ne se relit pas
     * @throws IllegalArgumentException si la fiche ne peut pas jouer
     */
    @Override
    public Optional<Taken> take(String campaignId, String id) {
        if (!pages.isEntry(campaignId, moduleId(), id)) return Optional.empty();
        Optional<String> title = pages.pageTitle(campaignId, id);
        if (title.isEmpty()) return Optional.empty();
        SheetStore.State state = store.read(campaignId);
        SheetState sheet;
        try {
            sheet = SheetHistory.replay(state.history(id).orElse(List.of(new SheetEvent.SheetCreated())));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw SheetStore.unreadable();
        }
        return Optional.of(new Taken(title.get(), sheet.values().profile()));
    }

    /**
     * Les dégâts que la copie posée a subis baissent les PV actuels de sa fiche.
     *
     * @return {@code false} si {@code id} n'est pas une fiche vivante de la campagne
     * @throws ClientErrorException (409) si les fiches ne se relisent pas ; rien n'est écrit
     */
    @Override
    public boolean sustained(String campaignId, String id, List<GameEvent> events) {
        if (!pages.isEntry(campaignId, moduleId(), id)) return false;
        List<Integer> amounts = events.stream()
                .filter(GameEvent.DamageDealt.class::isInstance)
                .map(event -> ((GameEvent.DamageDealt) event).amount())
                .toList();
        if (!amounts.isEmpty()) sheets.sustained(campaignId, id, amounts);
        return true;
    }
}
