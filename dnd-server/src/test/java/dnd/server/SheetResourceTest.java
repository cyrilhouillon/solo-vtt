package dnd.server;

import dnd.engine.sheet.Ability;
import dnd.engine.sheet.Proficiency;
import dnd.engine.sheet.Skill;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import solovtt.module.ModuleData;
import solovtt.module.NotebookPages;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Les fiches des personnages joueurs, sur des ports en mémoire. */
class SheetResourceTest {

    private static final String STRAHD = "strahd";
    private static final String PHANDELVER = "phandelver";

    /** Les données des modules, par campagne. */
    static final class Donnees implements ModuleData {
        final Map<String, String> fichiers = new HashMap<>();

        @Override
        public boolean exists(String campaignId) {
            return campaignId.equals(STRAHD) || campaignId.equals(PHANDELVER);
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

    /** Les Carnets, en mémoire : pages vivantes, racines de section, entrées de module. */
    static final class Pages implements NotebookPages {
        record Page(String campaignId, String id, String title, String parentId, String section, String entryOf) {}

        final List<Page> pages = new ArrayList<>();

        @Override
        public List<SectionPage> sectionRoots(String campaignId, String moduleId) {
            return pages.stream().filter(p -> p.campaignId().equals(campaignId) && moduleId.equals(p.section()))
                    .map(p -> new SectionPage(p.id(), p.title())).toList();
        }

        @Override
        public SectionPage createSectionRoot(String campaignId, String moduleId, String title, String contentJson) {
            var page = new Page(campaignId, UUID.randomUUID().toString(), title, null, moduleId, null);
            pages.add(page);
            return new SectionPage(page.id(), title);
        }

        @Override
        public Optional<String> sectionRootContent(String campaignId, String moduleId, String pageId) {
            return Optional.empty();
        }

        @Override
        public Optional<String> pageTitle(String campaignId, String pageId) {
            return find(campaignId, pageId).map(Page::title);
        }

        @Override
        public List<SectionPage> childPages(String campaignId, String moduleId, String pageId) {
            return List.of();
        }

        @Override
        public Optional<String> sectionPageContent(String campaignId, String moduleId, String pageId) {
            return Optional.empty();
        }

        @Override
        public SectionPage createEntry(String campaignId, String moduleId, String parentId, String title) {
            Page parent = find(campaignId, parentId).filter(p -> moduleId.equals(p.section()))
                    .orElseThrow(() -> new IllegalArgumentException("Une entrée se range dans la section de son module"));
            var page = new Page(campaignId, UUID.randomUUID().toString(), title, parent.id(), null, moduleId);
            pages.add(page);
            return new SectionPage(page.id(), title);
        }

        @Override
        public boolean isEntry(String campaignId, String moduleId, String pageId) {
            return find(campaignId, pageId).filter(p -> moduleId.equals(p.entryOf())).isPresent();
        }

        void renommer(String id, String title) {
            pages.replaceAll(p -> p.id().equals(id) ? new Page(p.campaignId(), id, title, p.parentId(), p.section(), p.entryOf()) : p);
        }

        void supprimer(String id) {
            pages.removeIf(p -> p.id().equals(id));
        }

        String pageOrdinaire(String campaignId, String title) {
            var page = new Page(campaignId, UUID.randomUUID().toString(), title, null, null, null);
            pages.add(page);
            return page.id();
        }

        private Optional<Page> find(String campaignId, String pageId) {
            return pages.stream().filter(p -> p.campaignId().equals(campaignId) && p.id().equals(pageId)).findFirst();
        }
    }

    private final Donnees donnees = new Donnees();
    private final Pages pages = new Pages();

    private SheetResource dnd() {
        return new SheetResource(donnees, pages);
    }

    private static DndDtos.SheetValuesDto roublarde(int dex) {
        Map<Ability, Integer> abilities = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) abilities.put(ability, 10);
        abilities.put(Ability.DEXTERITY, dex);
        abilities.put(Ability.WISDOM, 12);
        return new DndDtos.SheetValuesDto("Roublard", 1, "Halfelin", "Criminel", abilities, 2, Set.of(Ability.DEXTERITY),
                Map.of(Skill.STEALTH, Proficiency.EXPERTISE, Skill.PERCEPTION, Proficiency.PROFICIENT, Skill.ARCANA, Proficiency.NONE),
                14, 9, 9, "1d8", 7.5, 2, List.of(new DndDtos.AttackDto("Rapière", 4, "1d8+2", "perforant", "1,5 m")),
                "Attaque sournoise", "Armure de cuir", "Armes courantes", "Commun", List.of());
    }

