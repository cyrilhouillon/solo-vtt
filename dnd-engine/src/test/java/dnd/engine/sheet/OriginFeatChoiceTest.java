package dnd.engine.sheet;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OriginFeatChoiceTest {

    private static String refus(OriginFeatChoice choix, Set<Skill> prises) {
        return assertThrows(IllegalArgumentException.class, () -> choix.validate(prises)).getMessage();
    }

    @Test
    void chaqueDonSeul_estValide_sesSousChoixRestantAChoisir() {
        for (OriginFeat don : OriginFeat.values()) assertDoesNotThrow(() -> OriginFeatChoice.of(don).validate(Set.of()));
        assertEquals("Doué : maîtrise de 3 à choisir", OriginFeatChoice.of(OriginFeat.SKILLED).text());
        assertEquals("Initié à la magie (liste à choisir) — incantation à choisir ; 2 sorts mineurs et 1 sort du 1er niveau (à choisir)",
                OriginFeatChoice.of(OriginFeat.MAGIC_INITIATE).text());
    }

    @Test
    void doue_ecritSesTroisCompetences_ouCeQuiReste() {
        assertEquals("Doué : maîtrise de Histoire, Médecine, Religion",
                new OriginFeatChoice(OriginFeat.SKILLED, null, null, Set.of(Skill.RELIGION, Skill.HISTORY, Skill.MEDICINE)).text());
        assertEquals("Doué : maîtrise de Histoire, 2 à choisir",
                new OriginFeatChoice(OriginFeat.SKILLED, null, null, Set.of(Skill.HISTORY)).text());
    }

    @Test
    void initieALaMagie_ecritSaListeEtSonIncantation() {
        assertEquals("Initié à la magie (Druide) — incantation Intelligence ; 2 sorts mineurs et 1 sort du 1er niveau (à choisir)",
                new OriginFeatChoice(OriginFeat.MAGIC_INITIATE, SpellList.DRUID, Ability.INTELLIGENCE, Set.of()).text());
    }

    @Test
    void unSousChoixImpossible_estRefuseAvecLaRaison() {
        assertEquals("Doué : 3 compétences au plus", refus(new OriginFeatChoice(OriginFeat.SKILLED, null, null,
                Set.of(Skill.HISTORY, Skill.MEDICINE, Skill.RELIGION, Skill.NATURE)), Set.of()));
        assertEquals("Doué : Religion déjà maîtrisée",
                refus(new OriginFeatChoice(OriginFeat.SKILLED, null, null, Set.of(Skill.RELIGION)), Set.of(Skill.RELIGION)));
        assertEquals("Vigilant : aucune liste de sorts à choisir",
                refus(new OriginFeatChoice(OriginFeat.ALERT, SpellList.CLERIC, null, Set.of()), Set.of()));
        assertEquals("Vigilant : aucune caractéristique d’incantation à choisir",
                refus(new OriginFeatChoice(OriginFeat.ALERT, null, Ability.WISDOM, Set.of()), Set.of()));
        assertEquals("Sauvagerie martiale : aucune compétence à choisir",
                refus(new OriginFeatChoice(OriginFeat.SAVAGE_ATTACKER, null, null, Set.of(Skill.HISTORY)), Set.of()));
        assertEquals("Caractéristique d’incantation : Intelligence, Sagesse ou Charisme",
                refus(new OriginFeatChoice(OriginFeat.MAGIC_INITIATE, SpellList.CLERIC, Ability.STRENGTH, Set.of()), Set.of()));
        assertEquals("Choisir un don d’origines", refus(new OriginFeatChoice(null, null, null, Set.of()), Set.of()));
    }
}
