package solovtt.engine;

/** Ce que l'appelant (UI, IA, réseau) reçoit quand le moteur rend la main. */
public sealed interface StepResult {

    /** Plus rien à résoudre. */
    record Idle() implements StepResult {
    }

    /** Le moteur attend la réponse à cette décision. */
    record AwaitingDecision(Decision decision) implements StepResult {
    }
}
