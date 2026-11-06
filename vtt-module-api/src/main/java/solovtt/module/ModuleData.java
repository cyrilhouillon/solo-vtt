package solovtt.module;

import java.util.Optional;

/**
 * Les données qu'un module garde dans une campagne : un document JSON par campagne et par module, que le cœur range
 * sans l'interpréter. Le cœur implémente ce port ; un module l'injecte.
 */
public interface ModuleData {

    /** Vrai si une campagne porte cet id. */
    boolean exists(String campaignId);

    /**
     * Le document du module dans la campagne, vide si le module n'a encore rien écrit.
     *
     * @param moduleId l'id du module, en minuscules, chiffres et tirets
     * @throws IllegalArgumentException si aucune campagne n'a cet id, ou si l'id du module est invalide
     */
    Optional<String> read(String campaignId, String moduleId);

    /**
     * Remplace le document du module dans la campagne.
     *
     * @param moduleId l'id du module, en minuscules, chiffres et tirets
     * @throws IllegalArgumentException si aucune campagne n'a cet id, ou si l'id du module est invalide
     */
    void write(String campaignId, String moduleId, String json);
}
