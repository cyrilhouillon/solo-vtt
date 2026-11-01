package dnd.engine.events;

import dnd.engine.core.Event;
import dnd.engine.state.DamageType;
import dnd.engine.state.GameState;

import java.util.List;

/**
 * Tout ce qui se passe dans la partie. Les événements qui changent l'état surchargent
 * {@link #apply} ; les autres (jets de dés, explications de règles) servent au journal
 * et à l'affichage.
 *
 * <p>Les jets de dés sont des événements : pour rejouer une partie, on rejoue le journal,
 * on ne relance pas les dés.</p>
 */
public sealed interface GameEvent extends Event<GameState> {

    default void apply(GameState state) {
    }

    /** Description lisible, évaluée après application de l'événement. */
    String describe(GameState state);

    private static String name(GameState s, String id) {
        return s.get(id).name();
    }

    private static int sum(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).sum();
    }

    // --- Événements informatifs ----------------------------------------------

    record AttackDeclared(String attackerId, String targetId, String weapon) implements GameEvent {
        public String describe(GameState s) {
            return "%s attaque %s (%s)".formatted(name(s, attackerId), name(s, targetId), weapon);
        }
    }

    record D20Rolled(String rollerId, List<Integer> rolls, int kept, String mode) implements GameEvent {
        public D20Rolled {
            rolls = List.copyOf(rolls);
        }

        public String describe(GameState s) {
            return rolls.size() == 1
                    ? "d20 : %d".formatted(kept)
                    : "d20 (%s) : %s → %d".formatted(mode, rolls, kept);
        }
    }

    record DiceRolled(String source, String notation, List<Integer> values) implements GameEvent {
        public DiceRolled {
            values = List.copyOf(values);
        }

        public String describe(GameState s) {
            return "%s %s : %s = %d".formatted(source, notation, values, sum(values));
        }
    }

    record RuleApplied(String source, String text) implements GameEvent {
        public String describe(GameState s) {
            return "[%s] %s".formatted(source, text);
        }
    }

    record AttackResolved(String attackerId, String targetId, int total, int armorClass,
                          boolean hit, boolean critical) implements GameEvent {
        public String describe(GameState s) {
            String outcome = critical ? "COUP CRITIQUE" : hit ? "TOUCHÉ" : "RATÉ";
            return "Attaque de %s : %d contre CA %d → %s".formatted(name(s, attackerId), total, armorClass, outcome);
        }
    }

    // --- Événements qui modifient l'état ---------------------------------------

    record ReactionUsed(String creatureId, String reaction) implements GameEvent {
        public void apply(GameState s) {
            s.get(creatureId).spendReaction();
        }

        public String describe(GameState s) {
            return "%s utilise sa réaction : %s".formatted(name(s, creatureId), reaction);
        }
    }

    record SpellSlotExpended(String creatureId, int level) implements GameEvent {
        public void apply(GameState s) {
            s.get(creatureId).expendSpellSlot(level);
        }

        public String describe(GameState s) {
            return "%s dépense un emplacement de niveau %d (reste %d)"
                    .formatted(name(s, creatureId), level, s.get(creatureId).spellSlots(level));
        }
    }

    record ArmorClassBonusApplied(String creatureId, String source, int bonus) implements GameEvent {
        public void apply(GameState s) {
            s.get(creatureId).addArmorClassBonus(source, bonus);
        }

        public String describe(GameState s) {
            return "CA de %s : %+d (%s) → %d".formatted(name(s, creatureId), bonus, source, s.get(creatureId).armorClass());
        }
    }

    record DamageDealt(String targetId, int amount, DamageType type) implements GameEvent {
        public void apply(GameState s) {
            s.get(targetId).takeDamage(amount);
        }

        public String describe(GameState s) {
            var t = s.get(targetId);
            return "%s subit %d dégâts %s (PV %d/%d)".formatted(t.name(), amount, type.label(), t.hitPoints(), t.maxHitPoints());
        }
    }
}
