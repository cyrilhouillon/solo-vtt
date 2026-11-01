package dnd.engine.attack;

import solovtt.engine.Decision;
import solovtt.engine.Engine;
import solovtt.engine.Listener;
import dnd.engine.events.GameEvent;
import dnd.engine.events.GameEvent.AttackDeclared;
import dnd.engine.events.GameEvent.AttackResolved;
import dnd.engine.events.GameEvent.D20Rolled;
import dnd.engine.events.GameEvent.DamageDealt;
import dnd.engine.events.GameEvent.DiceRolled;
import dnd.engine.events.GameEvent.RuleApplied;
import dnd.engine.state.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Les règles de base d'une attaque (règles 2024 / SRD 5.2), écrites comme des listeners
 * ordinaires. Elles n'ont aucun privilège par rapport aux règles optionnelles.
 */
public final class CoreAttackRules {

    private CoreAttackRules() {
    }

    public static List<AttackRule> all() {
        return List.of(
                new Declare(),
                new RollAttack(),
                new DetermineHit(),
                new ConfirmHit(),
                new RollDamage(),
                new ApplyResistance(),
                new ApplyVulnerability(),
                new ApplyDamage());
    }

    /** Petite base pour éviter de répéter la signature générique partout. */
    /** Une règle du pipeline d'attaque D&D. */
    public interface AttackRule extends Listener<GameState, GameEvent, AttackPhase, AttackContext> {
    }

