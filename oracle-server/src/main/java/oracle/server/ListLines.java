package oracle.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Les lignes d'une liste de l'oracle (une sous-page de Listes), lues dans son document Tiptap : chaque élément de
 * premier niveau d'une liste de premier niveau est une ligne, dans l'ordre du document ; son contenu est son premier
 * paragraphe. Une sous-liste dans un élément est une note, le texte hors des listes est ignoré. Un élément sans texte visible ni lien est
 * une ligne vide, que le tirage saute. Copie de la lecture des listes de Mythic : un module ne dépend pas d'un autre.
 */
final class ListLines {

    static final String PAGE_LINK = "pageLink";
    static final String MISSING_PAGE = "page introuvable";

    private static final Set<String> LISTS = Set.of("bulletList", "orderedList");
    private static final ObjectMapper JSON = new ObjectMapper();

    private ListLines() {
    }

    /**
     * Une ligne : ses nœuds en ligne, tels qu'écrits (texte avec marques, liens de page), et le texte qu'on y lit.
     *
     * @param written une entrée y est écrite
     */
    record Line(List<JsonNode> inline, String text, boolean written) {}

    /**
     * Les lignes du document ; aucune s'il est illisible.
     *
     * @param titleOf le titre actuel d'une page, pour un lien sans texte choisi
     */
    static List<Line> of(String contentJson, Function<String, Optional<String>> titleOf) {
        if (contentJson == null) return List.of();
        JsonNode doc;
        try {
            doc = JSON.readTree(contentJson);
        } catch (JsonProcessingException e) {
            return List.of();
        }
        List<Line> lines = new ArrayList<>();
        for (JsonNode block : doc.path("content")) {
            if (!LISTS.contains(block.path("type").asText())) continue;
            for (JsonNode item : block.path("content")) lines.add(line(item, titleOf));
        }
        return lines;
    }

    private static Line line(JsonNode item, Function<String, Optional<String>> titleOf) {
        JsonNode first = item.path("content").path(0);
        if (!"paragraph".equals(first.path("type").asText())) return new Line(List.of(), "", false);
        List<JsonNode> inline = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        boolean link = false;
        for (JsonNode node : first.path("content")) {
            inline.add(node);
            switch (node.path("type").asText()) {
                case "text" -> text.append(node.path("text").asText(""));
                case PAGE_LINK -> {
                    link = true;
                    JsonNode label = node.path("attrs").path("label");
                    text.append(label.isTextual() ? label.asText()
                            : titleOf.apply(node.path("attrs").path("pageId").asText("")).orElse(MISSING_PAGE));
                }
                default -> text.append(' ');
            }
        }
        String shown = text.toString().replaceAll("\\s+", " ").strip();
        return new Line(List.copyOf(inline), shown, link || !shown.isEmpty());
    }
}
