package mythic.server;

import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;
import solovtt.module.ModuleData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MythicResourceTest {

    private static final String STRAHD = "strahd";
    private static final String PHANDELVER = "phandelver";

    /** Les données des modules de deux campagnes, en mémoire. */
    private static final class Donnees implements ModuleData {
        final Map<String, String> fichiers = new HashMap<>();

        @Override
        public boolean exists(String campaignId) {
            return Set.of(STRAHD, PHANDELVER).contains(campaignId);
        }

        @Override
        public Optional<String> read(String campaignId, String moduleId) {
            return Optional.ofNullable(fichiers.get(campaignId + "/" + moduleId));
        }

        @Override
        public void write(String campaignId, String moduleId, String json) {
            fichiers.put(campaignId + "/" + moduleId, json);
        }
    }

    private final Donnees donnees = new Donnees();
    private final DonneesCommunes communes = new DonneesCommunes();
    private final PagesDeTest pages = new PagesDeTest();

    private MythicResource oracle(int... des) {
        return new MythicResource(donnees, pages, new MeaningStore(communes), Dice.scripted(des));
    }

    @Test
    void uneCampagneNeuve_aUnChaosDe5() {
        assertEquals(5, oracle().chaos(STRAHD).chaos());
    }

    @Test
    void leChaosSeGarde_parCampagne() {
        oracle().setChaos(STRAHD, new MythicDtos.ChaosDto(7));

        assertEquals(7, oracle().chaos(STRAHD).chaos());
        assertEquals(5, oracle().chaos(PHANDELVER).chaos());
        assertEquals("{\"chaos\":7}", donnees.fichiers.get(STRAHD + "/mythic"));
    }

    @Test
    void unChaosHorsDe1A9_estRefuse_sansRienEcrire() {
        assertThrows(IllegalArgumentException.class, () -> oracle().setChaos(STRAHD, new MythicDtos.ChaosDto(0)));
        assertThrows(IllegalArgumentException.class, () -> oracle().setChaos(STRAHD, new MythicDtos.ChaosDto(10)));
        assertThrows(IllegalArgumentException.class, () -> oracle().setChaos(STRAHD, null));
        assertEquals(Map.of(), donnees.fichiers);
    }

    @Test
    void uneQuestion_repondAuChaosDeLaCampagne() {
        oracle().setChaos(STRAHD, new MythicDtos.ChaosDto(4));

        var reponse = oracle(42).ask(STRAHD, new MythicDtos.QuestionRequest("  Le garde est-il corrompu ?  ", "LIKELY"));

        assertEquals("Le garde est-il corrompu ?", reponse.question());
        assertEquals(42, reponse.roll());
        assertEquals(new MythicDtos.OddsDto("LIKELY", "Probable"), reponse.odds());
        assertEquals(4, reponse.chaos());
        assertEquals("YES", reponse.answer());
        assertEquals("Oui", reponse.answerLabel());
        assertNull(reponse.randomEvent());
    }

    @Test
    void unDoubleSousLeChaos_rendLEvenementEtSonFocus() {
        var reponse = oracle(33, 25).ask(STRAHD, new MythicDtos.QuestionRequest(null, "FIFTY_FIFTY"));

        assertNull(reponse.question());
        assertEquals(new MythicDtos.RandomEventDto("NPC_ACTION", "Action d'un PNJ", 25, null), reponse.randomEvent());
    }

    @Test
    void uneProbabiliteAbsenteOuInconnue_estRefusee() {
        assertThrows(IllegalArgumentException.class, () -> oracle(42).ask(STRAHD, new MythicDtos.QuestionRequest("?", null)));
        assertThrows(IllegalArgumentException.class, () -> oracle(42).ask(STRAHD, new MythicDtos.QuestionRequest("?", "PEUT_ETRE")));
        assertThrows(IllegalArgumentException.class, () -> oracle(42).ask(STRAHD, null));
        assertThrows(IllegalArgumentException.class, () -> oracle(42).ask(STRAHD, new MythicDtos.QuestionRequest("x".repeat(501), "LIKELY")));
    }

    @Test
    void uneCampagneInconnue_rend404() {
        assertThrows(NotFoundException.class, () -> oracle().chaos("inconnue"));
        assertThrows(NotFoundException.class, () -> oracle().setChaos("inconnue", new MythicDtos.ChaosDto(6)));
        assertThrows(NotFoundException.class, () -> oracle(42).ask("inconnue", new MythicDtos.QuestionRequest(null, "LIKELY")));
    }

    @Test
    void lesProbabilites_vontDImpossibleACertain() {
        var odds = oracle().odds();
        assertEquals(9, odds.size());
        assertEquals("Impossible", odds.getFirst().label());
        assertEquals("Certain", odds.getLast().label());
    }

    @Test
    void unFichierAbime_repartDuChaosDeDepart() {
        donnees.write(STRAHD, "mythic", "{pas du json");
        assertEquals(5, oracle().chaos(STRAHD).chaos());
    }

    @Test
    void avecLesTablesImportees_unEvenementPorteSonSens() {
        new MeaningResource(new MeaningStore(communes), Dice.scripted()).importTables(TablesDeTest.texte());

        var reponse = oracle(33, 25, 4, 90).ask(STRAHD, new MythicDtos.QuestionRequest(null, "FIFTY_FIFTY"));

        assertEquals(new MythicDtos.MeaningDto("Actions", "action1-004", 4, "action2-090", 90), reponse.randomEvent().meaning());
    }

    @Test
    void lesListes_sontCreeesUneFois_videsDansLaSectionMythic() {
        var listes = oracle().lists(STRAHD);

        assertEquals(Map.of(listes.threadsPageId(), "Fils", listes.charactersPageId(), "Personnages"), pages.titres(STRAHD));
        pages.racines.forEach(r -> {
            assertEquals("mythic", r.moduleId());
            assertEquals(MythicResource.EMPTY_LIST, r.content());
        });
        assertEquals("{\"threadsPageId\":\"" + listes.threadsPageId() + "\",\"charactersPageId\":\""
                + listes.charactersPageId() + "\"}", donnees.fichiers.get(STRAHD + "/mythic"));

        assertEquals(listes, oracle().lists(STRAHD));
        assertEquals(2, pages.racines.size());
        oracle().lists(PHANDELVER);
        assertEquals(4, pages.racines.size());
    }

    @Test
    void uneListeRenommee_estGardee() {
        var listes = oracle().lists(STRAHD);
        pages.renommer(listes.threadsPageId(), "Intrigues");

        assertEquals(listes, oracle().lists(STRAHD));
        assertEquals(2, pages.racines.size());
    }

    @Test
    void leChaosEtLesListes_surviventLUnALAutre() {
        oracle().setChaos(STRAHD, new MythicDtos.ChaosDto(7));
        var listes = oracle().lists(STRAHD);
        assertEquals(7, oracle().chaos(STRAHD).chaos());

        oracle().setChaos(STRAHD, new MythicDtos.ChaosDto(3));
        assertEquals(listes, oracle().lists(STRAHD));
        assertEquals(3, oracle().chaos(STRAHD).chaos());
        assertEquals(2, pages.racines.size());
    }

    @Test
    void desIdsPerdus_reprennentLesRacinesParLeurTitre_sansDoublon() {
        var listes = oracle().lists(STRAHD);
        donnees.fichiers.put(STRAHD + "/mythic", "{abîmé");

        assertEquals(listes, oracle().lists(STRAHD));
        assertEquals(2, pages.racines.size());
        assertEquals(5, oracle().chaos(STRAHD).chaos());

        donnees.fichiers.remove(STRAHD + "/mythic");
        assertEquals(listes, oracle().lists(STRAHD));
        assertEquals(2, pages.racines.size());
    }

    @Test
    void unIdGardeDUnePageDisparue_creeUneNouvellePage() {
        donnees.fichiers.put(STRAHD + "/mythic", "{\"chaos\":6,\"threadsPageId\":\"disparue\"}");

        var listes = oracle().lists(STRAHD);

        assertEquals(Map.of(listes.threadsPageId(), "Fils", listes.charactersPageId(), "Personnages"), pages.titres(STRAHD));
        assertEquals(6, oracle().chaos(STRAHD).chaos());
    }

    @Test
    void lesListesDUneCampagneInconnue_sont404() {
        assertThrows(NotFoundException.class, () -> oracle().lists("inconnue"));
        assertEquals(List.of(), pages.racines);
    }
}
