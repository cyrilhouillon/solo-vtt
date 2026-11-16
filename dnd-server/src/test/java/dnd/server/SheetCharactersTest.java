package dnd.server;

import dnd.engine.catalog.PlayerCharacters;
import dnd.engine.sheet.Build;
import dnd.engine.events.GameEvent;
import dnd.engine.state.Creature;
import dnd.engine.state.DamageType;
import jakarta.ws.rs.ClientErrorException;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SheetCharactersTest {

    private static final String STRAHD = "strahd";

    private final SheetResourceTest.Donnees donnees = new SheetResourceTest.Donnees();
    private final SheetResourceTest.Pages pages = new SheetResourceTest.Pages();
    private final SheetResource dnd = new SheetResource(donnees, pages, Dice.scripted());
    private final SheetCharacters personnages = new SheetCharacters(donnees, pages, dnd);

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
        assertEquals(List.of("Épée à deux mains", "Fléau d’armes", "Javeline", "Attaque à mains nues", "Javeline (lancer)"),
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

    private static GameEvent.DamageDealt coup(int degats) {
        return new GameEvent.DamageDealt("aria-1", degats, DamageType.SLASHING);
    }

    private int pvActuels(String id) {
        return dnd.read(STRAHD, id).values().hitPoints();
    }

    @Test
    void unCoupRecu_baisseLaFiche_relueParUneAutreInstance_etLaCopieSuivante() {
        String aria = guerrier("Aria");
        int pleins = pvActuels(aria);

        assertTrue(personnages.sustained(STRAHD, aria, List.of(coup(5))));

        assertEquals(pleins - 5, pvActuels(aria));
        assertTrue(donnees.fichiers.get(STRAHD + "/dnd").contains("{\"type\":\"lost\",\"amount\":5}"));
        var autreRessource = new SheetResource(donnees, pages, Dice.scripted());
        var autresPersonnages = new SheetCharacters(donnees, pages, autreRessource);
        assertEquals(pleins - 5, autreRessource.read(STRAHD, aria).values().hitPoints());
        assertEquals(pleins - 5, autresPersonnages.take(STRAHD, aria).orElseThrow().profile().create("c", "Aria").hitPoints());
    }

    @Test
    void unCoup_seComposeAvecUneRetouche_etNeDescendPasSous0() {
        String aria = guerrier("Aria");
        var retouchee = dnd.read(STRAHD, aria).values().toSheet().withHitPoints(10);
        dnd.edit(STRAHD, aria, DndDtos.SheetValuesDto.of(retouchee));

        personnages.sustained(STRAHD, aria, List.of(coup(3)));
        assertEquals(7, pvActuels(aria));

        personnages.sustained(STRAHD, aria, List.of(coup(20)));
        assertEquals(0, pvActuels(aria));
    }

    @Test
    void uneAutrePage_nEstPasUnPersonnage_etRienNEstEcrit() {
        String perdu = guerrier("Perdu");
        pages.supprimer(perdu);
        String avant = donnees.fichiers.get(STRAHD + "/dnd");

        assertFalse(personnages.sustained(STRAHD, perdu, List.of(coup(5))));
        assertFalse(personnages.sustained(STRAHD, "inconnue", List.of(coup(5))));
        assertFalse(personnages.sustained(STRAHD, pages.pageOrdinaire(STRAHD, "Notes"), List.of(coup(5))));
        assertEquals(avant, donnees.fichiers.get(STRAHD + "/dnd"));
    }

    @Test
    void sansDegats_rienNEstEcrit() {
        String aria = guerrier("Aria");
        String avant = donnees.fichiers.get(STRAHD + "/dnd");

        assertTrue(personnages.sustained(STRAHD, aria, List.of()));
        assertTrue(personnages.sustained(STRAHD, aria, List.of(new GameEvent.CombatStarted())));
        assertTrue(personnages.sustained(STRAHD, aria, List.of(coup(0))));
        assertEquals(avant, donnees.fichiers.get(STRAHD + "/dnd"));
    }

    @Test
    void unDocumentAbime_refuseLeCoupEn409_sansEtreEcrase() {
        String aria = guerrier("Aria");
        donnees.fichiers.put(STRAHD + "/dnd", "{abîmé");

        var refus = assertThrows(ClientErrorException.class, () -> personnages.sustained(STRAHD, aria, List.of(coup(5))));

        assertEquals(409, refus.getResponse().getStatus());
        assertEquals("{abîmé", donnees.fichiers.get(STRAHD + "/dnd"));
    }

    @Test
    void desPvPerdusSansMontant_rendentLaFicheIllisible() {
        String aria = guerrier("Aria");
        personnages.sustained(STRAHD, aria, List.of(coup(5)));
        donnees.fichiers.computeIfPresent(STRAHD + "/dnd", (k, json) -> json.replace(",\"amount\":5", ""));

        var refus = assertThrows(ClientErrorException.class, () -> dnd.read(STRAHD, aria));
        assertEquals(409, refus.getResponse().getStatus());
    }
}
