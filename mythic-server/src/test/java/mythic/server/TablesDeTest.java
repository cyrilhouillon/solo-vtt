package mythic.server;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Un fichier de tables de sens aux mots inventés (`action1-001`…) : aucun mot du livre n'entre dans le dépôt. */
final class TablesDeTest {

    private TablesDeTest() {
    }

    static String section(String titre, String prefixe) {
        return "# " + titre + "\n" + IntStream.rangeClosed(1, 100).mapToObj(i -> "%s-%03d".formatted(prefixe, i))
                .collect(Collectors.joining("\n")) + "\n";
    }

    static String texte() {
        return section("Actions 1", "action1") + section("Actions 2", "action2")
                + section("Descriptions 1", "description1") + section("Descriptions 2", "description2")
                + section("Élément : Lieux", "lieu") + section("Élément : Ton de l'aventure", "ton");
    }
}
