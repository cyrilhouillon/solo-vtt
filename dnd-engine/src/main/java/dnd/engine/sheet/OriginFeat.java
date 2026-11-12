package dnd.engine.sheet;

/** Les dons d'origines du SRD 5.2.1 (« Origin Feats »), et ce qu'ils font, en bref. */
public enum OriginFeat {
    SKILLED("Doué", "Maîtrise de trois compétences au choix."),
    MAGIC_INITIATE("Initié à la magie",
            "Deux sorts mineurs et un sort du 1er niveau d’une liste (Clerc, Druide ou Magicien) ; le sort du 1er niveau "
                    + "se lance une fois sans emplacement par Repos long."),
    SAVAGE_ATTACKER("Sauvagerie martiale",
            "Une fois par tour, en touchant avec une arme, lancer deux fois ses dés de dégâts et garder l’un des résultats."),
    ALERT("Vigilant",
            "Bonus de maîtrise ajouté à l’Initiative ; aussitôt après l’Initiative, l’échanger avec un allié volontaire "
                    + "(ni l’un ni l’autre Neutralisé).");

    /** Le nombre de compétences de Doué. */
    public static final int SKILLED_COUNT = 3;

    private final String label;
    private final String text;

    OriginFeat(String label, String text) {
        this.label = label;
        this.text = text;
    }

    public String label() {
        return label;
    }

    /** Ce que fait le don, en une phrase. */
    public String text() {
        return text;
    }
}
