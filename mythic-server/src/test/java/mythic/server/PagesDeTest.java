package mythic.server;

import solovtt.module.NotebookPages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Les racines de section des Carnets, en mémoire : la campagne, le module, le titre et le contenu de chacune. */
final class PagesDeTest implements NotebookPages {

    record Racine(String campaignId, String moduleId, String id, String title, String content) {}

    final List<Racine> racines = new ArrayList<>();

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
