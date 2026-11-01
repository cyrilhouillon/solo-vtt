package solovtt.server;

import solovtt.engine.Decision;

/** Une action refusée parce qu'une décision attend une réponse ; rendue en 409 avec la raison. */
public class DecisionPendingException extends IllegalStateException {

    public DecisionPendingException(Decision pending) {
        super("Une décision attend une réponse : « " + pending.prompt() + " »");
    }
}
