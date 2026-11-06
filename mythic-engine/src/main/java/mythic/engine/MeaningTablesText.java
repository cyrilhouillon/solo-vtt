package mythic.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lit les tables de sens d'un fichier texte à titres : {@code # Actions 1}, {@code # Actions 2},
 * {@code # Descriptions 1}, {@code # Descriptions 2}, puis zéro ou plusieurs {@code # Élément : <nom>}, chacun suivi de
 * ses 100 entrées, une par ligne. Les lignes vides sont ignorées ; un refus dit la ligne en cause.
 */
public final class MeaningTablesText {

    private static final Pattern ELEMENT = Pattern.compile("(?iu)[ée]l[ée]ment\\s*:\\s*(.*)");
    private static final List<String> COLUMNS = List.of(
            MeaningTables.ACTIONS + " 1", MeaningTables.ACTIONS + " 2",
            MeaningTables.DESCRIPTIONS + " 1", MeaningTables.DESCRIPTIONS + " 2");

    private MeaningTablesText() {
    }

    /** Une section du fichier : son nom, la ligne de son titre et ses entrées. */
    private record Section(String name, int line, boolean element, List<String> entries) {}

    /**
     * @throws IllegalArgumentException avec la raison et la ligne si le texte est mal formé
     */
    public static MeaningTables parse(String text) {
        if (text == null) text = "";
        if (text.startsWith("﻿")) text = text.substring(1);
        List<Section> sections = new ArrayList<>();
        String[] lines = text.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            int number = i + 1;
            String line = lines[i].strip();
            if (line.isEmpty()) continue;
            if (line.startsWith("#")) {
                sections.add(heading(line.substring(1).strip(), number, sections));
            } else if (sections.isEmpty()) {
                throw new IllegalArgumentException("Ligne " + number + " : texte avant le premier titre (« # Actions 1 » attendu)");
            } else {
                sections.getLast().entries().add(line);
            }
        }
        Map<String, List<String>> columns = new LinkedHashMap<>();
        Map<String, List<String>> elements = new LinkedHashMap<>();
        for (Section section : sections) {
            if (section.entries().size() != MeaningTables.SIZE)
                throw new IllegalArgumentException(section.name() + " : " + section.entries().size() + " entrées, "
                        + MeaningTables.SIZE + " attendues (ligne " + section.line() + ")");
            (section.element() ? elements : columns).put(section.name(), section.entries());
        }
        for (String column : COLUMNS)
            if (!columns.containsKey(column)) throw new IllegalArgumentException("Section « # " + column + " » absente");
        return new MeaningTables(columns.get(COLUMNS.get(0)), columns.get(COLUMNS.get(1)), columns.get(COLUMNS.get(2)),
                columns.get(COLUMNS.get(3)), elements);
    }

    private static Section heading(String title, int number, List<Section> sections) {
        Matcher element = ELEMENT.matcher(title);
        if (element.matches()) {
            String name = element.group(1).strip();
            if (name.isEmpty()) throw new IllegalArgumentException("Ligne " + number + " : nom d'élément vide");
            if (sections.stream().anyMatch(s -> s.element() && s.name().equalsIgnoreCase(name)))
                throw new IllegalArgumentException("Ligne " + number + " : élément « " + name + " » en double");
            return new Section(name, number, true, new ArrayList<>());
        }
        String normalized = title.replaceAll("\\s+", " ");
        String column = COLUMNS.stream().filter(c -> c.equalsIgnoreCase(normalized)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ligne " + number + " : titre inconnu « # " + title
                        + " » (Actions 1, Actions 2, Descriptions 1, Descriptions 2 ou Élément : <nom>)"));
        if (sections.stream().anyMatch(s -> !s.element() && s.name().equals(column)))
            throw new IllegalArgumentException("Ligne " + number + " : section « # " + column + " » en double");
        return new Section(column, number, false, new ArrayList<>());
    }
}
