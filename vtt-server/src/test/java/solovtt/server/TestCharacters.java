package solovtt.server;

import dnd.engine.catalog.CatalogEntry;
import dnd.engine.catalog.PlayerCharacters;
import dnd.engine.catalog.SrdCatalog;
import dnd.engine.rules.DuelingStyle;
import dnd.engine.rules.ShieldReaction;
import dnd.engine.state.CreatureProfile;
import dnd.engine.state.DamageType;
import dnd.engine.state.Weapon;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Deux personnages joueurs de test, servis au catalogue de chaque campagne où le module D&D est actif : la Guerrière
 * (Duel) et la Magicienne (Bouclier, deux emplacements de niveau 1), sous les ids qu'avaient les prétirés. Les tests qui
 * construisent une {@link SceneSession} sans Quarkus les prennent par {@link #entry} et {@link #catalog}.
 */
@ApplicationScoped
public class TestCharacters implements PlayerCharacters {

    /** Niveau 1 : For 17, Dex 14, Con 14 ; cotte de mailles + bouclier ; Épée longue (For +3, maîtrise +2). */
    static final CreatureProfile GUERRIERE = new CreatureProfile(18, 12, 12, 9, 2,
            List.of(new Weapon("Épée longue", 1, 8, DamageType.SLASHING, 5, 3, true)),
            Set.of(DuelingStyle.FEATURE), Set.of(), List.of());

    /** Niveau 1 : Int 17, Dex 14, Con 13 ; sans armure ; Dague (Dex +2, maîtrise +2). */
    static final CreatureProfile MAGICIENNE = new CreatureProfile(12, 7, 7, 9, 2,
            List.of(new Weapon("Dague", 1, 4, DamageType.PIERCING, 4, 2, true).thrown(6, 18)),
            Set.of(), Set.of(ShieldReaction.SPELL), List.of(2));

    private static final Map<String, Taken> CHARACTERS = Map.of(
            "guerriere", new Taken("Guerrière", GUERRIERE),
            "magicienne", new Taken("Magicienne", MAGICIENNE));

    @Override
    public String moduleId() {
        return "dnd";
    }

    @Override
    public List<Listed> list(String campaignId) {
        return List.of(new Listed("guerriere", "Guerrière"), new Listed("magicienne", "Magicienne"));
    }

    @Override
    public Optional<Taken> take(String campaignId, String id) {
        return Optional.ofNullable(CHARACTERS.get(id));
    }

    /** Une entrée : un monstre du SRD ou l'un de ces personnages. */
    static CatalogEntry entry(String id) {
        return catalog().apply(id).orElseThrow(() -> new IllegalArgumentException("Entrée de test inconnue : " + id));
    }

    /** Le catalogue de test : les monstres du SRD et ces personnages. */
    static Function<String, Optional<CatalogEntry>> catalog() {
        return id -> SrdCatalog.entries().stream().filter(e -> e.id().equals(id)).findFirst()
                .or(() -> Optional.ofNullable(CHARACTERS.get(id)).map(t -> CatalogEntry.character(id, t.name(), t.profile())));
    }
}
