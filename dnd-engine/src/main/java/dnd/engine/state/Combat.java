package dnd.engine.state;

import solovtt.engine.TurnOrder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Un combat en cours : les groupes d'initiative du plus haut au plus bas, les créatures qui ont déjà joué
 * ce round, et le tour en cours. Les mutations sont réservées à {@code GameEvent.apply}.
 */
public final class Combat {

    private final List<InitiativeGroup> groups;
    private final Set<String> played;
    private int round;
    private TurnOrder turn;

    public Combat(List<InitiativeGroup> groups) {
        this(new ArrayList<>(groups), new LinkedHashSet<>(), 1, null);
    }

    private Combat(List<InitiativeGroup> groups, Set<String> played, int round, TurnOrder turn) {
        this.groups = groups;
        this.played = played;
        this.round = round;
        this.turn = turn;
    }

    public Combat copy() {
        return new Combat(new ArrayList<>(groups), new LinkedHashSet<>(played), round, turn);
    }

    // --- Lecture -------------------------------------------------------------

    public List<InitiativeGroup> groups() {
        return List.copyOf(groups);
    }

    public int round() {
        return round;
    }

    /** Le tour en cours ; vide un instant entre la fin d'un tour et le début du suivant. */
    public Optional<TurnOrder> turn() {
        return Optional.ofNullable(turn);
    }

    public Optional<String> activeId() {
        return turn().flatMap(TurnOrder::activeId);
    }

    public boolean hasPlayed(String creatureId) {
        return played.contains(creatureId);
    }

    public boolean includes(String creatureId) {
        return groupOf(creatureId).isPresent();
    }

    public Optional<InitiativeGroup> groupOf(String creatureId) {
        return groups.stream().filter(g -> g.members().contains(creatureId)).findFirst();
    }

    public String signature() {
        String order = groups.stream()
                .map(g -> g.initiative() + "/" + g.modifier() + g.members())
                .collect(Collectors.joining(","));
        return "combat[round=%d, ordre=%s, joué=%s, tour=%s]".formatted(round, order, played, turn);
    }

    // --- Mutations (réservées à GameEvent.apply) -------------------------------

    public void startRound(int round) {
        this.round = round;
        played.clear();
    }

    public void offer(TurnOrder turn) {
        this.turn = turn;
    }

    /**
     * La créature joue : elle est active et a joué ce round. Sans tour proposé, elle est le candidat unique
     * (aucun {@code TurnOffered} ne la précède) ; sinon elle doit être parmi les candidats.
     *
     * @throws IllegalArgumentException hors des candidats proposés
     * @throws IllegalStateException si une créature joue déjà
     */
    public void startTurn(String creatureId) {
        turn = turn == null ? TurnOrder.offer(round, List.of(creatureId)) : turn.choose(creatureId);
        played.add(creatureId);
    }

    public void endTurn() {
        turn = null;
    }

    /** Range la créature dans son groupe d'initiative, créé à sa place s'il n'existe pas. */
    public void join(String creatureId, int initiative, int modifier, boolean playedThisRound) {
        int index = 0;
        while (index < groups.size() && groups.get(index).ranksBefore(initiative, modifier)) index++;
        if (index < groups.size() && groups.get(index).sameRank(initiative, modifier)) {
            InitiativeGroup g = groups.get(index);
            List<String> members = new ArrayList<>(g.members());
            members.add(creatureId);
            groups.set(index, new InitiativeGroup(initiative, modifier, members));
        } else {
            groups.add(index, new InitiativeGroup(initiative, modifier, List.of(creatureId)));
        }
        if (playedThisRound) played.add(creatureId);
    }

    /** Retire la créature de l'ordre ; si elle jouait, le tour se termine. */
    public void leave(String creatureId) {
        for (int i = 0; i < groups.size(); i++) {
            InitiativeGroup g = groups.get(i);
            if (!g.members().contains(creatureId)) continue;
            List<String> members = new ArrayList<>(g.members());
            members.remove(creatureId);
            if (members.isEmpty()) groups.remove(i);
            else groups.set(i, new InitiativeGroup(g.initiative(), g.modifier(), members));
            break;
        }
        played.remove(creatureId);
        if (turn != null && creatureId.equals(turn.active())) turn = null;
    }

    public boolean isEmpty() {
        return groups.isEmpty();
    }
}
