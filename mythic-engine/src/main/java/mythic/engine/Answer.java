package mythic.engine;

/** La réponse de l'oracle à une question oui/non. */
public enum Answer {
    EXCEPTIONAL_YES("Oui exceptionnel"),
    YES("Oui"),
    NO("Non"),
    EXCEPTIONAL_NO("Non exceptionnel");

    private final String label;

    Answer(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
