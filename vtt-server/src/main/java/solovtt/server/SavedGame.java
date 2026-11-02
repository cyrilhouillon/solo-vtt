package solovtt.server;

import solovtt.scene.DistanceUnit;

import java.util.List;

/**
 * Une partie sauvée : les commandes réussies de la session depuis la dernière carte, dans l'ordre,
 * chacune avec les dés qu'elle a tirés. Les rejouer sur une session neuve redonne la partie à l'identique,
 * décision en attente comprise.
 */
public record SavedGame(List<SavedCommand> commands) {

    public SavedGame {
        commands = List.copyOf(commands);
    }

    /** Un appel réussi d'une méthode de {@link SceneSession} qui modifie la partie, et les dés tirés pendant cet appel. */
    public sealed interface SavedCommand {

        List<Integer> rolls();

        record Create(double width, double height, DistanceUnit unit, List<Integer> rolls) implements SavedCommand {}

        record Place(String catalogId, double x, double y, List<Integer> rolls) implements SavedCommand {}

        record Remove(String tokenId, List<Integer> rolls) implements SavedCommand {}

        record Move(String tokenId, double x, double y, List<Integer> rolls) implements SavedCommand {}

        record Attack(String attackerId, String attackId, String targetId, List<Integer> rolls) implements SavedCommand {}

        record StartCombat(List<Integer> rolls) implements SavedCommand {}

        record EndTurn(List<Integer> rolls) implements SavedCommand {}

        record EndCombat(List<Integer> rolls) implements SavedCommand {}

        record Answer(String decisionId, String option, List<Integer> rolls) implements SavedCommand {}
    }
}
