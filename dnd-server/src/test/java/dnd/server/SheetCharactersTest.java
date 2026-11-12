package dnd.server;

import dnd.engine.catalog.PlayerCharacters;
import dnd.engine.sheet.Build;
import dnd.engine.state.Creature;
import jakarta.ws.rs.ClientErrorException;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SheetCharactersTest {

    private static final String STRAHD = "strahd";

    private final SheetResourceTest.Donnees donnees = new SheetResourceTest.Donnees();
    private final SheetResourceTest.Pages pages = new SheetResourceTest.Pages();
    private final SheetResource dnd = new SheetResource(donnees, pages, Dice.scripted());
    private final SheetCharacters personnages = new SheetCharacters(donnees, pages);

    private String guerrier(String nom) {
        String id = dnd.create(STRAHD, new DndDtos.NewSheetRequest(nom)).pageId();
        dnd.build(STRAHD, id, DndDtos.BuildDto.of(Build.fighter()));
        return id;
    }

    @Test
    void lesFichesVivantes_sontListeesParNom_sousLeTitreDeLeurPage() {
        String zed = guerrier("Zed");
        String aria = guerrier("Aria");
        String perdu = guerrier("Perdu");
        pages.supprimer(perdu);

        assertEquals(List.of(new PlayerCharacters.Listed(aria, "Aria"), new PlayerCharacters.Listed(zed, "Zed")),
                personnages.list(STRAHD));
        assertEquals("dnd", personnages.moduleId());
    }

    @Test
    void laCopie_suitLaFicheDuMoment_etSonTitre() {
        String aria = guerrier("Aria");
        pages.renommer(aria, "Aria la Brave");

        PlayerCharacters.Taken copie = personnages.take(STRAHD, aria).orElseThrow();
        Creature creature = copie.profile().create("c-1", copie.name());

        assertEquals("Aria la Brave", copie.name());
        assertEquals(dnd.read(STRAHD, aria).values().armorClass(), creature.armorClass());
        assertEquals(dnd.read(STRAHD, aria).values().hitPointMax(), creature.maxHitPoints());
        assertEquals(List.of("Épée à deux mains", "Fléau d’armes", "Javeline", "Javeline (lancer)"),
                creature.attacks().stream().map(a -> a.label()).toList());
    }

    @Test
    void uneFicheVierge_neJouePas_etUnePageQuiNEnEstPasUne_estInconnue() {
        String vierge = dnd.create(STRAHD, new DndDtos.NewSheetRequest("Vierge")).pageId();

        var refus = assertThrows(IllegalArgumentException.class, () -> personnages.take(STRAHD, vierge));
        assertEquals("PV maximum à 0 : compléter la fiche avant de la poser", refus.getMessage());
        assertEquals(Optional.empty(), personnages.take(STRAHD, pages.pageOrdinaire(STRAHD, "Notes")));
        assertEquals(Optional.empty(), personnages.take(STRAHD, "inconnue"));
    }

    @Test
    void unDocumentAbime_videLaListe_maisRefuseLaCopieEn409() {
        String aria = guerrier("Aria");
        donnees.fichiers.put(STRAHD + "/dnd", "{abîmé");

        assertTrue(personnages.list(STRAHD).isEmpty());
        var refus = assertThrows(ClientErrorException.class, () -> personnages.take(STRAHD, aria));
        assertEquals(409, refus.getResponse().getStatus());
    }
}
