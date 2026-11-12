package solovtt.server;

/**
 * Une page du Carnet d'une campagne. {@code combatId} est le combat ({@link CombatStore}) dont elle est l'entrée : une
 * entrée de combat porte le nom du combat, sans contenu ni sous-page ; {@code null} pour toute autre page. {@code parentId} est la page sous laquelle elle est rangée, {@code null} à la racine.
 * {@code section} est l'id de la section dont elle est la racine : celle d'un module, ou {@code combats}, que tient le
 * cœur ; {@code null} pour toute autre page : une page est dans une section si sa racine en est une, sans le porter elle-même. Une racine de
 * section ne se déplace ni ne se supprime. {@code content} est le document de l'éditeur (JSON de Tiptap), que le serveur
 * garde sans l'interpréter ; {@code null} pour une page neuve.
 */
public record Page(String id, String title, String parentId, String section, String content, String combatId) {

    /** Une page qui n'est pas l'entrée d'un combat. */
    public Page(String id, String title, String parentId, String section, String content) {
        this(id, title, parentId, section, content, null);
    }

    /** Une page ordinaire, hors de toute section. */
    public Page(String id, String title, String parentId, String content) {
        this(id, title, parentId, null, content);
    }

    public Page withTitle(String title) {
        return new Page(id, title, parentId, section, content, combatId);
    }

    public Page withParent(String parentId) {
        return new Page(id, title, parentId, section, content, combatId);
    }

    public Page withContent(String content) {
        return new Page(id, title, parentId, section, content, combatId);
    }

    /** Vrai si la page est l'entrée d'un combat. */
    public boolean isCombatEntry() {
        return combatId != null;
    }

    /** Vrai si la page est la racine de la section d'un module. */
    public boolean isSectionRoot() {
        return section != null;
    }
}
