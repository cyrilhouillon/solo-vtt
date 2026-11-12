package dnd.engine.sheet;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Les trois façons de déterminer les valeurs de caractéristique (SRD 5.2.1, « Generate Your Scores »). */
public enum AbilityMethod {
    STANDARD_ARRAY("Valeurs standard"),
    POINT_BUY("Achat de points"),
    ROLLED("Dés");

    public static final List<Integer> STANDARD_SCORES = List.of(15, 14, 13, 12, 10, 8);
    public static final int POINT_BUY_BUDGET = 27;
    public static final int POINT_BUY_MIN = 8;
    public static final int POINT_BUY_MAX = 15;
    private static final int[] POINT_BUY_COSTS = {0, 1, 2, 3, 4, 5, 7, 9};

    private final String label;

    AbilityMethod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Le coût d'une valeur à l'achat de points, de 8 (0 point) à 15 (9 points). */
    public static int pointBuyCost(int score) {
        if (score < POINT_BUY_MIN || score > POINT_BUY_MAX) {
            throw new IllegalArgumentException("Achat de points : valeur entre %d et %d : %d".formatted(POINT_BUY_MIN, POINT_BUY_MAX, score));
        }
        return POINT_BUY_COSTS[score - POINT_BUY_MIN];
    }

    /**
     * Refuse des valeurs que la méthode ne peut pas donner, avec la raison.
     *
     * @param scores les six valeurs
     * @param rolls  les jets de la fiche, pour la méthode {@link #ROLLED} ; ignorés sinon
     */
    public void validate(Map<Ability, Integer> scores, List<AbilityRoll> rolls) {
        for (Ability ability : Ability.values()) {
            if (scores.get(ability) == null) throw new IllegalArgumentException("Valeur manquante : " + ability.label());
        }
        Collection<Integer> values = scores.values();
        switch (this) {
            case STANDARD_ARRAY -> {
                if (!sorted(values).equals(sorted(STANDARD_SCORES))) {
                    throw new IllegalArgumentException("Valeurs standard : 15, 14, 13, 12, 10 et 8, une fois chacune");
                }
            }
            case POINT_BUY -> {
                int cost = 0;
                for (int score : values) cost += pointBuyCost(score);
                if (cost > POINT_BUY_BUDGET) {
                    throw new IllegalArgumentException("Achat de points : %d points dépensés, %d au plus".formatted(cost, POINT_BUY_BUDGET));
                }
            }
            case ROLLED -> {
                if (rolls == null || rolls.size() != Ability.values().length) {
                    throw new IllegalArgumentException("Dés : lancer d'abord les six jets de caractéristique");
                }
                List<Integer> totals = rolls.stream().map(AbilityRoll::total).toList();
                if (!sorted(values).equals(sorted(totals))) {
                    throw new IllegalArgumentException("Dés : les valeurs sont les totaux des jets " + sorted(totals).reversed());
                }
            }
        }
    }

    private static List<Integer> sorted(Collection<Integer> values) {
        return values.stream().sorted().toList();
    }
}
