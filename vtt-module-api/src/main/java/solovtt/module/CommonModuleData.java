package solovtt.module;

import java.util.Optional;

/**
 * Les données qu'un module garde pour le joueur, communes à toutes ses campagnes : un document JSON par module, que le
 * cœur range sans l'interpréter. Le cœur implémente ce port ; un module l'injecte.
 */
public interface CommonModuleData {

    /**
     * Le document commun du module, vide si le module n'a encore rien écrit.
     *
     * @param moduleId l'id du module, en minuscules, chiffres et tirets
     * @throws IllegalArgumentException si l'id du module est invalide
     */
    Optional<String> read(String moduleId);

    /**
     * Remplace le document commun du module.
     *
     * @param moduleId l'id du module, en minuscules, chiffres et tirets
     * @throws IllegalArgumentException si l'id du module est invalide
     */
    void write(String moduleId, String json);
}
