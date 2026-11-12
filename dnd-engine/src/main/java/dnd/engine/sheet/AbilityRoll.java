package dnd.engine.sheet;

import solovtt.engine.Dice;

import java.util.List;

/** Un jet de caractéristique : quatre d6, dont on garde les trois plus hauts (SRD 5.2.1, « Generate Your Scores »). */
public record AbilityRoll(List<Integer> dice) {

    public AbilityRoll {
        if (dice == null || dice.size() != 4) throw new IllegalArgumentException("Un jet de caractéristique a quatre d6");
        for (Integer die : dice) {
            if (die == null || die < 1 || die > 6) throw new IllegalArgumentException("Un d6 vaut de 1 à 6 : " + die);
        }
        dice = List.copyOf(dice);
    }

    /** Lance quatre d6. */
    public static AbilityRoll roll(Dice dice) {
        return new AbilityRoll(List.of(dice.roll(6), dice.roll(6), dice.roll(6), dice.roll(6)));
    }

    /** La somme des trois plus hauts dés. */
    public int total() {
        return dice.stream().mapToInt(Integer::intValue).sum() - dice.stream().mapToInt(Integer::intValue).min().orElseThrow();
    }
}
