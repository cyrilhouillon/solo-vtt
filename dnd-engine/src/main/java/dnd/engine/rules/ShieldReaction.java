package dnd.engine.rules;

import dnd.engine.attack.AttackContext;
import dnd.engine.events.GameEvent;
import dnd.engine.events.GameEvent.ArmorClassBonusApplied;
import dnd.engine.events.GameEvent.SpellSlotExpended;
import dnd.engine.state.Creature;
import dnd.engine.state.GameState;
import solovtt.engine.Engine;

/**
 * Bouclier (2024) : réaction « when you are hit by an attack roll » (SRD 5.2.1). +5 à la CA,
 * y compris contre l'attaque déclencheuse, jusqu'au début du prochain tour.
 *
 * <p>Proposé seulement à la cible, et seulement s'il change le résultat. Le cas « ciblé par Projectile
 * magique » viendra avec les sorts, dans la fenêtre de réaction de ce déclencheur.</p>
 */
public final class ShieldReaction implements Reaction<AttackContext> {

    public static final String SPELL = "shield";
    public static final int BONUS = 5;
    public static final String CAST = "Lancer Bouclier";

    public String name() { return "Bouclier"; }

    public String option() { return CAST; }

    public boolean offeredTo(Creature reactor, AttackContext ctx, GameState s) {
        return reactor.id().equals(ctx.targetId())
                && !ctx.critical()                       // un 20 naturel touche quoi qu'il arrive
                && reactor.knowsSpell(SPELL)
                && reactor.lowestAvailableSlot().isPresent()
                && ctx.total() < reactor.armorClass() + BONUS;
    }

    public void apply(Creature reactor, AttackContext ctx, Engine<GameState, GameEvent> e) {
        e.emit(new SpellSlotExpended(reactor.id(), reactor.lowestAvailableSlot().getAsInt()));
        e.emit(new ArmorClassBonusApplied(reactor.id(), "Bouclier", BONUS));
        // TODO : retirer le bonus au début du prochain tour de la cible.
        // Il faut pour ça une structure de tour (événement TurnStarted + effets à durée).
    }
}
