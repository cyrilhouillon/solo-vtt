package dnd.engine.attack;

import dnd.engine.events.GameEvent;
import dnd.engine.state.Attack;
import dnd.engine.state.Creature;
import dnd.engine.state.GameState;
import solovtt.engine.Pipeline;
import solovtt.scene.DistanceUnit;

public final class AttackAction {

    private AttackAction() {
    }

    /**
     * Une attaque choisie parmi celles de l'attaquant, contre une cible à {@code distance},
     * exprimée dans {@code unit}. Ce que les règles interdisent (soi-même, hors d'allonge, au-delà
     * de la portée longue) est refusé ici, avant toute émission : rien ne s'est passé dans la partie.
     *
     * @throws IllegalArgumentException avec la raison du refus, les longueurs données dans {@code unit}
     */
    public static Pipeline<GameState, GameEvent, AttackPhase, AttackContext> of(
            String attackerId, String attackId, String targetId, double distance, DistanceUnit unit, GameState state) {
        Creature attacker = state.get(attackerId);
        state.get(targetId);
        if (attackerId.equals(targetId)) {
            throw new IllegalArgumentException("Une créature ne peut pas s'attaquer elle-même");
        }
        Attack attack = attacker.attacks().stream().filter(a -> a.id().equals(attackId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Attaque inconnue pour %s : %s".formatted(attacker.name(), attackId)));
        double metres = unit.convert(distance, DistanceUnit.METRE);
        attack.refusal(metres, unit).ifPresent(reason -> {
            throw new IllegalArgumentException(reason);
        });
        return new Pipeline<>("Attaque", AttackPhase.class, new AttackContext(attackerId, targetId, attack, metres));
    }

    /** La première attaque de l'attaquant, au contact : pour les scénarios sans carte (démo, tests). */
    public static Pipeline<GameState, GameEvent, AttackPhase, AttackContext> of(String attackerId, String targetId, GameState state) {
        var attacks = state.get(attackerId).attacks();
        if (attacks.isEmpty()) throw new IllegalArgumentException(state.get(attackerId).name() + " n'a aucune attaque");
        return of(attackerId, attacks.getFirst().id(), targetId, 0, DistanceUnit.METRE, state);
    }
}
