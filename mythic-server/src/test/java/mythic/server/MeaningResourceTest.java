package mythic.server;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeaningResourceTest {

    private final DonneesCommunes communes = new DonneesCommunes();

    private MeaningResource tables(int... des) {
        return new MeaningResource(new MeaningStore(communes), Dice.scripted(des));
    }

    private static MythicDtos.MeaningRequest table(String sorte, String element) {
        return new MythicDtos.MeaningRequest(sorte, element);
    }

    @Test
    void sansImport_aucuneTable() {
        assertEquals(new MythicDtos.MeaningTablesDto(false, List.of()), tables().tables());
    }

    @Test
    void lImport_listeActionsDescriptionsPuisLesElementsDansLOrdreDuFichier_etSeGarde() {
        var attendu = new MythicDtos.MeaningTablesDto(true, List.of(
                new MythicDtos.MeaningTableDto("ACTIONS", null, "Actions"),
                new MythicDtos.MeaningTableDto("DESCRIPTIONS", null, "Descriptions"),
                new MythicDtos.MeaningTableDto("ELEMENT", "Lieux", "Lieux"),
                new MythicDtos.MeaningTableDto("ELEMENT", "Ton de l'aventure", "Ton de l'aventure")));

        assertEquals(attendu, tables().importTables(TablesDeTest.texte()));
        assertEquals(attendu, tables().tables());
        assertTrue(communes.fichiers.containsKey("mythic"));
    }

    @Test
    void unTirage_rendDeuxMotsEtLeursJets() {
        tables().importTables(TablesDeTest.texte());

        assertEquals(new MythicDtos.MeaningDto("Actions", "action1-001", 1, "action2-057", 57),
                tables(1, 57).draw(table("ACTIONS", null)));
        assertEquals(new MythicDtos.MeaningDto("Descriptions", "description1-002", 2, "description2-003", 3),
                tables(2, 3).draw(table("DESCRIPTIONS", null)));
        assertEquals(new MythicDtos.MeaningDto("Lieux", "lieu-010", 10, "lieu-099", 99),
                tables(10, 99).draw(table("ELEMENT", "Lieux")));
    }

    @Test
    void unFichierMalForme_estRefuse_etGardeLesTablesDAvant() {
        tables().importTables(TablesDeTest.texte());
        String avant = communes.fichiers.get("mythic");

        var refus = assertThrows(IllegalArgumentException.class,
                () -> tables().importTables(TablesDeTest.texte().replace("action2-100\n", "")));

        assertEquals("Actions 2 : 99 entrées, 100 attendues (ligne 102)", refus.getMessage());
        assertEquals(avant, communes.fichiers.get("mythic"));
    }

    @Test
    void sansTables_leTirageEstRefuseEn409() {
        var refus = assertThrows(WebApplicationException.class, () -> tables(1, 1).draw(table("ACTIONS", null)));

        assertEquals(409, refus.getResponse().getStatus());
        assertEquals(MeaningResource.NOT_IMPORTED, refus.getMessage());
    }

    @Test
    void uneTableAbsenteOuInconnue_estRefusee() {
        tables().importTables(TablesDeTest.texte());

        assertThrows(IllegalArgumentException.class, () -> tables(1, 1).draw(null));
        assertThrows(IllegalArgumentException.class, () -> tables(1, 1).draw(table(null, null)));
        assertThrows(IllegalArgumentException.class, () -> tables(1, 1).draw(table("SORTS", null)));
        assertThrows(IllegalArgumentException.class, () -> tables(1, 1).draw(table("ELEMENT", null)));
        assertThrows(IllegalArgumentException.class, () -> tables(1, 1).draw(table("ELEMENT", "Inconnue")));
    }

    @Test
    void unDocumentAbime_vautSansTables_etLImportLeRemplace() {
        communes.write("mythic", "{pas du json");
        assertEquals(new MythicDtos.MeaningTablesDto(false, List.of()), tables().tables());

        tables().importTables(TablesDeTest.texte());
        assertTrue(tables().tables().imported());
    }
}
