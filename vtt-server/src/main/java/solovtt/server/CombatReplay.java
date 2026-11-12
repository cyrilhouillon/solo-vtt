package solovtt.server;

import dnd.engine.state.Creature;
import solovtt.scene.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Le rejeu pas à pas d'un combat : sa partie est rejouée jusqu'au lancement exclu (le pas 0, le départ), puis une commande
 * après l'autre.
 * Un pas se ferme après une commande qui a écrit au journal sans laisser de décision en attente : une attaque et la
 * réponse à la réaction qu'elle déclenche forment un seul pas. Une commande qui n'écrit rien (repositionnement libre,
 * grille) rejoint le pas suivant.
 */
final class CombatReplay {

    private CombatReplay() {}

    /**
     * Les pas du combat {@code combatId}, dans l'ordre.
     *
     * @throws ActionRefusedException   si le combat est encore en cours
     * @throws IllegalArgumentException si la partie ne lance pas ce combat
     * @throws RuntimeException         si la partie ne se rejoue pas à l'identique
     */
    static List<Dtos.ReplayStepDto> of(SavedGame game, String combatId) {
        List<SavedGame.SavedCommand> commands = game.commands();
        int start = -1;
        for (int i = 0; i < commands.size(); i++) {
            if (commands.get(i) instanceof SavedGame.SavedCommand.StartCombat s && combatId.equals(s.combatId())) {
                start = i;
                break;
            }
        }
        if (start < 0) throw new IllegalArgumentException("La partie ne lance pas le combat " + combatId);

        SceneSession session = SceneSession.replayed(new SavedGame(List.of()));
        for (int i = 0; i < start; i++) session.replayNext(commands.get(i));
        List<Dtos.ReplayStepDto> steps = new ArrayList<>();
        steps.add(step(session, List.of(), null));

        int from = session.journal().size();
        String targetId = null;
        for (int i = start; i < commands.size(); i++) {
            SavedGame.SavedCommand command = commands.get(i);
            session.replayNext(command);
            if (command instanceof SavedGame.SavedCommand.Attack attack) targetId = attack.targetId();
            List<SceneSession.JournalEntry> journal = session.journal();
            if (journal.size() > from && session.pendingDecision().isEmpty()) {
                steps.add(step(session, journal.subList(from, journal.size()), targetId));
                from = journal.size();
                targetId = null;
            }
        }
        if (session.combatId().isPresent()) throw new ActionRefusedException("Ce combat est en cours : il se rejoue une fois terminé");
        return steps;
    }

    private static Dtos.ReplayStepDto step(SceneSession session, List<SceneSession.JournalEntry> entries, String targetId) {
        var scene = session.scene().orElseThrow();
        Optional<Creature> target = Optional.ofNullable(targetId).flatMap(session::creature);
        List<Dtos.CreatureSheetDto> sheets = scene.tokens().stream()
                .map(Token::entityId)
                .map(id -> Dtos.CreatureSheetDto.of(session.creature(id).orElseThrow(), session.entryOf(id).kind(), scene.unit(),
                        session.attackRefusal(id).orElse(null)))
                .toList();
        return new Dtos.ReplayStepDto(entries.stream().map(Dtos.JournalEntryDto::of).toList(),
                target.map(Creature::id).orElse(null),
                target.map(Creature::hitPoints).orElse(null),
                target.map(Creature::maxHitPoints).orElse(null),
                Dtos.SceneDto.of(scene, session),
                session.combat().map(c -> Dtos.CombatDto.of(c, session)).orElse(null),
                sheets);
    }
}
