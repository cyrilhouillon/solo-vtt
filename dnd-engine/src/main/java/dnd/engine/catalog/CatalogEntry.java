package dnd.engine.catalog;

import dnd.engine.state.Creature;

import java.util.function.Function;

/** Une créature prête à l'emploi. {@code factory} reçoit l'id de l'instance à créer. */
public record CatalogEntry(String id, String name, Kind kind, Function<String, Creature> factory) {

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

    public Creature create(String instanceId) {
        return factory.apply(instanceId);
    }
}
