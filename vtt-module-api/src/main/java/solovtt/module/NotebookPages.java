package solovtt.module;

import java.util.List;

/**
 * Les pages qu'un module range dans sa section du Carnet d'une campagne. Une racine de section est une page ordinaire
 * (onglet, verrou, édition, recherche, liens, sous-pages), à ceci près qu'elle ne se déplace ni ne se supprime ; le Carnet
 * la montre sous les pages ordinaires, dans la section du module. Le cœur implémente ce port ; un module l'injecte.
 */
public interface NotebookPages {

    /**
     * Les racines vivantes de la section du module dans la campagne.
     *
     * @param moduleId l'id du module, en minuscules, chiffres et tirets
     * @throws IllegalArgumentException si aucune campagne n'a cet id, ou si l'id du module est invalide
     */
    List<SectionPage> sectionRoots(String campaignId, String moduleId);

    /**
     * Crée une racine de la section du module, avec ce contenu (document JSON de l'éditeur ; {@code null} : vide).
     *
     * @param moduleId l'id du module, en minuscules, chiffres et tirets
     * @throws IllegalArgumentException si aucune campagne n'a cet id, ou si l'id du module est invalide
     */
    SectionPage createSectionRoot(String campaignId, String moduleId, String title, String contentJson);

    /** Une racine de section : son id de page et son titre actuel. */
    record SectionPage(String id, String title) {}
}
