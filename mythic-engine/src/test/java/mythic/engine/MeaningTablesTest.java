package mythic.engine;

import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static mythic.engine.TablesDeTest.colonne;
import static mythic.engine.TablesDeTest.tables;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MeaningTablesTest {

    @Test
    void lesActions_tirentUnMotDansChaqueColonne() {
        assertEquals(new Meaning("Actions", "action1-001", 1, "action2-057", 57), tables().actions(Dice.scripted(1, 57)));
    }

    @Test
    void lesDescriptions_tirentUnMotDansChaqueColonne() {
        assertEquals(new Meaning("Descriptions", "description1-100", 100, "description2-042", 42),
                tables().descriptions(Dice.scripted(100, 42)));
    }

    @Test
    void uneTableDElements_estTireeDeuxFois() {
        assertEquals(new Meaning("Lieux", "lieu-003", 3, "lieu-003", 3), tables().element("Lieux", Dice.scripted(3, 3)));
    }

    @Test
    void uneTableDElementsInconnue_estRefusee() {
        assertThrows(IllegalArgumentException.class, () -> tables().element("Inconnue", Dice.scripted(1, 1)));
    }

    @Test
    void uneEntreeVide_estRefusee() {
        List<String> trouee = new ArrayList<>(colonne("action1"));
        trouee.set(9, "  ");

        var refus = assertThrows(IllegalArgumentException.class, () -> new MeaningTables(trouee, colonne("a2"),
                colonne("d1"), colonne("d2"), Map.of()));
        assertEquals("Actions 1 : entrée 10 vide", refus.getMessage());
    }
}