    @Test
    void laSection_naitUneSeuleFois_etSeRetrouveRenommeeOuPerdue() {
        String joueurs = dnd().ensureSections(STRAHD).playersPageId();
        assertEquals(joueurs, dnd().ensureSections(STRAHD).playersPageId());
        assertEquals(List.of(new NotebookPages.SectionPage(joueurs, "Personnages joueurs")), pages.sectionRoots(STRAHD, "dnd"));

        pages.renommer(joueurs, "Héros");
        assertEquals(joueurs, dnd().ensureSections(STRAHD).playersPageId());

        // Données perdues : la racine titrée Personnages joueurs est reprise, sinon une page neuve.
        donnees.fichiers.clear();
        pages.renommer(joueurs, "Personnages joueurs");
        assertEquals(joueurs, dnd().ensureSections(STRAHD).playersPageId());
        donnees.fichiers.clear();
        pages.renommer(joueurs, "Héros");
        String neuve = dnd().ensureSections(STRAHD).playersPageId();
        assertTrue(!neuve.equals(joueurs));
    }

    @Test
    void unPersonnage_seCreeSousPersonnagesJoueurs_avecUneFicheVide() {
        DndDtos.SheetDto aria = dnd().create(STRAHD, new DndDtos.NewSheetRequest("  Aria  "));

        assertEquals(Optional.of("Aria"), pages.pageTitle(STRAHD, aria.pageId()));
        assertTrue(pages.isEntry(STRAHD, "dnd", aria.pageId()));
        assertEquals(1, pages.sectionRoots(STRAHD, "dnd").size(), "la section est assurée à la création");
        assertEquals(1, aria.values().level());
        assertEquals(10, aria.values().abilities().get(Ability.STRENGTH));
        assertEquals(10, aria.derived().passivePerception());
        assertEquals(aria, dnd().read(STRAHD, aria.pageId()));
        assertTrue(donnees.fichiers.get(STRAHD + "/dnd").contains("\"type\":\"created\""));
    }

    @Test
    void uneSaisie_seGardeAvecSesDerives_etEntreDansLHistoire() {
        String aria = dnd().create(STRAHD, new DndDtos.NewSheetRequest("Aria")).pageId();

        DndDtos.SheetDto saisie = dnd().edit(STRAHD, aria, roublarde(14));
        assertEquals(6, saisie.derived().skills().get(Skill.STEALTH));
        assertEquals(3, saisie.derived().skills().get(Skill.PERCEPTION));
        assertEquals(4, saisie.derived().savingThrows().get(Ability.DEXTERITY));
        assertEquals(13, saisie.derived().passivePerception());
        assertEquals(Map.of(Skill.STEALTH, Proficiency.EXPERTISE, Skill.PERCEPTION, Proficiency.PROFICIENT),
                saisie.values().skills(), "une compétence non maîtrisée n'est pas gardée");

        // Relu par une ressource neuve : seule l'histoire gardée compte.
        assertEquals(saisie, dnd().read(STRAHD, aria));
        dnd().edit(STRAHD, aria, roublarde(16));
        assertEquals(16, dnd().read(STRAHD, aria).values().abilities().get(Ability.DEXTERITY));
        String fichier = donnees.fichiers.get(STRAHD + "/dnd");
        assertEquals(2, fichier.split("\"type\":\"edited\"", -1).length - 1);
    }

    @Test
    void uneValeurHorsBornes_estRefuseeAvecSaRaison_sansRienChanger() {
        String aria = dnd().create(STRAHD, new DndDtos.NewSheetRequest("Aria")).pageId();
        String avant = donnees.fichiers.get(STRAHD + "/dnd");

        var refus = assertThrows(IllegalArgumentException.class, () -> dnd().edit(STRAHD, aria, roublarde(31)));
        assertEquals("Dextérité entre 1 et 30 : 31", refus.getMessage());
        assertEquals(avant, donnees.fichiers.get(STRAHD + "/dnd"));
        assertThrows(IllegalArgumentException.class, () -> dnd().edit(STRAHD, aria, null));
        assertThrows(IllegalArgumentException.class, () -> dnd().create(STRAHD, new DndDtos.NewSheetRequest("  ")));
        assertThrows(IllegalArgumentException.class, () -> dnd().create(STRAHD, new DndDtos.NewSheetRequest("x".repeat(121))));
    }

