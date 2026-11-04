package solovtt.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** L'arbre du Carnet, qui n'est rien d'autre que le parent de chaque page. */
public final class PageTree {

    private PageTree() {}

    /**
     * Vrai si {@code id} est {@code ancestorId} ou l'une de ses sous-pages, à toute profondeur. La remontée s'arrête à la
     * racine, à un parent inconnu ou après autant de pas que de pages : une boucle déjà sur le disque ne la bloque pas.
     */
    public static boolean isWithin(List<Page> pages, String id, String ancestorId) {
        Map<String, String> parents = parents(pages);
        String current = id;
        for (int steps = 0; current != null && steps <= parents.size(); steps++) {
            if (current.equals(ancestorId)) return true;
            current = parents.get(current);
        }
        return false;
    }

    /**
     * Les pages, à la racine celles dont le parent est inconnu (fiche illisible) et celles qui bouclent sur elles-mêmes
     * (fiches éditées à la main) : aucune page ne disparaît de l'arbre, et les autres gardent leur parent.
     */
    public static List<Page> rooted(List<Page> pages) {
        Map<String, String> parents = parents(pages);
        return pages.stream().map(page -> isAttached(parents, page) ? page
                : new Page(page.id(), page.title(), null, page.content())).toList();
    }

    /** Un parent connu, sans boucle qui revienne à la page : une boucle plus haut se résout sur ses propres pages. */
    private static boolean isAttached(Map<String, String> parents, Page page) {
        if (page.parentId() == null) return true;
        if (!parents.containsKey(page.parentId())) return false;
        String current = page.parentId();
        for (int steps = 0; current != null && steps <= parents.size(); steps++) {
            if (current.equals(page.id())) return false;
            current = parents.get(current);
        }
        return true;
    }

    private static Map<String, String> parents(List<Page> pages) {
        Map<String, String> parents = new HashMap<>();
        pages.forEach(p -> parents.put(Objects.requireNonNull(p.id()), p.parentId()));
        return parents;
    }
}
