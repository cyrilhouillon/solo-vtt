package solovtt.module;

import java.util.List;
import java.util.Optional;

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

    /**
     * Le contenu (document JSON de l'éditeur) d'une racine vivante de la section du module ; vide pour toute autre page,
     * une page à la corbeille ou une racine sans contenu.
     *
     * @param moduleId l'id du module, en minuscules, chiffres et tirets
     * @throws IllegalArgumentException si aucune campagne n'a cet id, ou si l'id du module est invalide
     */
    Optional<String> sectionRootContent(String campaignId, String moduleId, String pageId);

    /**
     * Le titre actuel d'une page vivante du Carnet, de section ou non ; vide pour une page inconnue ou à la corbeille. Sert
     * à lire un lien de page, qui ne garde que l'id de sa cible.
     *
     * @throws IllegalArgumentException si aucune campagne n'a cet id
     */
    Optional<String> pageTitle(String campaignId, String pageId);

    /**
     * Les sous-pages directes et vivantes d'une page de la section du module (racine ou descendante), triées par titre ;
     * aucune si la page n'est pas une page vivante de cette section.
     *
     * @param moduleId l'id du module, en minuscules, chiffres et tirets
     * @throws IllegalArgumentException si aucune campagne n'a cet id, ou si l'id du module est invalide
     */
    List<SectionPage> childPages(String campaignId, String moduleId, String pageId);

    /**
     * Le contenu (document JSON de l'éditeur) d'une page vivante de la section du module, racine ou descendante ; vide pour
     * toute autre page, une page à la corbeille ou une page sans contenu.
     *
     * @param moduleId l'id du module, en minuscules, chiffres et tirets
     * @throws IllegalArgumentException si aucune campagne n'a cet id, ou si l'id du module est invalide
     */
    Optional<String> sectionPageContent(String campaignId, String moduleId, String pageId);

    /** Une page de section : son id et son titre actuel. */
    record SectionPage(String id, String title) {}
}
