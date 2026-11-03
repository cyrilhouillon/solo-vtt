package solovtt.server;

import org.junit.jupiter.api.Test;
import solovtt.scene.DistanceUnit;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapStoreTest {

    private static SavedMap carte(String nom, double largeur) {
        return new SavedMap(nom, largeur, 20, DistanceUnit.METRE, null, true, List.of(new SavedMap.SavedToken("gobelin", 12, 1.5)));
    }

    @Test
    void enMemoire_listeParNom_etRemplaceLeMemeNom() {
        var store = MapStore.inMemory();
        store.save(carte("Grotte", 30));
        store.save(carte("crypte", 30));
        store.save(carte("Grotte", 45));

        assertEquals(List.of("crypte", "Grotte"), store.list().stream().map(SavedMap::name).toList());
        assertEquals(Optional.of(carte("Grotte", 45)), store.find("Grotte"));
        assertEquals(Optional.empty(), store.find("Marais"));
    }
}
