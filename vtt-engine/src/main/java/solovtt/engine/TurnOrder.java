package solovtt.engine;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Où en est l'enchaînement des tours : le round, les participants qui peuvent jouer maintenant
 * (les candidats), et celui qui joue (l'actif), s'il est déjà choisi.
 *
 * <p>L'ordre ne sait rien d'une initiative : c'est la {@link TurnPolicy} du système qui fournit
 * les candidats. Un seul candidat devient actif d'office ; s'il y en a plusieurs (ex æquo d'initiative
 * en D&D, « les joueurs » dans Daggerheart), le joueur choisit qui joue ({@link #choose}).</p>
 *
 * @param round      le round en cours, à partir de 1
 * @param candidates les ids des participants qui peuvent jouer, dans l'ordre d'affichage, sans doublon
 * @param active     l'id du participant qui joue, ou {@code null} tant qu'il n'est pas choisi
 */
public record TurnOrder(int round, List<String> candidates, String active) {

    public TurnOrder {
        if (round < 1) throw new IllegalArgumentException("Le round commence à 1 : " + round);
        if (candidates == null || candidates.isEmpty()) throw new IllegalArgumentException("Aucun participant ne peut jouer");
        candidates = List.copyOf(candidates);
        if (new HashSet<>(candidates).size() != candidates.size()) {
            throw new IllegalArgumentException("Participant en double : " + candidates);
        }
        if (active != null && !candidates.contains(active)) {
            throw new IllegalArgumentException("L'actif n'est pas candidat : " + active);
        }
    }

    /** Les candidats d'un tour ; un seul devient actif d'office, plusieurs restent à choisir. */
    public static TurnOrder offer(int round, List<String> candidates) {
        return new TurnOrder(round, candidates, candidates != null && candidates.size() == 1 ? candidates.getFirst() : null);
    }

    /** L'ordre avec ce candidat actif. */
    public TurnOrder choose(String participantId) {
        if (active != null) throw new IllegalStateException("Un participant joue déjà : " + active);
        if (!candidates.contains(participantId)) {
            throw new IllegalArgumentException("Ce participant ne peut pas jouer maintenant : " + participantId);
        }
        return new TurnOrder(round, candidates, participantId);
    }

    public Optional<String> activeId() {
        return Optional.ofNullable(active);
    }

    /** Plusieurs candidats, et personne n'est encore choisi. */
    public boolean awaitingChoice() {
        return active == null;
    }
}
