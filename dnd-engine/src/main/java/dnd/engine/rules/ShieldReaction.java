package dnd.engine.rules;

import dnd.engine.attack.AttackContext;
import dnd.engine.attack.AttackPhase;
import dnd.engine.core.Decision;
import dnd.engine.core.Engine;
import dnd.engine.core.Listener;
import dnd.engine.events.GameEvent.ArmorClassBonusApplied;
import dnd.engine.events.GameEvent.ReactionUsed;
import dnd.engine.events.GameEvent.RuleApplied;
import dnd.engine.events.GameEvent.SpellSlotExpended;
import dnd.engine.state.Creature;
import dnd.engine.state.GameState;

import java.util.List;
import java.util.Optional;

/**
 * Bouclier (2024) : réaction déclenchée quand on est touché par un jet d'attaque.
 * +5 à la CA, y compris contre l'attaque déclencheuse, jusqu'au début du prochain tour.
 *
 * <p>C'est l'exemple type de règle qui SUSPEND le moteur : le jet est connu, le moteur
 * s'arrête et demande à la cible si elle veut réagir.</p>
 *
 * <p>Pour ne pas spammer le joueur, on ne propose le sort que s'il change le résultat.
 * Un vrai jeu pourrait laisser le choix au joueur via une option.</p>
 */
public final class ShieldReaction implements Listener<AttackPhase, AttackContext> {

    public static final String SPELL = "shield";
    public static final int BONUS = 5;
    public static final String CAST = "Lancer Bouclier";
    public static final String DECLINE = "Ne rien faire";

    public AttackPhase phase() { return AttackPhase.HIT_REACTIONS; }

    public boolean appliesTo(AttackContext ctx, GameState s) {
        Creature target = s.get(ctx.targetId());
        return ctx.hit()
                && !ctx.critical()                       // un 20 naturel touche quoi qu'il arrive
                && target.reactionAvailable()
                && target.knowsSpell(SPELL)
                && target.lowestAvailableSlot().isPresent()
                && ctx.total() < target.armorClass() + BONUS;
    }

    public Optional<Decision> handle(AttackContext ctx, Engine e) {
        Creature target = e.state().get(ctx.targetId());
        String targetId = target.id();
        int slot = target.lowestAvailableSlot().getAsInt();

        String prompt = "%s est touchée (%d contre CA %d). Lancer Bouclier (réaction, emplacement de niveau %d) ?"
                .formatted(target.name(), ctx.total(), target.armorClass(), slot);

        return Optional.of(e.ask(targetId, prompt, List.of(CAST, DECLINE), (choice, engine) -> {
            if (CAST.equals(choice)) {
                engine.emit(new ReactionUsed(targetId, "Bouclier"));
                engine.emit(new SpellSlotExpended(targetId, slot));
                engine.emit(new ArmorClassBonusApplied(targetId, "Bouclier", BONUS));
                // TODO : retirer le bonus au début du prochain tour de la cible.
                // Il faut pour ça une structure de tour (événement TurnStarted + effets à durée).
            } else {
                engine.emit(new RuleApplied(engine.state().get(targetId).name(), "renonce à Bouclier"));
            }
        }));
    }
}
