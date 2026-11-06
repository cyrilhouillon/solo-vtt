package mythic.engine;

import java.util.EnumMap;
import java.util.Map;

/**
 * La table de destin de Mythic GME 2e : pour chaque probabilité et chaque facteur de chaos, trois seuils de d100. Un jet
 * inférieur ou égal au premier donne oui exceptionnel, au deuxième oui ; supérieur ou égal au troisième, non
 * exceptionnel ; sinon non. Un côté sans résultat exceptionnel (« X » du livre) a pour seuil {@link #NONE_YES} ou
 * {@link #NONE_NO}, qu'aucun jet n'atteint.
 */
public final class FateChart {

    /** Aucun jet de 1 à 100 n'est inférieur ou égal à 0 : pas de oui exceptionnel. */
    public static final int NONE_YES = 0;
    /** Aucun jet de 1 à 100 n'atteint 101 : pas de non exceptionnel. */
    public static final int NONE_NO = 101;

    private static final int X = -1;

    /** Une case : les trois seuils. */
    public record Cell(int exceptionalYes, int yes, int exceptionalNo) {

        public Answer answer(int roll) {
            if (roll < 1 || roll > 100) throw new IllegalArgumentException("Un d100 va de 1 à 100 : " + roll);
            if (roll <= exceptionalYes) return Answer.EXCEPTIONAL_YES;
            if (roll <= yes) return Answer.YES;
            if (roll >= exceptionalNo) return Answer.EXCEPTIONAL_NO;
            return Answer.NO;
        }
    }

    /** Ligne par probabilité, chaos 1 à 9 de gauche à droite : seuils exceptionnel oui, oui, exceptionnel non. */
    private static final Map<Odds, int[][]> ROWS = new EnumMap<>(Odds.class);

    static {
        ROWS.put(Odds.CERTAIN, new int[][]{
                {10, 50, 91}, {13, 65, 94}, {15, 75, 96}, {17, 85, 98}, {18, 90, 99}, {19, 95, 100}, {20, 99, X}, {20, 99, X}, {20, 99, X}});
        ROWS.put(Odds.NEARLY_CERTAIN, new int[][]{
                {7, 35, 88}, {10, 50, 91}, {13, 65, 94}, {15, 75, 96}, {17, 85, 98}, {18, 90, 99}, {19, 95, 100}, {20, 99, X}, {20, 99, X}});
        ROWS.put(Odds.VERY_LIKELY, new int[][]{
                {5, 25, 86}, {7, 35, 88}, {10, 50, 91}, {13, 65, 94}, {15, 75, 96}, {17, 85, 98}, {18, 90, 99}, {19, 95, 100}, {20, 99, X}});
        ROWS.put(Odds.LIKELY, new int[][]{
                {3, 15, 84}, {5, 25, 86}, {7, 35, 88}, {10, 50, 91}, {13, 65, 94}, {15, 75, 96}, {17, 85, 98}, {18, 90, 99}, {19, 95, 100}});
        ROWS.put(Odds.FIFTY_FIFTY, new int[][]{
                {2, 10, 83}, {3, 15, 84}, {5, 25, 86}, {7, 35, 88}, {10, 50, 91}, {13, 65, 94}, {15, 75, 96}, {17, 85, 98}, {18, 90, 99}});
        ROWS.put(Odds.UNLIKELY, new int[][]{
                {1, 5, 82}, {2, 10, 83}, {3, 15, 84}, {5, 25, 86}, {7, 35, 88}, {10, 50, 91}, {13, 65, 94}, {15, 75, 96}, {17, 85, 98}});
        ROWS.put(Odds.VERY_UNLIKELY, new int[][]{
                {X, 1, 81}, {1, 5, 82}, {2, 10, 83}, {3, 15, 84}, {5, 25, 86}, {7, 35, 88}, {10, 50, 91}, {13, 65, 94}, {15, 75, 96}});
        ROWS.put(Odds.NEARLY_IMPOSSIBLE, new int[][]{
                {X, 1, 81}, {X, 1, 81}, {1, 5, 82}, {2, 10, 83}, {3, 15, 84}, {5, 25, 86}, {7, 35, 88}, {10, 50, 91}, {13, 65, 94}});
        ROWS.put(Odds.IMPOSSIBLE, new int[][]{
                {X, 1, 81}, {X, 1, 81}, {X, 1, 81}, {1, 5, 82}, {2, 10, 83}, {3, 15, 84}, {5, 25, 86}, {7, 35, 88}, {10, 50, 91}});
    }

    private FateChart() {
    }

    public static Cell cell(Odds odds, ChaosFactor chaos) {
        int[] row = ROWS.get(odds)[chaos.value() - 1];
        return new Cell(row[0] == X ? NONE_YES : row[0], row[1], row[2] == X ? NONE_NO : row[2]);
    }
}
