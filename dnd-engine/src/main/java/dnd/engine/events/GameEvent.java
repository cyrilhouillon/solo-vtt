package dnd.engine.events;

import solovtt.engine.Event;
import dnd.engine.state.Combat;
import dnd.engine.state.DamageType;
import dnd.engine.state.GameState;
import dnd.engine.state.InitiativeGroup;
import dnd.engine.state.Length;
import solovtt.engine.TurnOrder;
import solovtt.scene.DistanceUnit;

import java.util.List;
import java.util.stream.Collectors;

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

    private static String names(GameState s, List<String> ids) {
        return ids.stream().map(id -> name(s, id)).collect(Collectors.joining(", "));
    }

    private static Combat combat(GameState s) {
        return s.combat().orElseThrow(() -> new IllegalStateException("Aucun combat en cours"));
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

    /** Des dés tirés hors d20 (bonus au jet, dégâts) : {@code values.size()} dés à {@code sides} faces. */
    record DiceRolled(String source, int sides, List<Integer> values) implements GameEvent {
        public DiceRolled {
            values = List.copyOf(values);
        }

        /** « 2d6 » : le nombre de dés tirés et leurs faces. */
        public String notation() {
            return values.size() + "d" + sides;
        }

        public String describe(GameState s) {
            return "%s %s : %s = %d".formatted(source, notation(), values, sum(values));
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

    // --- Combat ------------------------------------------------------------------

    record CombatStarted() implements GameEvent {
        public String describe(GameState s) {
            return "Le combat commence";
        }
    }

    /** Le joueur met fin au combat : plus d'ordre ni de tour ; les créatures gardent leurs PV. */
    record CombatEnded() implements GameEvent {
        public void apply(GameState s) {
            s.endCombat();
        }

        public String describe(GameState s) {
            return "Le combat se termine";
        }
    }

    record InitiativeRolled(String creatureId, int roll, int bonus) implements GameEvent {
        public String describe(GameState s) {
            return "Initiative de %s : d20 %d %s %d = %d".formatted(
                    name(s, creatureId), roll, bonus < 0 ? "-" : "+", Math.abs(bonus), roll + bonus);
        }
    }

    /** Fixe l'ordre de jeu : le combat commence dans l'état. */
    record InitiativeOrderSet(List<InitiativeGroup> groups) implements GameEvent {
        public InitiativeOrderSet {
            groups = List.copyOf(groups);
        }

        public void apply(GameState s) {
            s.startCombat(groups);
        }

        public String describe(GameState s) {
            return "Ordre de jeu : " + groups.stream()
                    .map(g -> "%s (%d)".formatted(String.join(" et ", g.members().stream().map(id -> name(s, id)).toList()), g.initiative()))
                    .collect(Collectors.joining(", "));
        }
    }

    record RoundStarted(int round) implements GameEvent {
        public void apply(GameState s) {
            combat(s).startRound(round);
        }

        public String describe(GameState s) {
            return "Round " + round;
        }
    }

    /** Plusieurs créatures peuvent jouer : le joueur choisit laquelle. */
    record TurnOffered(int round, List<String> candidates) implements GameEvent {
        public TurnOffered {
            candidates = List.copyOf(candidates);
        }

        public void apply(GameState s) {
            combat(s).offer(TurnOrder.offer(round, candidates));
        }

        public String describe(GameState s) {
            return "À jouer, au choix : " + names(s, candidates);
        }
    }

    record TurnStarted(String creatureId) implements GameEvent {
        public void apply(GameState s) {
            combat(s).startTurn(creatureId);
        }

        public String describe(GameState s) {
            return "Tour de " + name(s, creatureId);
        }
    }

    record TurnEnded(String creatureId) implements GameEvent {
        public void apply(GameState s) {
            combat(s).endTurn();
        }

        public String describe(GameState s) {
            return "Fin du tour de " + name(s, creatureId);
        }
    }

    /**
     * La créature active se déplace de {@code metres} et les dépense de sa vitesse ; {@code unit} est celle de la carte,
     * pour décrire le déplacement dans l'unité du joueur.
     */
    record CreatureMoved(String creatureId, double metres, DistanceUnit unit) implements GameEvent {
        public void apply(GameState s) {
            combat(s).spendMovement(metres);
        }

        public String describe(GameState s) {
            double left = combat(s).movementLeft(s.get(creatureId));
            return "%s se déplace de %s (reste %s)".formatted(name(s, creatureId), Length.of(metres, unit), Length.of(left, unit));
        }
    }

    record ReactionRegained(String creatureId) implements GameEvent {
        public void apply(GameState s) {
            s.get(creatureId).regainReaction();
        }

        public String describe(GameState s) {
            return name(s, creatureId) + " récupère sa réaction";
        }
    }

    /** Une créature arrive en plein combat ; {@code playedThisRound} si son rang est déjà passé dans ce round. */
    record JoinedCombat(String creatureId, int initiative, int modifier, boolean playedThisRound) implements GameEvent {
        public void apply(GameState s) {
            combat(s).join(creatureId, initiative, modifier, playedThisRound);
        }

        public String describe(GameState s) {
            return "%s entre dans le combat (initiative %d%s)".formatted(
                    name(s, creatureId), initiative, playedThisRound ? ", jouera au round suivant" : "");
        }
    }

    /** Une créature quitte le combat ; s'il ne reste personne, le combat s'arrête. */
    record LeftCombat(String creatureId) implements GameEvent {
        public void apply(GameState s) {
            Combat c = combat(s);
            c.leave(creatureId);
            if (c.isEmpty()) s.endCombat();
        }

        public String describe(GameState s) {
            return name(s, creatureId) + " quitte le combat";
        }
    }
}