    @Test
    void unePageQuiNEstPasUneFiche_ouUneCampagneInconnue_rend404() {
        String aria = dnd().create(STRAHD, new DndDtos.NewSheetRequest("Aria")).pageId();
        String session = pages.pageOrdinaire(STRAHD, "Session 1");
        String joueurs = dnd().ensureSections(STRAHD).playersPageId();

        assertThrows(NotFoundException.class, () -> dnd().read(STRAHD, session));
        assertThrows(NotFoundException.class, () -> dnd().read(STRAHD, joueurs));
        assertThrows(NotFoundException.class, () -> dnd().read(STRAHD, "inconnue"));
        assertThrows(NotFoundException.class, () -> dnd().read(PHANDELVER, aria), "une fiche est propre à sa campagne");
        assertThrows(NotFoundException.class, () -> dnd().edit(STRAHD, session, roublarde(14)));
        assertThrows(NotFoundException.class, () -> dnd().read("inconnue", aria));
        assertThrows(NotFoundException.class, () -> dnd().ensureSections("inconnue"));

        pages.supprimer(aria);
        assertThrows(NotFoundException.class, () -> dnd().read(STRAHD, aria), "une fiche à la corbeille ne se lit plus");
    }

    @Test
    void unDocumentAbime_estRefuse_sansEtreEcrase() {
        String aria = dnd().create(STRAHD, new DndDtos.NewSheetRequest("Aria")).pageId();
        donnees.fichiers.put(STRAHD + "/dnd", "{pas du json");

        var refus = assertThrows(ClientErrorException.class, () -> dnd().read(STRAHD, aria));
        assertEquals(409, refus.getResponse().getStatus());
        assertThrows(ClientErrorException.class, () -> dnd().edit(STRAHD, aria, roublarde(14)));
        assertThrows(ClientErrorException.class, () -> dnd().ensureSections(STRAHD));
        assertThrows(ClientErrorException.class, () -> dnd().create(STRAHD, new DndDtos.NewSheetRequest("Borin")));
        assertEquals("{pas du json", donnees.fichiers.get(STRAHD + "/dnd"));
    }

    @Test
    void uneHistoireIllisible_estRefuseeEn409_sansEtreEcrasee() {
        String aria = dnd().create(STRAHD, new DndDtos.NewSheetRequest("Aria")).pageId();
        String valide = donnees.fichiers.get(STRAHD + "/dnd");
        String created = "{\"type\":\"created\"}";
        for (String abimee : List.of(
                created + ",{\"type\":\"edited\",\"values\":{\"level\":0}}",
                created + ",{\"type\":\"edited\"}",
                created + ",{\"type\":\"inconnu\"}",
                created + "," + created)) {
            String fichier = valide.replace(created, abimee);
            donnees.fichiers.put(STRAHD + "/dnd", fichier);

            var refus = assertThrows(ClientErrorException.class, () -> dnd().read(STRAHD, aria), abimee);
            assertEquals(409, refus.getResponse().getStatus(), abimee);
            assertThrows(ClientErrorException.class, () -> dnd().edit(STRAHD, aria, roublarde(14)), abimee);
            assertEquals(fichier, donnees.fichiers.get(STRAHD + "/dnd"), abimee);
        }
    }

    @Test
    void uneFicheDontLaCreationNAPasEteEcrite_seLitVide_etSeSaisit() {
        String joueurs = dnd().ensureSections(STRAHD).playersPageId();
        String aria = pages.createEntry(STRAHD, "dnd", joueurs, "Aria").id();

        assertEquals(1, dnd().read(STRAHD, aria).values().level());
        dnd().edit(STRAHD, aria, roublarde(14));
        assertEquals(14, dnd().read(STRAHD, aria).values().abilities().get(Ability.DEXTERITY));
        assertTrue(donnees.fichiers.get(STRAHD + "/dnd").contains("[{\"type\":\"created\"},{\"type\":\"edited\""));
    }
}
