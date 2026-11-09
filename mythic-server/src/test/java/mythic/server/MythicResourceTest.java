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

    private static final MythicDtos.ListDrawDto PERSONNAGES_VIDE = new MythicDtos.ListDrawDto("CHARACTERS", "Personnages",
            true, false, null, null, null, null, false, null, null);

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
        assertEquals(new MythicDtos.RandomEventDto("NPC_ACTION", "Action d'un PNJ", 25, null, "CURRENT_CONTEXT",
                "Contexte actuel", PERSONNAGES_VIDE), reponse.randomEvent());
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

    private static String liste(String... elements) {
        StringBuilder items = new StringBuilder();
        for (String element : elements) {
            if (items.length() > 0) items.append(',');
            items.append("{\"type\":\"listItem\",\"content\":[{\"type\":\"paragraph\"")
                    .append(element.isEmpty() ? "" : ",\"content\":[" + element + "]").append("}]}");
        }
        return "{\"type\":\"doc\",\"content\":[{\"type\":\"bulletList\",\"content\":[" + items + "]}]}";
    }

    private static String texte(String texte) {
        return "{\"type\":\"text\",\"text\":\"" + texte + "\"}";
    }

    private static final String LIEN_VESSA = "{\"type\":\"pageLink\",\"attrs\":{\"pageId\":\"vessa\",\"label\":null}}";

    /** Les listes de la campagne, Personnages : « Garde Ylo », lien vers Vessa, « Garde Ylo », ligne vide. */
    private MythicDtos.ListsDto listesEcrites() {
        var listes = oracle().lists(STRAHD);
        pages.ecrire(listes.charactersPageId(), liste(texte("Garde Ylo"), LIEN_VESSA, texte("Garde Ylo"), ""));
        pages.pageOrdinaire(STRAHD, "vessa", "Vessa");
        return listes;
    }

    @Test
    void unFocusDePnj_tireDansPersonnages_etRendLEntree() {
        listesEcrites();

        var liste = oracle(33, 25, 5).ask(STRAHD, new MythicDtos.QuestionRequest(null, "FIFTY_FIFTY")).randomEvent().list();

        assertEquals("CHARACTERS", liste.list());
        assertEquals(3, liste.line());
        assertEquals(5, liste.lineRoll());
        assertNull(liste.sectionDie());
        assertEquals("Garde Ylo", liste.entryText());
        assertEquals("Garde Ylo", liste.entry().get(0).path("text").asText());
    }

    @Test
    void laLigneTireeEstUnLien_lEntreeEstLeLienDOrigine_luParSonTitre() {
        listesEcrites();

        var evenement = oracle(33, 41, 3).ask(STRAHD, new MythicDtos.QuestionRequest(null, "FIFTY_FIFTY")).randomEvent();

        assertEquals("NPC_NEGATIVE", evenement.effectiveFocus());
        assertEquals("pageLink", evenement.list().entry().get(0).path("type").asText());
        assertEquals("vessa", evenement.list().entry().get(0).path("attrs").path("pageId").asText());
        assertEquals("Vessa", evenement.list().entryText());
    }

    @Test
    void uneLigneVideTiree_vautChoisir_sansEntree() {
        listesEcrites();

        var liste = oracle(33, 25, 8).ask(STRAHD, new MythicDtos.QuestionRequest(null, "FIFTY_FIFTY")).randomEvent().list();

        assertEquals(4, liste.line());
        assertEquals(true, liste.blank());
        assertNull(liste.entry());
        assertNull(liste.entryText());
    }

    @Test
    void filsVide_unFocusDeFil_devientContexteActuel_sansDeDePlus() {
        listesEcrites();

        var evenement = oracle(33, 60).ask(STRAHD, new MythicDtos.QuestionRequest(null, "FIFTY_FIFTY")).randomEvent();

        assertEquals("MOVE_AWAY_FROM_A_THREAD", evenement.focus());
        assertEquals("CURRENT_CONTEXT", evenement.effectiveFocus());
        assertEquals(true, evenement.list().empty());
        assertEquals("Fils", evenement.list().listLabel());
    }

    @Test
    void desListesPerdues_ouUnContenuIllisible_sontVides() {
        var listes = oracle().lists(STRAHD);
        pages.ecrire(listes.charactersPageId(), "{pas du json");
        assertEquals(true, oracle(33, 25).ask(STRAHD, new MythicDtos.QuestionRequest(null, "LIKELY")).randomEvent().list().empty());

        donnees.fichiers.put(STRAHD + "/mythic", "{\"chaos\":5,\"charactersPageId\":\"pas-une-racine\"}");
        assertEquals(true, oracle(33, 25).ask(STRAHD, new MythicDtos.QuestionRequest(null, "LIKELY")).randomEvent().list().empty());
    }

    @Test
    void lesListesDUneAutreCampagne_neComptentPas() {
        listesEcrites();

        var liste = oracle(33, 25).ask(PHANDELVER, new MythicDtos.QuestionRequest(null, "LIKELY")).randomEvent().list();

        assertEquals(true, liste.empty());
    }

    @Test
    void uneSceneAuDessusDuChaos_estAttendue_etRevientNettoyee() {
        var scene = oracle(6).testScene(STRAHD, new MythicDtos.SceneRequest("  On entre au donjon  "));

        assertEquals(new MythicDtos.SceneDto("On entre au donjon", 5, 6, "EXPECTED", "Scène attendue", List.of(), List.of(),
                null), scene);
    }

    @Test
    void uneSceneImpaireSousLeChaos_estModifiee_avecSonAjustement() {
        oracle().setChaos(STRAHD, new MythicDtos.ChaosDto(7));

        var scene = oracle(5, 4).testScene(STRAHD, new MythicDtos.SceneRequest(null));

        assertNull(scene.expected());
        assertEquals(7, scene.chaos());
        assertEquals("ALTERED", scene.outcome());
        assertEquals(List.of(new MythicDtos.AdjustmentDto("INCREASE_AN_ACTIVITY", "Intensifier une activité")), scene.adjustments());
        assertEquals(List.of(4), scene.adjustmentRolls());
        assertNull(scene.randomEvent());
        assertEquals(7, oracle().chaos(STRAHD).chaos());
    }

    @Test
    void uneScenePaireSousLeChaos_estInterrompue_parUnEvenementQuiTireDansLesListes() {
        listesEcrites();

        var scene = oracle(2, 25, 5).testScene(STRAHD, null);

        assertEquals("INTERRUPTED", scene.outcome());
        assertEquals("Scène interrompue", scene.outcomeLabel());
        assertEquals("NPC_ACTION", scene.randomEvent().focus());
        assertEquals("Garde Ylo", scene.randomEvent().list().entryText());
    }

    @Test
    void uneSceneInterrompue_PersonnagesPerdu_devientContexteActuel() {
        donnees.fichiers.put(STRAHD + "/mythic", "{\"chaos\":5,\"charactersPageId\":\"pas-une-racine\"}");

        var evenement = oracle(4, 25).testScene(STRAHD, null).randomEvent();

        assertEquals("CURRENT_CONTEXT", evenement.effectiveFocus());
    }

    @Test
    void uneSceneTropLongue_estRefusee_sansDe() {
        assertThrows(IllegalArgumentException.class,
                () -> oracle().testScene(STRAHD, new MythicDtos.SceneRequest("x".repeat(501))));
    }

    @Test
    void laSceneDUneCampagneInconnue_est404() {
        assertThrows(NotFoundException.class, () -> oracle(6).testScene("inconnue", null));
    }

    @Test
    void finDeScene_controleGarde_baisseLeChaos_etGardeLesListes() {
        var listes = oracle().lists(STRAHD);

        assertEquals(new MythicDtos.SceneEndDto(true, 5, 4), oracle().endScene(STRAHD, new MythicDtos.SceneEndRequest(true)));
        assertEquals(4, oracle().chaos(STRAHD).chaos());
        assertEquals(listes, oracle().lists(STRAHD));
        assertEquals(2, pages.racines.size());
        assertEquals(5, oracle().chaos(PHANDELVER).chaos());
    }

    @Test
    void finDeScene_controlePerdu_monteLeChaos_sansDepasser9() {
        assertEquals(new MythicDtos.SceneEndDto(false, 5, 6), oracle().endScene(STRAHD, new MythicDtos.SceneEndRequest(false)));
        oracle().setChaos(STRAHD, new MythicDtos.ChaosDto(9));
        assertEquals(new MythicDtos.SceneEndDto(false, 9, 9), oracle().endScene(STRAHD, new MythicDtos.SceneEndRequest(false)));
        assertEquals(9, oracle().chaos(STRAHD).chaos());
    }

    @Test
    void finDeScene_sansControle_estRefusee_sansRienEcrire() {
        assertThrows(IllegalArgumentException.class, () -> oracle().endScene(STRAHD, null));
        assertThrows(IllegalArgumentException.class, () -> oracle().endScene(STRAHD, new MythicDtos.SceneEndRequest(null)));
        assertEquals(Map.of(), donnees.fichiers);
    }

    @Test
    void laFinDeSceneDUneCampagneInconnue_est404() {
        assertThrows(NotFoundException.class, () -> oracle().endScene("inconnue", new MythicDtos.SceneEndRequest(true)));
    }
}
