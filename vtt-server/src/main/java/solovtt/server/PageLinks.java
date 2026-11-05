package solovtt.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

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
        return citingAny(pages, Set.of(id));
    }

    /**
     * Les pages hors de {@code ids} qui contiennent au moins un lien vers l'une d'elles, dans l'ordre reçu : celles dont
     * les liens mourraient si toutes ces pages disparaissaient.
     */
    public static List<Page> citingAny(List<Page> pages, Set<String> ids) {
        return pages.stream().filter(p -> !ids.contains(p.id()) && linksTo(p, ids)).toList();
    }

    private static boolean linksTo(Page page, Set<String> ids) {
        if (page.content() == null) return false;
        try {
            return contains(JSON.readTree(page.content()), ids);
        } catch (JsonProcessingException e) {
            LOG.log(System.Logger.Level.WARNING, "Liens ignorés, contenu illisible : " + page.id(), e);
            return false;
        }
    }

    /** Parcourt tout le document : un lien peut être dans un paragraphe, une liste, une citation… */
    private static boolean contains(JsonNode node, Set<String> ids) {
        if (node.isObject() && PAGE_LINK.equals(node.path("type").asText())
                && ids.contains(node.path("attrs").path("pageId").asText(""))) {
            return true;
        }
        for (JsonNode child : node) {
            if (contains(child, ids)) return true;
        }
        return false;
    }
}
