package mythic.engine;

import java.util.List;
import java.util.stream.IntStream;

/** Des tables de sens de mots inventés (`action1-001`…) : aucun mot du livre n'entre dans le dépôt. */
final class TablesDeTest {

    private TablesDeTest() {
    }

    static List<String> colonne(String prefixe) {
        return IntStream.rangeClosed(1, MeaningTables.SIZE).mapToObj(i -> "%s-%03d".formatted(prefixe, i)).toList();
    }

    static String section(String titre, String prefixe) {
        return "# " + titre + "\n" + String.join("\n", colonne(prefixe)) + "\n";
    }

    /** Les quatre colonnes obligatoires, puis deux tables d'éléments. */
    static String texte() {
        return section("Actions 1", "action1") + section("Actions 2", "action2")
                + section("Descriptions 1", "description1") + section("Descriptions 2", "description2")
                + section("Élément : Ton de l'aventure", "ton") + section("Élément : Lieux", "lieu");
    }

    static MeaningTables tables() {
        return MeaningTablesText.parse(texte());
    }
}
