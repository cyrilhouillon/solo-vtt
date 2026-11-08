package mythic.server;

import solovtt.module.NotebookPages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Les racines de section des Carnets, en mémoire : la campagne, le module, le titre et le contenu de chacune ; et les
 * titres des pages ordinaires, que lisent les liens.
 */
final class PagesDeTest implements NotebookPages {

    record Racine(String campaignId, String moduleId, String id, String title, String content) {}

    final List<Racine> racines = new ArrayList<>();
    /** Les pages ordinaires vivantes, par campagne puis id : leur titre. */
    final Map<String, Map<String, String>> pagesOrdinaires = new HashMap<>();

    @Override
    public List<SectionPage> sectionRoots(String campaignId, String moduleId) {
        return racines.stream()
                .filter(r -> r.campaignId().equals(campaignId) && r.moduleId().equals(moduleId))
                .map(r -> new SectionPage(r.id(), r.title()))
                .toList();
    }

    @Override
    public SectionPage createSectionRoot(String campaignId, String moduleId, String title, String contentJson) {
        var racine = new Racine(campaignId, moduleId, UUID.randomUUID().toString(), title, contentJson);
        racines.add(racine);
        return new SectionPage(racine.id(), racine.title());
    }

    @Override
    public Optional<String> sectionRootContent(String campaignId, String moduleId, String pageId) {
        return racines.stream()
                .filter(r -> r.campaignId().equals(campaignId) && r.moduleId().equals(moduleId) && r.id().equals(pageId))
                .findFirst()
                .map(Racine::content);
    }

    @Override
    public Optional<String> pageTitle(String campaignId, String pageId) {
        String titre = titres(campaignId).get(pageId);
        if (titre != null) return Optional.of(titre);
        return Optional.ofNullable(pagesOrdinaires.getOrDefault(campaignId, Map.of()).get(pageId));
    }

    /** Remplace le contenu d'une racine, comme le joueur qui écrit dans la page. */
    void ecrire(String id, String content) {
        racines.replaceAll(r -> r.id().equals(id) ? new Racine(r.campaignId(), r.moduleId(), id, r.title(), content) : r);
    }

    /** Une page ordinaire de la campagne, que peut citer un lien. */
    void pageOrdinaire(String campaignId, String id, String title) {
        pagesOrdinaires.computeIfAbsent(campaignId, c -> new HashMap<>()).put(id, title);
    }

    /** Renomme une racine, comme le joueur depuis l'arbre. */
    void renommer(String id, String title) {
        racines.replaceAll(r -> r.id().equals(id) ? new Racine(r.campaignId(), r.moduleId(), id, title, r.content()) : r);
    }

    /** Les titres des racines d'une campagne, par id. */
    Map<String, String> titres(String campaignId) {
        Map<String, String> titres = new HashMap<>();
        racines.stream().filter(r -> r.campaignId().equals(campaignId)).forEach(r -> titres.put(r.id(), r.title()));
        return titres;
    }
}
