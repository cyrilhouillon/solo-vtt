package solovtt.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Les liens entre pages du Carnet : un nœud {@code pageLink} de l'éditeur, {@code { type: "pageLink", attrs: { pageId,
 * label } }}. Le serveur les lit sans jamais réécrire le contenu.
 */
public final class PageLinks {

    static final String PAGE_LINK = "pageLink";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final System.Logger LOG = System.getLogger(PageLinks.class.getName());

    private PageLinks() {}

    /**
     * Les pages qui contiennent au moins un lien vers {@code id}, dans l'ordre reçu, sans la page elle-même. Une page sans
     * contenu ou au contenu illisible est sautée.
     */
    public static List<Page> citing(List<Page> pages, String id) {
        return pages.stream().filter(p -> !p.id().equals(id) && linksTo(p, id)).toList();
    }

    private static boolean linksTo(Page page, String id) {
        if (page.content() == null) return false;
        try {
            return contains(JSON.readTree(page.content()), id);
        } catch (JsonProcessingException e) {
            LOG.log(System.Logger.Level.WARNING, "Liens ignorés, contenu illisible : " + page.id(), e);
            return false;
        }
    }

    /** Parcourt tout le document : un lien peut être dans un paragraphe, une liste, une citation… */
    private static boolean contains(JsonNode node, String id) {
        if (node.isObject() && PAGE_LINK.equals(node.path("type").asText())
                && id.equals(node.path("attrs").path("pageId").asText(null))) {
            return true;
        }
        for (JsonNode child : node) {
            if (contains(child, id)) return true;
        }
        return false;
    }
}
