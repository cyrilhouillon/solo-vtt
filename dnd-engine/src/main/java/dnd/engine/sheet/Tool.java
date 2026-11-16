package dnd.engine.sheet;

/**
 * Les outils du SRD 5.2.1 (« Tools ») dont un personnage peut avoir la maîtrise : outils d'artisan, instruments de
 * musique (une variante chacun) et autres outils. La boîte de jeux, qui se choisit par variante, reste {@link GamingSet}.
 */
public enum Tool {
    ALCHEMISTS_SUPPLIES("matériel d’alchimiste", Category.ARTISAN),
    BREWERS_SUPPLIES("matériel de brasseur", Category.ARTISAN),
    CALLIGRAPHERS_SUPPLIES("matériel de calligraphe", Category.ARTISAN),
    PAINTERS_SUPPLIES("matériel de peintre", Category.ARTISAN),
    TINKERS_TOOLS("outils de bricoleur", Category.ARTISAN),
    CARTOGRAPHERS_TOOLS("outils de cartographe", Category.ARTISAN),
    CARPENTERS_TOOLS("outils de charpentier", Category.ARTISAN),
    COBBLERS_TOOLS("outils de cordonnier", Category.ARTISAN),
    SMITHS_TOOLS("outils de forgeron", Category.ARTISAN),
    JEWELERS_TOOLS("outils de joaillier", Category.ARTISAN),
    MASONS_TOOLS("outils de maçon", Category.ARTISAN),
    WOODCARVERS_TOOLS("outils de menuisier", Category.ARTISAN),
    POTTERS_TOOLS("outils de potier", Category.ARTISAN),
    GLASSBLOWERS_TOOLS("outils de souffleur de verre", Category.ARTISAN),
    LEATHERWORKERS_TOOLS("outils de tanneur", Category.ARTISAN),
    WEAVERS_TOOLS("outils de tisserand", Category.ARTISAN),
    COOKS_UTENSILS("ustensiles de cuisinier", Category.ARTISAN),
    SHAWM("chalemie", Category.MUSICAL_INSTRUMENT),
    HORN("cor", Category.MUSICAL_INSTRUMENT),
    BAGPIPES("cornemuse", Category.MUSICAL_INSTRUMENT),
    FLUTE("flûte", Category.MUSICAL_INSTRUMENT),
    PAN_FLUTE("flûte de pan", Category.MUSICAL_INSTRUMENT),
    LUTE("luth", Category.MUSICAL_INSTRUMENT),
    LYRE("lyre", Category.MUSICAL_INSTRUMENT),
    DRUM("tambour", Category.MUSICAL_INSTRUMENT),
    DULCIMER("tympanon", Category.MUSICAL_INSTRUMENT),
    VIOL("viole", Category.MUSICAL_INSTRUMENT),
    DISGUISE_KIT("accessoires de déguisement", Category.OTHER),
    NAVIGATORS_TOOLS("instruments de navigateur", Category.OTHER),
    FORGERY_KIT("matériel de contrefaçon", Category.OTHER),
    POISONERS_KIT("matériel d’empoisonneur", Category.OTHER),
    HERBALISM_KIT("matériel d’herboriste", Category.OTHER),
    THIEVES_TOOLS("outils de voleur", Category.OTHER);

    /** Outils d'artisan, instruments de musique, autres outils. */
    public enum Category {
        ARTISAN("Outils d’artisan"),
        MUSICAL_INSTRUMENT("Instruments de musique"),
        OTHER("Autres outils");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final String label;
    private final Category category;

    Tool(String label, Category category) {
        this.label = label;
        this.category = category;
    }

    /** Le nom tel qu'il s'écrit dans une liste de la fiche, en minuscules. */
    public String label() {
        return label;
    }

    public Category category() {
        return category;
    }
}
