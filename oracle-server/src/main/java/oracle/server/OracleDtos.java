package oracle.server;

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
}
