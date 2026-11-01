package solovtt.server;

/** Une action que l'état de la partie ne permet pas (combat déjà lancé, aucun combat…) ; rendue en 409 avec la raison. */
public class ActionRefusedException extends RuntimeException {

    public ActionRefusedException(String reason) {
        super(reason);
    }
}
