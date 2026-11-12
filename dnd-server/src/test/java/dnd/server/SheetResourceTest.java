package dnd.server;

import dnd.engine.sheet.Ability;
import dnd.engine.sheet.AbilityMethod;
import dnd.engine.sheet.Build;
import dnd.engine.sheet.CharacterClass;
import dnd.engine.sheet.FightingStyle;
import dnd.engine.sheet.Proficiency;
import dnd.engine.sheet.Skill;
import dnd.engine.sheet.SrdWeapon;
import dnd.engine.sheet.StartingEquipment;
import dnd.engine.state.DamageType;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    /** Les dés tirés par les ressources ; aucun par défaut : un test qui en tire les scripte. */
    private Dice des = Dice.scripted();
    private int desTires;

    private SheetResource dnd() {
        return new SheetResource(donnees, pages, sides -> {
            desTires++;
            return des.roll(sides);
        });
    }

    /** Six jets de 4d6, aux totaux 16, 15, 14, 12, 10 et 9. */
    private static Dice sixJets() {
        return Dice.scripted(6, 6, 4, 1, 5, 5, 5, 2, 6, 4, 4, 3, 4, 4, 4, 4, 3, 3, 4, 2, 3, 3, 3, 1);
    }

    /** Le Guerrier suggéré, avec ces compétences, cet équipement et ce style. */
    private static DndDtos.BuildDto guerrier(Set<Skill> skills, StartingEquipment equipment, FightingStyle style) {
        Build base = Build.fighter();
        return DndDtos.BuildDto.of(new Build(base.characterClass(), base.method(), base.scores(), skills, style,
                equipment, equipment.weapons(), null));
    }

    private static DndDtos.SheetValuesDto avecCA(DndDtos.SheetValuesDto v, int armorClass) {
        return new DndDtos.SheetValuesDto(v.className(), v.level(), v.species(), v.background(), v.abilities(),
                v.proficiencyBonus(), v.savingThrows(), v.skills(), armorClass, v.hitPointMax(), v.hitPoints(), v.hitDice(),
                v.speed(), v.initiative(), v.attacks(), v.features(), v.equipment(), v.proficiencies(), v.languages(),
                v.spellSlots());
    }

    private static DndDtos.SheetValuesDto roublarde(int dex) {
        Map<Ability, Integer> abilities = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) abilities.put(ability, 10);
        abilities.put(Ability.DEXTERITY, dex);
        abilities.put(Ability.WISDOM, 12);
        return new DndDtos.SheetValuesDto("Roublard", 1, "Halfelin", "Criminel", abilities, 2, Set.of(Ability.DEXTERITY),
                Map.of(Skill.STEALTH, Proficiency.EXPERTISE, Skill.PERCEPTION, Proficiency.PROFICIENT, Skill.ARCANA, Proficiency.NONE),
                14, 9, 9, "1d8", 7.5, 2, List.of(new DndDtos.AttackDto("Rapière", 4, 1, 8, 2, DamageType.PIERCING, 1.5, null, null)),
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

    @Test
    void lesOptionsDuGuerrier_disentSesChoix() {
        List<DndDtos.ClassOptionsDto> classes = dnd().classes(STRAHD);

        assertEquals(1, classes.size());
        DndDtos.ClassOptionsDto guerrier = classes.getFirst();
        assertEquals("FIGHTER", guerrier.id());
        assertEquals("Guerrier", guerrier.label());
        assertEquals(2, guerrier.skillCount());
        assertEquals(9, guerrier.skillChoices().size());
        assertTrue(guerrier.skillChoices().contains(new DndDtos.ChoiceDto("ATHLETICS", Skill.ATHLETICS.label())));
        assertEquals(4, guerrier.fightingStyles().size());
        assertEquals(DndDtos.BuildDto.of(Build.fighter()), guerrier.defaultBuild());
        assertTrue(guerrier.fightingStyles().contains(
                new DndDtos.DescribedChoiceDto("DEFENSE", "Défense", FightingStyle.DEFENSE.text())));
        assertEquals(List.of(
                        new DndDtos.DescribedChoiceDto("FIGHTER_A", "Équipement A", StartingEquipment.FIGHTER_A.text()),
                        new DndDtos.DescribedChoiceDto("FIGHTER_B", "Équipement B", StartingEquipment.FIGHTER_B.text())),
                guerrier.equipments());
        assertTrue(guerrier.equipments().getFirst().text().endsWith(", 4 po"));
        assertEquals(38, guerrier.weapons().size());
        assertTrue(guerrier.weapons().contains(new DndDtos.WeaponChoiceDto("GREATSWORD", "Épée à deux mains", "Écorchure")));
        assertEquals(3, guerrier.masteryCount());
        assertEquals(15, guerrier.suggestedScores().get(Ability.STRENGTH));
        assertEquals(8, guerrier.suggestedScores().get(Ability.INTELLIGENCE));
        assertEquals(List.of(15, 14, 13, 12, 10, 8), guerrier.standardScores());
        assertEquals(List.of("8", "9", "10", "11", "12", "13", "14", "15"), List.copyOf(guerrier.pointBuyCosts().keySet()));
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 7, 9), List.copyOf(guerrier.pointBuyCosts().values()));
        assertEquals(27, guerrier.pointBuyBudget());
        assertThrows(NotFoundException.class, () -> dnd().classes("inconnue"));
    }

    @Test
    void uneConstruction_remplitLaFiche_etSeRelitALIdentique() {
        DndDtos.SheetDto creee = dnd().create(STRAHD, new DndDtos.NewSheetRequest("Aria"));
        assertNull(creee.build());
        assertNull(creee.rolls());

        DndDtos.BuildDto choix = DndDtos.BuildDto.of(Build.fighter());
        DndDtos.SheetDto construite = dnd().build(STRAHD, creee.pageId(), choix);

        assertEquals(choix, construite.build());
        assertNull(construite.rolls());
        assertEquals("Guerrier", construite.values().className());
        assertEquals(15, construite.values().abilities().get(Ability.STRENGTH));
        assertEquals(17, construite.values().armorClass(), "cotte de mailles 16, Défense +1");
        assertEquals(11, construite.values().hitPointMax(), "d10 + Con +1");
        assertEquals(3, construite.values().attacks().size());
        assertEquals(StartingEquipment.FIGHTER_A.text(), construite.values().equipment());
        // Relue par une ressource neuve : seule l'histoire gardée compte.
        assertEquals(construite, dnd().read(STRAHD, creee.pageId()));
        assertTrue(donnees.fichiers.get(STRAHD + "/dnd").contains(
                "{\"type\":\"built\",\"values\":{\"className\":\"Guerrier\""));
        assertTrue(donnees.fichiers.get(STRAHD + "/dnd").contains(
                "\"build\":{\"characterClass\":\"FIGHTER\",\"method\":\"STANDARD_ARRAY\""));
    }

    @Test
    void uneRetouche_survitAUnChoixDontElleNeDependPas_pasAUnChoixDontElleDepend() {
        String aria = dnd().create(STRAHD, new DndDtos.NewSheetRequest("Aria")).pageId();
        DndDtos.SheetDto construite = dnd().build(STRAHD, aria,
                guerrier(Set.of(), StartingEquipment.FIGHTER_A, FightingStyle.DEFENSE));
        dnd().edit(STRAHD, aria, avecCA(construite.values(), 20));

        DndDtos.SheetDto competences = dnd().build(STRAHD, aria,
                guerrier(Set.of(Skill.ATHLETICS, Skill.PERCEPTION), StartingEquipment.FIGHTER_A, FightingStyle.DEFENSE));
        assertEquals(20, competences.values().armorClass(), "la CA ne dépend pas des compétences");
        assertEquals(Map.of(Skill.ATHLETICS, Proficiency.PROFICIENT, Skill.PERCEPTION, Proficiency.PROFICIENT),
                competences.values().skills());
        assertEquals(List.of(Skill.ATHLETICS, Skill.PERCEPTION), competences.build().skills());

        DndDtos.SheetDto equipementB = dnd().build(STRAHD, aria,
                guerrier(Set.of(Skill.ATHLETICS, Skill.PERCEPTION), StartingEquipment.FIGHTER_B, FightingStyle.DEFENSE));
        assertEquals(15, equipementB.values().armorClass(), "cuir clouté 12 + Dex 2 + Défense 1, retouche comprise");
    }

    @Test
    void lesDes_seLancentUneFois_puisDonnentLesValeurs() {
        String aria = dnd().create(STRAHD, new DndDtos.NewSheetRequest("Aria")).pageId();
        des = sixJets();

        DndDtos.SheetDto lancee = dnd().rollAbilities(STRAHD, aria);
        assertEquals(24, desTires);
        assertEquals(new DndDtos.AbilityRollDto(List.of(6, 6, 4, 1), 16), lancee.rolls().getFirst());
        assertEquals(List.of(16, 15, 14, 12, 10, 9), lancee.rolls().stream().map(DndDtos.AbilityRollDto::total).toList());
        assertEquals(lancee.rolls(), dnd().read(STRAHD, aria).rolls());
        assertTrue(donnees.fichiers.get(STRAHD + "/dnd").contains("{\"type\":\"rolled\",\"rolls\":[[6,6,4,1],[5,5,5,2]"));

        var refus = assertThrows(ClientErrorException.class, () -> dnd().rollAbilities(STRAHD, aria));
        assertEquals(409, refus.getResponse().getStatus());
        assertEquals(24, desTires, "aucun dé tiré au second lancer");

        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        scores.put(Ability.STRENGTH, 16);
        scores.put(Ability.DEXTERITY, 15);
        scores.put(Ability.CONSTITUTION, 14);
        scores.put(Ability.INTELLIGENCE, 9);
        scores.put(Ability.WISDOM, 10);
        scores.put(Ability.CHARISMA, 12);
        Build base = Build.fighter();
        DndDtos.SheetDto construite = dnd().build(STRAHD, aria, DndDtos.BuildDto.of(new Build(CharacterClass.FIGHTER,
                AbilityMethod.ROLLED, scores, Set.of(), base.fightingStyle(), base.equipment(), base.masteries(), null)));
        assertEquals(scores, construite.values().abilities());
        assertEquals(AbilityMethod.ROLLED, construite.build().method());
        assertEquals(lancee.rolls(), construite.rolls(), "la construction garde les jets");
        assertEquals(construite, dnd().read(STRAHD, aria));
    }

    @Test
    void uneConstructionImpossible_estRefuseeAvecSaRaison_sansRienEcrire() {
        String aria = dnd().create(STRAHD, new DndDtos.NewSheetRequest("Aria")).pageId();
        String avant = donnees.fichiers.get(STRAHD + "/dnd");
        DndDtos.BuildDto c = DndDtos.BuildDto.of(Build.fighter());

        var refus = assertThrows(IllegalArgumentException.class, () -> dnd().build(STRAHD, aria,
                guerrier(Set.of(Skill.ATHLETICS, Skill.PERCEPTION, Skill.HISTORY), StartingEquipment.FIGHTER_A, FightingStyle.DEFENSE)));
        assertEquals("Guerrier : 2 compétences au plus", refus.getMessage());
        var sansDes = assertThrows(IllegalArgumentException.class, () -> dnd().build(STRAHD, aria, new DndDtos.BuildDto(
                c.characterClass(), AbilityMethod.ROLLED, c.scores(), c.skills(), c.fightingStyle(), c.equipment(), c.masteries())));
        assertEquals("Dés : lancer d'abord les six jets de caractéristique", sansDes.getMessage());
        assertThrows(IllegalArgumentException.class, () -> dnd().build(STRAHD, aria, new DndDtos.BuildDto(
                c.characterClass(), c.method(), c.scores(), c.skills(), c.fightingStyle(), c.equipment(), null)));
        assertThrows(IllegalArgumentException.class, () -> dnd().build(STRAHD, aria, new DndDtos.BuildDto(
                c.characterClass(), c.method(), null, c.skills(), c.fightingStyle(), c.equipment(), c.masteries())));
        assertThrows(IllegalArgumentException.class, () -> dnd().build(STRAHD, aria, new DndDtos.BuildDto(
                c.characterClass(), c.method(), c.scores(), null, c.fightingStyle(), c.equipment(), c.masteries())));
        assertThrows(IllegalArgumentException.class, () -> dnd().build(STRAHD, aria, new DndDtos.BuildDto(
                null, c.method(), c.scores(), c.skills(), c.fightingStyle(), c.equipment(), c.masteries())));
        assertThrows(IllegalArgumentException.class, () -> dnd().build(STRAHD, aria, new DndDtos.BuildDto(
                c.characterClass(), c.method(), c.scores(), c.skills(), c.fightingStyle(), c.equipment(),
                List.of(SrdWeapon.GREATSWORD, SrdWeapon.GREATSWORD, SrdWeapon.FLAIL))));
        assertThrows(IllegalArgumentException.class, () -> dnd().build(STRAHD, aria, null));
        assertEquals(avant, donnees.fichiers.get(STRAHD + "/dnd"));
        assertNull(dnd().read(STRAHD, aria).build());
    }

    @Test
    void construireOuLancerSurUnePageQuiNEstPasUneFiche_rend404() {
        String session = pages.pageOrdinaire(STRAHD, "Session 1");
        DndDtos.BuildDto choix = DndDtos.BuildDto.of(Build.fighter());

        assertThrows(NotFoundException.class, () -> dnd().build(STRAHD, session, choix));
        assertThrows(NotFoundException.class, () -> dnd().rollAbilities(STRAHD, session));
        assertThrows(NotFoundException.class, () -> dnd().build("inconnue", session, choix));
        assertThrows(NotFoundException.class, () -> dnd().rollAbilities("inconnue", session));
        assertEquals(0, desTires);
    }

    @Test
    void uneSaisieApresUneConstruction_gardeLaConstructionEtLesJets() {
        String aria = dnd().create(STRAHD, new DndDtos.NewSheetRequest("Aria")).pageId();
        des = sixJets();
        List<DndDtos.AbilityRollDto> jets = dnd().rollAbilities(STRAHD, aria).rolls();
        DndDtos.SheetDto construite = dnd().build(STRAHD, aria, DndDtos.BuildDto.of(Build.fighter()));

        DndDtos.SheetDto saisie = dnd().edit(STRAHD, aria, avecCA(construite.values(), 20));
        assertEquals(20, saisie.values().armorClass());
        assertEquals(construite.build(), saisie.build());
        assertEquals(jets, saisie.rolls());
        assertEquals(saisie, dnd().read(STRAHD, aria));
    }

    @Test
    void unDocumentDAvantLaConstruction_seRelitSansConstruction() {
        String joueurs = dnd().ensureSections(STRAHD).playersPageId();
        String aria = pages.createEntry(STRAHD, "dnd", joueurs, "Aria").id();
        donnees.fichiers.put(STRAHD + "/dnd", """
                {"playersPageId":"%s","sheets":{"%s":[{"type":"created"},{"type":"edited","values":{
                "className":"Roublard","level":1,"species":"Halfelin","background":"Criminel",
                "abilities":{"STRENGTH":8,"DEXTERITY":14,"CONSTITUTION":12,"INTELLIGENCE":13,"WISDOM":12,"CHARISMA":10},
                "proficiencyBonus":2,"savingThrows":["DEXTERITY"],"skills":{"STEALTH":"EXPERTISE"},
                "armorClass":14,"hitPointMax":9,"hitPoints":9,"hitDice":"1d8","speed":7.5,"initiative":2,
                "attacks":[{"name":"Rapière","bonus":4,"diceCount":1,"dieSides":8,"damageBonus":2,"damageType":"PIERCING","reach":1.5}],
                "features":"Attaque sournoise","equipment":"Armure de cuir","proficiencies":"Armes courantes",
                "languages":"Commun","spellSlots":[]}}]}}
                """.formatted(joueurs, aria));

        DndDtos.SheetDto relue = dnd().read(STRAHD, aria);
        assertEquals("Roublard", relue.values().className());
        assertEquals(14, relue.values().armorClass());
        assertEquals(6, relue.derived().skills().get(Skill.STEALTH));
        assertNull(relue.build());
        assertNull(relue.rolls());
        assertNotNull(dnd().build(STRAHD, aria, DndDtos.BuildDto.of(Build.fighter())).build(), "une fiche d'avant se construit");
    }

    @Test
    void uneConstructionOuDesJetsIllisibles_sontRefusesEn409_sansEtreEcrases() {
        String aria = dnd().create(STRAHD, new DndDtos.NewSheetRequest("Aria")).pageId();
        String valide = donnees.fichiers.get(STRAHD + "/dnd");
        String created = "{\"type\":\"created\"}";
        String quatre = "[1,2,3,4]";
        String cinqJets = String.join(",", List.of(quatre, quatre, quatre, quatre, quatre));
        for (String abimee : List.of(
                created + ",{\"type\":\"built\"}",
                created + ",{\"type\":\"rolled\"}",
                created + ",{\"type\":\"rolled\",\"rolls\":[" + cinqJets + "]}",
                created + ",{\"type\":\"rolled\",\"rolls\":[[1,2,3,7]," + cinqJets + "]}",
                created + ",{\"type\":\"rolled\",\"rolls\":[" + cinqJets + "," + quatre + "]},{\"type\":\"rolled\",\"rolls\":["
                        + cinqJets + "," + quatre + "]}")) {
            String fichier = valide.replace(created, abimee);
            donnees.fichiers.put(STRAHD + "/dnd", fichier);

            var refus = assertThrows(ClientErrorException.class, () -> dnd().read(STRAHD, aria), abimee);
            assertEquals(409, refus.getResponse().getStatus(), abimee);
            assertThrows(ClientErrorException.class, () -> dnd().rollAbilities(STRAHD, aria), abimee);
            assertEquals(fichier, donnees.fichiers.get(STRAHD + "/dnd"), abimee);
        }
        assertEquals(0, desTires);
    }

    @Test
    void uneConstructionTronquee_estRefuseeEn409_sansEtreEcrasee() {
        String aria = dnd().create(STRAHD, new DndDtos.NewSheetRequest("Aria")).pageId();
        dnd().build(STRAHD, aria, DndDtos.BuildDto.of(Build.fighter()));
        String valide = donnees.fichiers.get(STRAHD + "/dnd");
        String scores = "\"scores\":{\"STRENGTH\":15,";
        assertTrue(valide.contains(scores), valide);
        String tronquee = valide.replace(scores, "\"scores\":{");
        donnees.fichiers.put(STRAHD + "/dnd", tronquee);

        var refus = assertThrows(ClientErrorException.class,
                () -> dnd().build(STRAHD, aria, DndDtos.BuildDto.of(Build.fighter())));
        assertEquals(409, refus.getResponse().getStatus());
        assertEquals(tronquee, donnees.fichiers.get(STRAHD + "/dnd"));
    }

    @Test
    void uneDexteriteRetouchee_compteDansLaCAAuChoixSuivant() {
        String aria = dnd().create(STRAHD, new DndDtos.NewSheetRequest("Aria")).pageId();
        DndDtos.SheetDto construite = dnd().build(STRAHD, aria,
                guerrier(Set.of(), StartingEquipment.FIGHTER_A, FightingStyle.DEFENSE));
        Map<Ability, Integer> abilities = new EnumMap<>(construite.values().abilities());
        abilities.put(Ability.DEXTERITY, 16);
        DndDtos.SheetValuesDto v = construite.values();
        dnd().edit(STRAHD, aria, new DndDtos.SheetValuesDto(v.className(), v.level(), v.species(), v.background(), abilities,
                v.proficiencyBonus(), v.savingThrows(), v.skills(), v.armorClass(), v.hitPointMax(), v.hitPoints(),
                v.hitDice(), v.speed(), v.initiative(), v.attacks(), v.features(), v.equipment(), v.proficiencies(),
                v.languages(), v.spellSlots()));

        DndDtos.SheetDto equipementB = dnd().build(STRAHD, aria,
                guerrier(Set.of(), StartingEquipment.FIGHTER_B, FightingStyle.DEFENSE));

        assertEquals(16, equipementB.values().abilities().get(Ability.DEXTERITY));
        assertEquals(16, equipementB.values().armorClass(), "cuir clouté 12 + Dex 3 + Défense 1");
    }

    @Test
    void uneAttaqueSansAllongeNiPortee_estRefuseeEn400_avecSaRaison() {
        String aria = dnd().create(STRAHD, new DndDtos.NewSheetRequest("Aria")).pageId();
        var v = roublarde(14);
        var sansPortee = new DndDtos.SheetValuesDto(v.className(), v.level(), v.species(), v.background(), v.abilities(),
                v.proficiencyBonus(), v.savingThrows(), v.skills(), v.armorClass(), v.hitPointMax(), v.hitPoints(), v.hitDice(),
                v.speed(), v.initiative(), List.of(new DndDtos.AttackDto("Coup", 2, 1, 4, 0, DamageType.BLUDGEONING, null, null, null)),
                v.features(), v.equipment(), v.proficiencies(), v.languages(), v.spellSlots());

        var refus = assertThrows(IllegalArgumentException.class, () -> dnd().edit(STRAHD, aria, sansPortee));
        assertEquals("Coup : une allonge ou une portée", refus.getMessage());
    }

    @Test
    void uneFicheDAvantLesAttaquesStructurees_estIllisibleEn409() {
        String joueurs = dnd().ensureSections(STRAHD).playersPageId();
        String aria = pages.createEntry(STRAHD, "dnd", joueurs, "Aria").id();
        String fichier = """
                {"playersPageId":"%s","sheets":{"%s":[{"type":"created"},{"type":"edited","values":{
                "className":"Roublard","level":1,"species":"","background":"",
                "abilities":{"STRENGTH":8,"DEXTERITY":14,"CONSTITUTION":12,"INTELLIGENCE":13,"WISDOM":12,"CHARISMA":10},
                "proficiencyBonus":2,"savingThrows":[],"skills":{},
                "armorClass":14,"hitPointMax":9,"hitPoints":9,"hitDice":"1d8","speed":7.5,"initiative":2,
                "attacks":[{"name":"Rapière","bonus":4,"damage":"1d8+2","damageType":"perforant","range":"1,5 m"}],
                "features":"","equipment":"","proficiencies":"","languages":"","spellSlots":[]}}]}}
                """.formatted(joueurs, aria);
        donnees.fichiers.put(STRAHD + "/dnd", fichier);

        var refus = assertThrows(ClientErrorException.class, () -> dnd().read(STRAHD, aria));
        assertEquals(409, refus.getResponse().getStatus());
        assertEquals(fichier, donnees.fichiers.get(STRAHD + "/dnd"));
    }
}