    static List<Integer> rollMany(Engine<GameState, GameEvent> engine, int count, int sides) {
        List<Integer> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) values.add(engine.roll(sides));
        return values;
    }

    static int sum(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).sum();
    }

    // -------------------------------------------------------------------------

    static final class Declare implements AttackRule {
        public AttackPhase phase() { return AttackPhase.DECLARE; }

        public Optional<Decision> handle(AttackContext ctx, Engine<GameState, GameEvent> e) {
            e.emit(new AttackDeclared(ctx.attackerId(), ctx.targetId(), ctx.attack().label()));
            return Optional.empty();
        }
    }

    /**
     * Avantage et désavantage sont résolus ici, en une fois, à partir de ce que les
     * listeners de MODIFY_ROLL ont accumulé : peu importe combien de sources, ni dans quel ordre.
     */
    static final class RollAttack implements AttackRule {
        public AttackPhase phase() { return AttackPhase.ROLL; }

        public Optional<Decision> handle(AttackContext ctx, Engine<GameState, GameEvent> e) {
            boolean adv = !ctx.advantageSources().isEmpty();
            boolean dis = !ctx.disadvantageSources().isEmpty();

            String mode = "normal";
            if (adv && dis) {
                e.emit(new RuleApplied("Règles", "avantage (%s) et désavantage (%s) s'annulent"
                        .formatted(String.join(", ", ctx.advantageSources()), String.join(", ", ctx.disadvantageSources()))));
            } else if (adv) {
                mode = "avantage : " + String.join(", ", ctx.advantageSources());
            } else if (dis) {
                mode = "désavantage : " + String.join(", ", ctx.disadvantageSources());
            }

            List<Integer> rolls = (adv ^ dis) ? List.of(e.roll(20), e.roll(20)) : List.of(e.roll(20));
            int kept = adv && !dis ? Math.max(rolls.get(0), rolls.get(1))
                    : dis && !adv ? Math.min(rolls.get(0), rolls.get(1))
                    : rolls.get(0);
            e.emit(new D20Rolled(ctx.attackerId(), rolls, kept, mode));

            int total = kept + ctx.weapon().attackBonus();
            for (AttackContext.DiceTerm term : ctx.attackDice()) {
                List<Integer> values = rollMany(e, term.count(), term.sides());
                e.emit(new DiceRolled(term.source(), term.notation(), values));
                total += sum(values);
            }
            ctx.setNatural(kept);
            ctx.setTotal(total);
            return Optional.empty();
        }
    }

    /** 20 naturel : touche et critique. 1 naturel : rate toujours. */
    static final class DetermineHit implements AttackRule {
        public AttackPhase phase() { return AttackPhase.DETERMINE_HIT; }

        public Optional<Decision> handle(AttackContext ctx, Engine<GameState, GameEvent> e) {
            int ac = e.state().get(ctx.targetId()).armorClass();
            boolean critical = ctx.natural() == 20;
            boolean hit = critical || (ctx.natural() != 1 && ctx.total() >= ac);
            ctx.setCritical(critical);
            ctx.setHit(hit);
            String detail = ctx.natural() == 1 ? "1 naturel, échec automatique"
                    : critical ? "20 naturel, coup critique"
                    : hit ? "touché (provisoire)" : "raté";
            e.emit(new RuleApplied("Règles", "total %d contre CA %d : %s".formatted(ctx.total(), ac, detail)));
            return Optional.empty();
        }
    }

    /** Après la fenêtre de réaction : la CA a peut-être changé (Bouclier). */
    static final class ConfirmHit implements AttackRule {
        public AttackPhase phase() { return AttackPhase.CONFIRM_HIT; }

        public Optional<Decision> handle(AttackContext ctx, Engine<GameState, GameEvent> e) {
            int ac = e.state().get(ctx.targetId()).armorClass();
            if (ctx.hit() && !ctx.critical() && ctx.total() < ac) {
                ctx.setHit(false);
            }
            e.emit(new AttackResolved(ctx.attackerId(), ctx.targetId(), ctx.total(), ac, ctx.hit(), ctx.critical()));
            return Optional.empty();
        }
    }

    static final class RollDamage implements AttackRule {
        public AttackPhase phase() { return AttackPhase.ROLL_DAMAGE; }

        public boolean appliesTo(AttackContext ctx, GameState s) { return ctx.hit(); }

        public Optional<Decision> handle(AttackContext ctx, Engine<GameState, GameEvent> e) {
            var weapon = ctx.weapon();
            int count = weapon.diceCount() * (ctx.critical() ? 2 : 1);
            List<Integer> values = rollMany(e, count, weapon.dieSides());
            e.emit(new DiceRolled(weapon.name(), count + "d" + weapon.dieSides(), values));
            int damage = sum(values) + weapon.damageBonus();
            e.emit(new RuleApplied("Dégâts", "%d (dés) %+d (modificateur) = %d"
                    .formatted(sum(values), weapon.damageBonus(), damage)));
            ctx.setDamage(damage);
            return Optional.empty();
        }
    }

    static final class ApplyResistance implements AttackRule {
        public AttackPhase phase() { return AttackPhase.DAMAGE_RESISTANCE; }

        public boolean appliesTo(AttackContext ctx, GameState s) {
            return ctx.hit() && s.get(ctx.targetId()).isResistantTo(ctx.weapon().damageType());
        }

        public Optional<Decision> handle(AttackContext ctx, Engine<GameState, GameEvent> e) {
            int before = ctx.damage();
            ctx.setDamage(before / 2);
            e.emit(new RuleApplied("Résistance", "%d ÷ 2 = %d (arrondi inférieur)".formatted(before, ctx.damage())));
            return Optional.empty();
        }
    }

    static final class ApplyVulnerability implements AttackRule {
        public AttackPhase phase() { return AttackPhase.DAMAGE_VULNERABILITY; }

        public boolean appliesTo(AttackContext ctx, GameState s) {
            return ctx.hit() && s.get(ctx.targetId()).isVulnerableTo(ctx.weapon().damageType());
        }

        public Optional<Decision> handle(AttackContext ctx, Engine<GameState, GameEvent> e) {
            int before = ctx.damage();
            ctx.setDamage(before * 2);
            e.emit(new RuleApplied("Vulnérabilité", "%d × 2 = %d".formatted(before, ctx.damage())));
            return Optional.empty();
        }
    }

    static final class ApplyDamage implements AttackRule {
        public AttackPhase phase() { return AttackPhase.APPLY_DAMAGE; }

        public boolean appliesTo(AttackContext ctx, GameState s) { return ctx.hit(); }

        public Optional<Decision> handle(AttackContext ctx, Engine<GameState, GameEvent> e) {
            e.emit(new DamageDealt(ctx.targetId(), ctx.damage(), ctx.weapon().damageType()));
            return Optional.empty();
        }
    }
}
