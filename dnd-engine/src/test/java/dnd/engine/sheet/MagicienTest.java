package dnd.engine.sheet;

import dnd.engine.rules.ShieldReaction;
import dnd.engine.state.Creature;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicienTest {

    private static final List<Spell> GRIMOIRE_AVEC_BOUCLIER = List.of(Spell.MAGE_ARMOR, Spell.DETECT_MAGIC,
            Spell.FEATHER_FALL, Spell.MAGIC_MISSILE, Spell.SHIELD, Spell.THUNDERWAVE);
    private static final List<Spell> PREPARES_AVEC_BOUCLIER = List.of(Spell.MAGE_ARMOR, Spell.DETECT_MAGIC,
            Spell.MAGIC_MISSILE, Spell.SHIELD);

    private static Build magicien() {
        Build base = Build.suggested(CharacterClass.WIZARD);
        return new Build(base.characterClass(), base.method(), base.scores(), Set.of(Skill.ARCANA, Skill.INVESTIGATION),
                null, base.equipment(), List.of(), null, null, Set.of(), null, base.cantrips(), base.spellbook(),
                base.prepared());
    }

    private static Build avecSorts(List<Spell> mineurs, List<Spell> grimoire, List<Spell> prepares) {
        return magicien().withSpells(mineurs, grimoire, prepares);
    }

    private static CharacterSheet construite(Build build) {
        return GuidedValues.of(build).rebuild(CharacterSheet.blank(), GuidedValues.NONE);
    }

    private static String refus(Build build) {
        return assertThrows(IllegalArgumentException.class, () -> build.validate(null)).getMessage();
    }

    /** La fiche avec ces langues et ces sorts saisis à la main. */
    private static CharacterSheet saisie(CharacterSheet f, String langues, Ability incantation, List<Spell> mineurs,
                                         List<Spell> grimoire, List<Spell> prepares) {
        return new CharacterSheet(f.characterClass(), f.fightingStyle(), f.level(), f.species(), f.size(), f.background(),
                f.abilities(), f.proficiencyBonus(), f.savingThrows(), f.skills(), f.armorBonus(), f.hitPointMax(),
                f.hitPoints(), f.hitDice(), f.speed(), f.initiative(), f.otherAttacks(), f.features(), f.speciesTraits(),
                f.feats(), f.inventory(), f.proficiencies(), langues, f.spellSlots(), incantation, mineurs, grimoire,
                prepares);
    }

    @Test
    void laListeDuMagicien_a15SortsMineursEt30SortsDuPremierNiveau_etBouclierPorteLIdDuMoteur() {
        assertEquals(15, Spell.of(SpellList.WIZARD, 0).size());
        assertEquals(30, Spell.of(SpellList.WIZARD, 1).size());
        assertEquals(ShieldReaction.SPELL, Spell.SHIELD.id());
        assertEquals(45, Arrays.stream(Spell.values()).map(Spell::id).distinct().count());
    }

    @Test
    void leMagicienSuggere_estValide_avecLesSortsRecommandes() {
        Build build = Build.suggested(CharacterClass.WIZARD);
        build.validate(null);

        assertEquals(Map.of(Ability.STRENGTH, 8, Ability.DEXTERITY, 12, Ability.CONSTITUTION, 13,
                Ability.INTELLIGENCE, 15, Ability.WISDOM, 14, Ability.CHARISMA, 10), build.scores());
        assertEquals(StartingEquipment.WIZARD_A, build.equipment());
        assertEquals(List.of(Spell.LIGHT, Spell.MAGE_HAND, Spell.RAY_OF_FROST), build.cantrips());
        assertEquals(List.of(Spell.MAGE_ARMOR, Spell.DETECT_MAGIC, Spell.FEATHER_FALL, Spell.MAGIC_MISSILE, Spell.SLEEP,
                Spell.THUNDERWAVE), build.spellbook());
        assertEquals(List.of(Spell.MAGE_ARMOR, Spell.DETECT_MAGIC, Spell.FEATHER_FALL, Spell.MAGIC_MISSILE), build.prepared());
        assertTrue(Build.fighter().cantrips().isEmpty());
    }

    @Test
    void unMagicien_remplitLaFiche() {
        Build build = magicien();
        build.validate(null);
        CharacterSheet fiche = construite(build);

        assertEquals(CharacterClass.WIZARD, fiche.characterClass());
        assertEquals(7, fiche.hitPointMax(), "d6 + Con 1");
        assertEquals("1d6", fiche.hitDice());
        assertEquals(Set.of(Ability.INTELLIGENCE, Ability.WISDOM), fiche.savingThrows());
        assertEquals(Ability.INTELLIGENCE, fiche.spellcastingAbility());
        assertEquals(12, fiche.spellSaveDc(), "8 + Int 2 + maîtrise 2");
        assertEquals(4, fiche.spellAttackBonus());
        assertEquals(List.of(new SpellSlots(1, 2, 0)), fiche.spellSlots());
        assertEquals(build.cantrips(), fiche.cantrips());
        assertEquals(build.spellbook(), fiche.spellbook());
        assertEquals(build.prepared(), fiche.preparedSpells());
        assertEquals(11, fiche.armorClass(), "10 + Dex 1, sans armure");
        assertEquals(List.of("Dague", "Bâton de combat", "Attaque à mains nues"),
                fiche.attacks().stream().map(SheetAttack::name).toList());
        assertEquals(5, fiche.inventory().gold());
        assertTrue(fiche.features().contains("Sorts (Intelligence) : sorts mineurs Lumière, Main du mage, Rayon de givre"));
        assertTrue(fiche.features().contains("Savoir rituel"));
        assertTrue(fiche.features().contains("Restauration magique"));
        assertEquals("Armes courantes ; aucune armure", fiche.proficiencies());
    }

    @Test
    void desSortsIncomplets_restentAChoisir() {
        Build build = avecSorts(List.of(Spell.LIGHT), List.of(Spell.SHIELD), List.of());
        build.validate(null);

        assertTrue(construite(build).features().contains(
                "sorts mineurs Lumière, 2 à choisir ; grimoire Bouclier, 5 à choisir ; préparés 4 à choisir"));
    }

    @Test
    void lEquipementB_n_aQueSesPiecesDOr() {
        Build base = magicien();
        Build b = new Build(base.characterClass(), base.method(), base.scores(), base.skills(), null,
                StartingEquipment.WIZARD_B, List.of(), null, null, Set.of(), null, base.cantrips(), base.spellbook(),
                base.prepared());
        CharacterSheet fiche = construite(b);

        assertEquals(55, fiche.inventory().gold());
        assertTrue(fiche.inventory().items().isEmpty());
    }

    @Test
    void desSortsImpossibles_sontRefusesAvecLaRaison() {
        List<Spell> mineurs = List.of(Spell.LIGHT, Spell.MAGE_HAND, Spell.RAY_OF_FROST);
        List<Spell> grimoire = magicien().spellbook();
        assertEquals("Sorts mineurs : 3 au plus",
                refus(avecSorts(List.of(Spell.LIGHT, Spell.MAGE_HAND, Spell.RAY_OF_FROST, Spell.FIRE_BOLT), grimoire, List.of())));
        assertEquals("Grimoire : 6 au plus",
                refus(avecSorts(mineurs, List.of(Spell.MAGE_ARMOR, Spell.DETECT_MAGIC, Spell.FEATHER_FALL,
                        Spell.MAGIC_MISSILE, Spell.SHIELD, Spell.THUNDERWAVE, Spell.SLEEP), List.of())));
        assertEquals("Sorts préparés : 4 au plus", refus(avecSorts(mineurs, grimoire, grimoire.subList(0, 5))));
        assertEquals("Bouclier : préparé hors du grimoire",
                refus(avecSorts(mineurs, grimoire, List.of(Spell.SHIELD))));
        assertEquals("Bouclier n’est pas un sort mineur", refus(avecSorts(List.of(Spell.SHIELD), grimoire, List.of())));
        assertEquals("Grimoire : Lumière n’est pas un sort du 1er niveau",
                refus(avecSorts(mineurs, List.of(Spell.LIGHT), List.of())));
        assertEquals("Sorts mineurs : un sort en double", refus(avecSorts(List.of(Spell.LIGHT, Spell.LIGHT), grimoire, List.of())));
        assertEquals("Sort inconnu", refus(avecSorts(Arrays.asList(Spell.LIGHT, null), grimoire, List.of())));
        assertEquals("Guerrier : aucun sort au niveau 1", refus(Build.fighter().withSpells(mineurs, List.of(), List.of())));
    }

    @Test
    void uneFicheLibre_refuseUnSortAuMauvaisNiveauOuEnDouble() {
        CharacterSheet vide = CharacterSheet.blank();
        assertEquals("Bouclier n’est pas un sort mineur", assertThrows(IllegalArgumentException.class,
                () -> saisie(vide, "", Ability.INTELLIGENCE, List.of(Spell.SHIELD), List.of(), List.of())).getMessage());
        assertEquals("Sorts préparés : Lumière est un sort mineur", assertThrows(IllegalArgumentException.class,
                () -> saisie(vide, "", Ability.INTELLIGENCE, List.of(), List.of(), List.of(Spell.LIGHT))).getMessage());
        assertEquals("Grimoire : un sort en double", assertThrows(IllegalArgumentException.class,
                () -> saisie(vide, "", null, List.of(), List.of(Spell.SHIELD, Spell.SHIELD), List.of())).getMessage());
        CharacterSheet sansIncantation = saisie(vide, "", null, List.of(), List.of(), List.of(Spell.SHIELD));
        assertNull(sansIncantation.spellSaveDc());
        assertNull(sansIncantation.spellAttackBonus());
    }

    @Test
    void unMagicienQuiPrepareBouclier_leConnaitEnCombat_avecSesEmplacementsRestants() {
        CharacterSheet fiche = construite(avecSorts(magicien().cantrips(), GRIMOIRE_AVEC_BOUCLIER, PREPARES_AVEC_BOUCLIER));
        Creature pleine = fiche.profile().create("pj", "Elminster");
        assertTrue(pleine.knowsSpell(ShieldReaction.SPELL));
        assertTrue(pleine.knowsSpell(Spell.LIGHT.id()));
        assertFalse(pleine.knowsSpell(Spell.FEATHER_FALL.id()), "au grimoire, pas préparé");
        assertEquals(2, pleine.spellSlots(1));

        Creature entamee = fiche.withSpellSlotExpended(1).profile().create("pj", "Elminster");
        assertEquals(1, entamee.spellSlots(1));
    }

    @Test
    void unMagicienSansBouclier_neLeConnaitPas() {
        assertFalse(construite(magicien()).profile().create("pj", "Elminster").knowsSpell(ShieldReaction.SPELL));
    }

    @Test
    void unEmplacementDepense_seCompteDansLHistoire_jamaisAuDelaDuMaximum() {
        Build build = magicien();
        SheetState etat = SheetHistory.replay(List.of(new SheetEvent.SheetCreated(),
                new SheetEvent.SheetBuilt(build, construite(build)),
                new SheetEvent.SpellSlotExpended(1)));
        assertEquals(List.of(new SpellSlots(1, 2, 1)), etat.values().spellSlots());
        assertSame(build, etat.build());

        SheetState epuise = SheetHistory.replay(List.of(new SheetEvent.SheetCreated(),
                new SheetEvent.SheetBuilt(build, construite(build)),
                new SheetEvent.SpellSlotExpended(1), new SheetEvent.SpellSlotExpended(1), new SheetEvent.SpellSlotExpended(1)));
        assertEquals(List.of(new SpellSlots(1, 2, 2)), epuise.values().spellSlots());

        SheetState sansNiveau3 = SheetHistory.replay(List.of(new SheetEvent.SheetCreated(),
                new SheetEvent.SheetBuilt(build, construite(build)), new SheetEvent.SpellSlotExpended(3)));
        assertEquals(List.of(new SpellSlots(1, 2, 0)), sansNiveau3.values().spellSlots());

        assertThrows(IllegalArgumentException.class, () -> new SheetEvent.SpellSlotExpended(0));
        assertThrows(IllegalArgumentException.class, () -> new SheetEvent.SpellSlotExpended(10));
    }

    @Test
    void passerDeMagicienAGuerrier_retireLesSorts_etGardeUneRetoucheIndependante() {
        Build build = magicien();
        CharacterSheet fiche = saisie(construite(build), "Commun, Elfique", Ability.INTELLIGENCE, build.cantrips(),
                build.spellbook(), build.prepared());
        SheetState etat = new SheetState(fiche, build, build.abilities(), null);

        CharacterSheet guerrier = etat.rebuild(Build.fighter());

        assertNull(guerrier.spellcastingAbility());
        assertTrue(guerrier.cantrips().isEmpty());
        assertTrue(guerrier.spellbook().isEmpty());
        assertTrue(guerrier.preparedSpells().isEmpty());
        assertTrue(guerrier.spellSlots().isEmpty());
        assertEquals("Commun, Elfique", guerrier.languages());
    }

    @Test
    void changerUnSortPrepare_neReecritQueLesPrepares_etGardeLesEmplacementsDepenses() {
        Build build = avecSorts(magicien().cantrips(), GRIMOIRE_AVEC_BOUCLIER, List.of(Spell.MAGE_ARMOR));
        CharacterSheet depense = construite(build).withSpellSlotExpended(1);
        List<Spell> mineursRetouches = List.of(Spell.FIRE_BOLT);
        CharacterSheet retouchee = saisie(depense, "", Ability.INTELLIGENCE, mineursRetouches, depense.spellbook(),
                depense.preparedSpells());
        SheetState etat = new SheetState(retouchee, build, build.abilities(), null);

        CharacterSheet apres = etat.rebuild(build.withSpells(build.cantrips(), build.spellbook(), List.of(Spell.SHIELD)));

        assertEquals(List.of(Spell.SHIELD), apres.preparedSpells());
        assertEquals(mineursRetouches, apres.cantrips(), "sorts mineurs inchangés dans la construction : la retouche reste");
        assertEquals(List.of(new SpellSlots(1, 2, 1)), apres.spellSlots());
    }

    @Test
    void passerDeGuerrierAMagicien_ecritLesSortsEtLesEmplacements() {
        SheetState guerrier = new SheetState(construite(Build.fighter()), Build.fighter(), Build.fighter().abilities(), null);

        CharacterSheet apres = guerrier.rebuild(magicien());

        assertEquals(Ability.INTELLIGENCE, apres.spellcastingAbility());
        assertEquals(magicien().prepared(), apres.preparedSpells());
        assertEquals(List.of(new SpellSlots(1, 2, 0)), apres.spellSlots());
        assertNull(apres.fightingStyle());
    }
}
