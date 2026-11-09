package oracle.server;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/** Les corps de requête et de réponse de l'oracle générique. */
final class OracleDtos {

    private OracleDtos() {
    }

    /** Une question facultative ; {@code null} ou vide : sans énoncé. */
    record QuestionRequest(String question) {}

    /** Une réponse oui/non à 50/50 : oui de 1 à 50 sur le d100. */
    record YesNoDto(String question, int roll, boolean yes) {}

    /** Un d100, de 1 à 100. */
    record D100Dto(String question, int roll) {}

    /** La page racine des listes, dans la section de l'oracle. */
    record ListsRootDto(String listsPageId) {}

    /** Une liste : une sous-page de Listes. */
    record ListDto(String id, String title) {}

    /**
     * Une ligne tirée dans une liste : le dé ({@code roll} sur {@code sides}, le nombre de lignes écrites), le numéro de la
     * ligne dans la liste, l'entrée en nœuds en ligne tels qu'écrits (un lien de page reste un lien) et en texte.
     */
    record DrawDto(String listId, String list, int roll, int sides, int line, List<JsonNode> entry, String entryText) {}
}
