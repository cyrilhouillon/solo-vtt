package dnd.engine.rules;

import dnd.engine.attack.AttackContext;
import dnd.engine.attack.AttackPhase;
import dnd.engine.attack.CoreAttackRules.AttackRule;
import dnd.engine.events.GameEvent;
import dnd.engine.state.Creature;
import dnd.engine.state.GameState;
import solovtt.engine.Decision;
import solovtt.engine.Engine;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * La fenêtre de réaction d'une attaque qui touche (Bouclier…) : le jet est connu, le coup pas encore confirmé.
 * Elle empile une {@link ReactionWindow} ; la cible est interrogée la première, puis les autres créatures.
 */
public final class AttackHitReactions implements AttackRule {

    private final List<Reaction<AttackContext>> reactions;

    public AttackHitReactions(List<Reaction<AttackContext>> reactions) {
        this.reactions = List.copyOf(reactions);
    }

    public AttackPhase phase() { return AttackPhase.HIT_REACTIONS; }

    public boolean appliesTo(AttackContext ctx, GameState s) {
        return ctx.hit();
    }

    public Optional<Decision> handle(AttackContext ctx, Engine<GameState, GameEvent> e) {
        Creature target = e.state().get(ctx.targetId());
        String situation = "Coup au but sur %s (%d contre CA %d).".formatted(target.name(), ctx.total(), target.armorClass());
        List<String> reactors = Stream.concat(Stream.of(target.id()),
                e.state().creatures().stream().map(Creature::id).filter(id -> !id.equals(target.id()))).toList();
        e.push(new ReactionWindow<>(situation, ctx, reactions, reactors));
        return Optional.empty();
    }
}
