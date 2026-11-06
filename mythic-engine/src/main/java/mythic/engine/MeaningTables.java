package mythic.engine;

import solovtt.engine.Dice;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Les tables de sens de Mythic GME 2e, fournies par le joueur : Actions et Descriptions en deux colonnes, dont chacune
 * donne un mot, et les tables d'éléments, une colonne tirée deux fois. Chaque colonne a 100 entrées, une par d100.
 */
public record MeaningTables(List<String> actions1, List<String> actions2, List<String> descriptions1,
                            List<String> descriptions2, Map<String, List<String>> elements) {

    /** Le nombre d'entrées d'une colonne : une par face du d100. */
    public static final int SIZE = 100;
    public static final String ACTIONS = "Actions";
    public static final String DESCRIPTIONS = "Descriptions";

    public MeaningTables {
        actions1 = column(ACTIONS + " 1", actions1);
        actions2 = column(ACTIONS + " 2", actions2);
        descriptions1 = column(DESCRIPTIONS + " 1", descriptions1);
        descriptions2 = column(DESCRIPTIONS + " 2", descriptions2);
        if (elements == null) elements = Map.of();
        Map<String, List<String>> copy = new LinkedHashMap<>();
        Set<String> names = new HashSet<>();
        for (var element : elements.entrySet()) {
            String name = element.getKey() == null ? "" : element.getKey().strip();
            if (name.isEmpty()) throw new IllegalArgumentException("Nom d'élément vide");
            if (!names.add(name.toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("Élément « " + name + " » en double");
            copy.put(name, column(name, element.getValue()));
        }
        elements = Collections.unmodifiableMap(copy);
    }

    /** Deux mots d'action, un par colonne. */
    public Meaning actions(Dice dice) {
        return pair(ACTIONS, actions1, actions2, dice);
    }

    /** Deux mots de description, un par colonne. */
    public Meaning descriptions(Dice dice) {
        return pair(DESCRIPTIONS, descriptions1, descriptions2, dice);
    }

    /**
     * Deux mots de la table d'éléments de ce nom, tirée deux fois.
     *
     * @throws IllegalArgumentException si aucune table d'éléments n'a ce nom
     */
    public Meaning element(String name, Dice dice) {
        List<String> column = elements.get(name);
        if (column == null) throw new IllegalArgumentException("Table d'éléments inconnue : " + name);
        return pair(name, column, column, dice);
    }

    /** Les noms des tables d'éléments, dans l'ordre du fichier. */
    public List<String> elementNames() {
        return List.copyOf(elements.keySet());
    }

    private static Meaning pair(String table, List<String> first, List<String> second, Dice dice) {
        int firstRoll = dice.roll(SIZE);
        int secondRoll = dice.roll(SIZE);
        return new Meaning(table, first.get(firstRoll - 1), firstRoll, second.get(secondRoll - 1), secondRoll);
    }

    private static List<String> column(String name, List<String> entries) {
        if (entries == null) throw new IllegalArgumentException(name + " : table absente");
        if (entries.size() != SIZE)
            throw new IllegalArgumentException(name + " : " + entries.size() + " entrées, " + SIZE + " attendues");
        List<String> stripped = entries.stream().map(e -> e == null ? "" : e.strip()).toList();
        for (int i = 0; i < SIZE; i++)
            if (stripped.get(i).isEmpty()) throw new IllegalArgumentException(name + " : entrée " + (i + 1) + " vide");
        return stripped;
    }
}
