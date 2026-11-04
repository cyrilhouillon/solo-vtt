package solovtt.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import solovtt.server.NotebookStore.Fragment;
import solovtt.server.NotebookStore.SearchHit;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La recherche par défaut du Carnet : relit le titre et le texte de toutes les pages à chaque appel, sans index. Un
 * adaptateur de {@link NotebookStore} peut la remplacer sans que rien d'autre ne change.
 */
public final class PageSearch {

    /** Caractères gardés avant le premier mot trouvé, et après. */
    static final int BEFORE = 40;
    static final int AFTER = 80;
    /** Longueur du début de texte montré quand seul le titre contient les mots. */
    static final int START = 120;
    static final String ELLIPSIS = "…";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final System.Logger LOG = System.getLogger(PageSearch.class.getName());

    private PageSearch() {}

    /**
     * Les pages dont le titre ou le texte contient chaque mot de la requête, sans casse ni accents : celles dont le titre
     * contient un mot d'abord, puis les autres, chaque groupe dans l'ordre reçu.
     */
    public static List<SearchHit> search(List<Page> pages, String query) {
        List<String> words = words(query);
        if (words.isEmpty()) return List.of();
        Map<String, String> titles = pages.stream().collect(Collectors.toMap(Page::id, Page::title, (a, b) -> a));
        List<SearchHit> byTitle = new ArrayList<>();
        List<SearchHit> byText = new ArrayList<>();
        for (Page page : pages) {
            String foldedTitle = fold(page.title());
            String text = text(page, titles::get);
            String foldedText = fold(text);
            if (!words.stream().allMatch(w -> foldedTitle.contains(w) || foldedText.contains(w))) continue;
            var hit = new SearchHit(page, excerpt(text, foldedText, words));
            (words.stream().anyMatch(foldedTitle::contains) ? byTitle : byText).add(hit);
        }
        byTitle.addAll(byText);
        return byTitle;
    }

    /** Les mots de la requête, pliés, sans doublon. */
    static List<String> words(String query) {
        if (query == null) return List.of();
        return Arrays.stream(fold(query).trim().split("\\s+")).filter(w -> !w.isEmpty()).distinct().toList();
    }

    /**
     * Sans casse ni accents, caractère par caractère : le texte plié garde la longueur de l'original, si bien qu'une
     * position trouvée dans l'un vaut dans l'autre. Un caractère qui ne se plie pas en un seul reste tel quel, en
     * minuscule (« Œ » donne « œ »).
     */
    static String fold(String text) {
        StringBuilder folded = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String f = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                    .toLowerCase(Locale.ROOT);
            folded.append(f.length() == 1 ? f.charAt(0) : Character.toLowerCase(c));
        }
        return folded.toString();
    }

    /**
     * Le texte que lit le joueur : les nœuds de texte et, pour un lien, son texte choisi, sinon le titre de sa cible ; un
     * espace entre deux blocs. Vide pour une page sans contenu ou au contenu illisible.
     */
    static String text(Page page, Function<String, String> titleOf) {
        if (page.content() == null) return "";
        try {
            StringBuilder text = new StringBuilder();
            append(JSON.readTree(page.content()), titleOf, text);
            return text.toString().replaceAll("\\s+", " ").strip();
        } catch (JsonProcessingException e) {
            LOG.log(System.Logger.Level.WARNING, "Texte ignoré par la recherche, contenu illisible : " + page.id(), e);
            return "";
        }
    }

    private static void append(JsonNode node, Function<String, String> titleOf, StringBuilder text) {
        String type = node.path("type").asText();
        switch (type) {
            case "text" -> text.append(node.path("text").asText(""));
            case PageLinks.PAGE_LINK -> {
                JsonNode label = node.path("attrs").path("label");
                String shown = label.isTextual() ? label.asText() : titleOf.apply(node.path("attrs").path("pageId").asText(""));
                if (shown != null) text.append(shown);
            }
            case "hardBreak" -> text.append(' ');
            default -> {
                // Un bloc (paragraphe, titre, liste…) : séparé de ses voisins par un espace.
                text.append(' ');
                for (JsonNode child : node.path("content")) append(child, titleOf, text);
                text.append(' ');
            }
        }
    }

    /** Autour du premier mot trouvé dans le texte, sinon son début ; les mots trouvés y sont marqués. */
    static List<Fragment> excerpt(String text, String folded, List<String> words) {
        if (text.isEmpty()) return List.of();
        int first = -1;
        int firstLength = 0;
        for (String word : words) {
            int at = folded.indexOf(word);
            if (at >= 0 && (first < 0 || at < first)) {
                first = at;
                firstLength = word.length();
            }
        }
        int start;
        int end;
        if (first < 0) {
            start = 0;
            end = cutEnd(text, 0, Math.min(text.length(), START));
        } else {
            start = cutStart(text, Math.max(0, first - BEFORE), first);
            end = cutEnd(text, first + firstLength, Math.min(text.length(), first + firstLength + AFTER));
        }
        List<Fragment> fragments = new ArrayList<>();
        if (start > 0) fragments.add(new Fragment(ELLIPSIS, false));
        mark(text, folded, words, start, end, fragments);
        if (end < text.length()) fragments.add(new Fragment(ELLIPSIS, false));
        return fragments;
    }

    /** Commence après un espace, sans dépasser le mot trouvé. */
    private static int cutStart(String text, int start, int limit) {
        if (start == 0) return 0;
        int space = text.indexOf(' ', start);
        return space >= 0 && space < limit ? space + 1 : start;
    }

    /** Finit avant un espace, sans revenir avant {@code limit}. */
    private static int cutEnd(String text, int limit, int end) {
        if (end >= text.length()) return text.length();
        int space = text.lastIndexOf(' ', end);
        return space > limit ? space : end;
    }

    /** Découpe {@code [start, end)} en morceaux, chaque occurrence d'un mot marquée ; le plus long l'emporte à égalité. */
    private static void mark(String text, String folded, List<String> words, int start, int end, List<Fragment> fragments) {
        int plain = start;
        int i = start;
        while (i < end) {
            int length = 0;
            for (String word : words) {
                if (word.length() > length && i + word.length() <= end && folded.startsWith(word, i)) length = word.length();
            }
            if (length == 0) {
                i++;
                continue;
            }
            if (plain < i) fragments.add(new Fragment(text.substring(plain, i), false));
            fragments.add(new Fragment(text.substring(i, i + length), true));
            i += length;
            plain = i;
        }
        if (plain < end) fragments.add(new Fragment(text.substring(plain, end), false));
    }
}
