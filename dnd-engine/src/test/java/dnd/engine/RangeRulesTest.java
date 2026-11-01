package dnd.engine;

import dnd.engine.attack.AttackAction;
import dnd.engine.events.GameEvent;
import dnd.engine.rules.DuelingStyle;
import dnd.engine.rules.RangeRules;
import dnd.engine.rules.StandardRules;
import dnd.engine.state.Condition;
import dnd.engine.state.Creature;
import dnd.engine.state.DamageType;
import dnd.engine.state.GameState;
import dnd.engine.state.Weapon;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;
import solovtt.engine.Engine;
import solovtt.engine.StepResult;
import solovtt.scene.DistanceUnit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Allonge, portée normale et longue, lancer (SRD 5.2.1, « Range », « Reach », « Thrown »). */
class RangeRulesTest {

    static final Weapon LONGSWORD = new Weapon("Épée longue", 1, 8, DamageType.SLASHING, 5, 3, true);
    static final Weapon DAGGER = new Weapon("Dague", 1, 4, DamageType.PIERCING, 4, 2, true).thrown(6, 18);
    static final Weapon SHORTBOW = Weapon.ranged("Arc court", 1, 6, DamageType.PIERCING, 4, 2, 24, 96);

    static final String MELEE_SWORD = "melee:Épée longue";
    static final String THROWN_DAGGER = "ranged:Dague";
    static final String BOW = "ranged:Arc court";

    static GameState state(Creature.Builder attacker, Creature.Builder target) {
        return new GameState().add(attacker.build()).add(target.build());
    }

    static Creature.Builder attacker() {
        return Creature.builder("a", "Attaquant").weapon(LONGSWORD).weapon(DAGGER).weapon(SHORTBOW);
    }

    static Creature.Builder target() {
        return Creature.builder("c", "Cible").armorClass(10).hitPoints(30);
    }

    static Engine<GameState, GameEvent> engine(GameState state, int... dice) {
        return new Engine<>(state, StandardRules.create(), Dice.scripted(dice));
    }

    static StepResult attack(Engine<GameState, GameEvent> e, String attackId, double metres) {
        return e.submit(AttackAction.of("a", attackId, "c", metres, DistanceUnit.METRE, e.state()));
    }

    static GameEvent.D20Rolled d20(Engine<GameState, GameEvent> e) {
        return e.events().stream().filter(GameEvent.D20Rolled.class::isInstance).map(GameEvent.D20Rolled.class::cast)
                .findFirst().orElseThrow();
    }

    @Test
    void epeeAUneCaseDeDistance_unSeulD20() {
        var e = engine(state(attacker(), target()), 15, 4);

        assertInstanceOf(StepResult.Idle.class, attack(e, MELEE_SWORD, 1.5));

        assertEquals(List.of(15), d20(e).rolls());
        assertEquals(30 - (4 + 3), e.state().get("c").hitPoints());
    }

    @Test
    void dagueLanceeDansLaPorteeNormale_memeBonusEtMemesDegats() {
        var e = engine(state(attacker(), target()), 10, 3);

        attack(e, THROWN_DAGGER, 6);

        assertEquals(List.of(10), d20(e).rolls());
        var declared = e.events().stream().filter(GameEvent.AttackDeclared.class::isInstance)
                .map(GameEvent.AttackDeclared.class::cast).findFirst().orElseThrow();
        assertEquals("Dague (lancer)", declared.weapon());
        var resolved = e.events().stream().filter(GameEvent.AttackResolved.class::isInstance)
                .map(GameEvent.AttackResolved.class::cast).findFirst().orElseThrow();
        assertEquals(10 + 4, resolved.total());
        assertEquals(30 - (3 + 2), e.state().get("c").hitPoints());
    }

    @Test
    void arcAuDelaDeLaPorteeNormale_desavantage() {
        var e = engine(state(attacker(), target()), 18, 7, 2);

        attack(e, BOW, 30);

        var d20 = d20(e);
        assertEquals(List.of(18, 7), d20.rolls());
        assertEquals(7, d20.kept());
        assertTrue(d20.mode().contains(RangeRules.LONG_RANGE), d20.mode());
    }

