package dnd.engine.sheet;

import dnd.engine.state.DamageType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeciesChoiceTest {

    private static final Set<Skill> ATHLETISME_PERCEPTION = Set.of(Skill.ATHLETICS, Skill.PERCEPTION);

    private static String refus(SpeciesChoice choix) {
        return assertThrows(IllegalArgumentException.class, () -> choix.validate(ATHLETISME_PERCEPTION, Set.of())).getMessage();
    }

    @Test
    void lesNeufEspeces_ontLeursOptionsDuSrd() {
        assertEquals(9, Species.values().length);
        assertEquals(10, Species.DRAGONBORN.options().size());
        assertEquals(3, Species.ELF.options().size());
        assertEquals(2, Species.GNOME.options().size());
        assertEquals(6, Species.GOLIATH.options().size());
        assertEquals(3, Species.TIEFLING.options().size());
        assertTrue(Species.DWARF.options().isEmpty());
        assertEquals(24, SpeciesOption.values().length);
        assertTrue(Arrays.stream(Species.values()).allMatch(s -> s.optionLabel() == null || !s.options().isEmpty()));
    }

    @Test
    void lesDragonsDonnentLeurTypeDeDegats() {
        assertEquals(DamageType.FIRE, SpeciesOption.BRASS_DRAGON.damageType());
        assertEquals(DamageType.COLD, SpeciesOption.WHITE_DRAGON.damageType());
        assertEquals(DamageType.LIGHTNING, SpeciesOption.BRONZE_DRAGON.damageType());
        assertEquals(DamageType.ACID, SpeciesOption.BLACK_DRAGON.damageType());
        assertEquals(DamageType.POISON, SpeciesOption.GREEN_DRAGON.damageType());
        assertEquals("Résistance aux dégâts de feu ; Souffle : dégâts de feu", SpeciesOption.RED_DRAGON.text());
        assertNull(SpeciesOption.DROW.damageType());
    }

    @Test
    void uneEspeceDUneSeuleTaille_laPrendDOffice() {
        assertEquals(CreatureSize.MEDIUM, SpeciesChoice.of(Species.DWARF).size());
        assertEquals(CreatureSize.SMALL, SpeciesChoice.of(Species.HALFLING).size());
        assertNull(SpeciesChoice.of(Species.HUMAN).size());
        assertEquals("M ou P (à choisir)", SpeciesChoice.of(Species.HUMAN).sizeText());
        assertEquals("P (petite)", new SpeciesChoice(Species.TIEFLING, CreatureSize.SMALL, null, null, null, null).sizeText());
    }

    @Test
    void uneEspeceSeule_estValide() {
        for (Species espece : Species.values()) SpeciesChoice.of(espece).validate(ATHLETISME_PERCEPTION, Set.of());
    }

    @Test
    void laVitesse_estCelleDeLEspece_ouDeSonOption() {
        assertEquals(9, SpeciesChoice.of(Species.DWARF).speed());
        assertEquals(10.5, SpeciesChoice.of(Species.GOLIATH).speed());
        assertEquals(10.5, new SpeciesChoice(Species.ELF, null, SpeciesOption.WOOD_ELF, null, null, null).speed());
        assertEquals(9, new SpeciesChoice(Species.ELF, null, SpeciesOption.HIGH_ELF, null, null, null).speed());
    }

    @Test
    void lesTraits_disentChaqueSousChoix_ouASonDefautAChoisir() {
        String nain = SpeciesChoice.of(Species.DWARF).traitsText();
        assertTrue(nain.startsWith("Vision dans le noir : 36 m\nConnaissance de la pierre"), nain);
        assertTrue(nain.contains("Ténacité naine"), nain);

        String goliath = SpeciesChoice.of(Species.GOLIATH).traitsText();
        assertTrue(goliath.contains("Ascendance gigante : à choisir"), goliath);
        assertTrue(!goliath.contains("Vision dans le noir"), goliath);

        String humain = SpeciesChoice.of(Species.HUMAN).traitsText();
        assertTrue(humain.contains("Compétent : une compétence au choix (à choisir)"), humain);
        assertTrue(humain.contains("Polyvalent : un don d’origines au choix (à choisir)"), humain);

        String drow = new SpeciesChoice(Species.ELF, null, SpeciesOption.DROW, Skill.INSIGHT, Ability.WISDOM, null).traitsText();
        assertTrue(drow.startsWith("Vision dans le noir : 36 m"), drow);
        assertTrue(drow.contains("Lignage elfique : Drow — "), drow);
        assertTrue(drow.contains("Caractéristique d’incantation : Sagesse"), drow);
        assertTrue(drow.contains("Sens aiguisés : Intuition (maîtrise)"), drow);
    }

    @Test
    void unSousChoixImpossible_estRefuseAvecLaRaison() {
        assertEquals("Nain : Drow n’est pas une option de l’espèce",
                refus(new SpeciesChoice(Species.DWARF, null, SpeciesOption.DROW, null, null, null)));
        assertEquals("Nain : taille P (petite) impossible",
                refus(new SpeciesChoice(Species.DWARF, CreatureSize.SMALL, null, null, null, null)));
        assertEquals("Acrobaties hors des compétences de Sens aiguisés",
                refus(new SpeciesChoice(Species.ELF, null, null, Skill.ACROBATICS, null, null)));
        assertEquals("Caractéristique d’incantation : Intelligence, Sagesse ou Charisme",
                refus(new SpeciesChoice(Species.ELF, null, null, null, Ability.STRENGTH, null)));
        assertEquals("Halfelin : aucune compétence à choisir",
                refus(new SpeciesChoice(Species.HALFLING, null, null, Skill.STEALTH, null, null)));
        assertEquals("Orc : aucune caractéristique d’incantation à choisir",
                refus(new SpeciesChoice(Species.ORC, null, null, null, Ability.WISDOM, null)));
        assertEquals("Choisir une espèce", refus(new SpeciesChoice(null, null, null, null, null, null)));
    }

    @Test
    void uneCompetenceDejaMaitriseeParLaClasse_estRefusee() {
        assertEquals("Perception déjà maîtrisée par la classe",
                refus(new SpeciesChoice(Species.ELF, null, null, Skill.PERCEPTION, null, null)));
    }
}
