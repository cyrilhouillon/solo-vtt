package dnd.engine.state;

import java.util.List;

/**
 * Les créatures qui partagent un rang d'initiative : même total, même modificateur. Chacune joue une fois
 * par round, dans l'ordre que choisit le joueur.
 *
 * @param initiative le total d'initiative (d20 + modificateur)
 * @param modifier   le modificateur d'initiative, qui départage deux totaux égaux
 * @param members    les ids des créatures, dans l'ordre d'arrivée
 */
public record InitiativeGroup(int initiative, int modifier, List<String> members) {

    public InitiativeGroup {
        members = List.copyOf(members);
    }

    /** Plus haut d'abord : total, puis modificateur. */
    public boolean ranksBefore(int otherInitiative, int otherModifier) {
        return initiative != otherInitiative ? initiative > otherInitiative : modifier > otherModifier;
    }

    public boolean sameRank(int otherInitiative, int otherModifier) {
        return initiative == otherInitiative && modifier == otherModifier;
    }
}