    @Test
    void arcExactementALaPorteeNormale_pasDeDesavantage() {
        var e = engine(state(attacker(), target()), 18, 2);

        attack(e, BOW, 24);

        assertEquals(List.of(18), d20(e).rolls());
    }

    @Test
    void voisinEnDiagonale_horsDAllonge_refuseSansEvenement() {
        var e = engine(state(attacker(), target()));

        var refused = assertThrows(IllegalArgumentException.class, () -> attack(e, MELEE_SWORD, Math.hypot(1.5, 1.5)));

        assertEquals("Épée longue : cible à 2,1 m, hors d'allonge (1,5 m)", refused.getMessage());
        assertTrue(e.events().isEmpty());
    }

    @Test
    void arcAuDelaDeLaPorteeLongue_refuse() {
        var e = engine(state(attacker(), target()));

        var refused = assertThrows(IllegalArgumentException.class, () -> attack(e, BOW, 97));

        assertEquals("Arc court : cible à 97 m, au-delà de la portée longue (96 m)", refused.getMessage());
        assertTrue(e.events().isEmpty());
    }

    @Test
    void surUneCarteEnPieds_laRaisonEstEnPieds() {
        var state = state(attacker(), target());

        var refused = assertThrows(IllegalArgumentException.class,
                () -> AttackAction.of("a", THROWN_DAGGER, "c", 65, DistanceUnit.FOOT, state));

        assertEquals("Dague (lancer) : cible à 65 ft, au-delà de la portée longue (60 ft)", refused.getMessage());
    }

    @Test
    void dagueLanceeAVingtCinqPieds_desavantage() {
        var state = state(attacker(), target());
        var e = engine(state, 12, 5, 1);

        e.submit(AttackAction.of("a", THROWN_DAGGER, "c", 25, DistanceUnit.FOOT, state));

        assertEquals(List.of(12, 5), d20(e).rolls());
    }

    @Test
    void attaquerSoiMemeOuAvecUneAttaqueInconnue_refuse() {
        var state = state(attacker(), target());

        assertThrows(IllegalArgumentException.class,
                () -> AttackAction.of("a", MELEE_SWORD, "a", 0, DistanceUnit.METRE, state));
        var unknown = assertThrows(IllegalArgumentException.class,
                () -> AttackAction.of("a", "melee:Arc court", "c", 0, DistanceUnit.METRE, state));
        assertTrue(unknown.getMessage().contains("Attaque inconnue"), unknown.getMessage());
    }

    @Test
    void styleDuel_neSAppliquePasAUneDagueLancee() {
        var e = engine(state(attacker().feature(DuelingStyle.FEATURE), target()), 15, 3);

        attack(e, THROWN_DAGGER, 3);

        assertEquals(30 - (3 + 2), e.state().get("c").hitPoints());
        assertTrue(e.events().stream().noneMatch(ev -> ev instanceof GameEvent.RuleApplied r && r.source().equals("Style Duel")));
    }

    @Test
    void styleDuel_sAppliqueALaDagueAuCorpsACorps() {
        var e = engine(state(attacker().feature(DuelingStyle.FEATURE), target()), 15, 3);

        attack(e, "melee:Dague", 1.5);

        assertEquals(30 - (3 + 2 + 2), e.state().get("c").hitPoints());
    }

    @Test
    void porteeLongueEtCibleEntravee_sAnnulent_unSeulD20() {
        var e = engine(state(attacker(), target().condition(Condition.RESTRAINED)), 9, 2);

        attack(e, BOW, 50);

        assertEquals(List.of(9), d20(e).rolls());
    }

    @Test
    void rejouerLeJournalDUneAttaqueADistance_redonneLEtatFinal() {
        var state = state(attacker(), target());
        var initial = state.copy();
        var e = engine(state, 4, 17, 6);

        attack(e, BOW, 40);

        e.events().forEach(ev -> ev.apply(initial));
        assertEquals(e.state().signature(), initial.signature());
    }
}
