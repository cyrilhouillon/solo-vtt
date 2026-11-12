package dnd.engine.catalog;

import dnd.engine.state.Creature;
import dnd.engine.state.CreatureProfile;

import java.util.function.BiFunction;

/**
 * Une créature prête à l'emploi. {@code factory} reçoit l'id et le nom de l'instance à créer. {@code profile} est la copie
 * d'un personnage joueur prise à cet instant, de quoi le recréer sans relire sa fiche ; {@code null} pour une créature
 * décrite par le code (monstre du SRD).
 */
public record CatalogEntry(String id, String name, Kind kind, CreatureProfile profile,
                           BiFunction<String, String, Creature> factory) {

    public enum Kind {
        CHARACTER("Personnage"),
        MONSTER("Monstre");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Une créature décrite par le code. */
    public CatalogEntry(String id, String name, Kind kind, BiFunction<String, String, Creature> factory) {
        this(id, name, kind, null, factory);
    }

    /** Un personnage joueur, copié dans {@code profile}. */
    public static CatalogEntry character(String id, String name, CreatureProfile profile) {
        return new CatalogEntry(id, name, Kind.CHARACTER, profile, profile::create);
    }

    public Creature create(String instanceId) {
        return create(instanceId, name);
    }

    public Creature create(String instanceId, String instanceName) {
        return factory.apply(instanceId, instanceName);
    }
}
