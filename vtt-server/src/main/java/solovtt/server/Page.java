package solovtt.server;

/**
 * Une page du Carnet d'une campagne. {@code parentId} est la page sous laquelle elle est rangée, {@code null} à la racine.
 * {@code section} est l'id du module dont elle est une racine de section (Mythic range ses listes ainsi), {@code null}
 * pour toute autre page : une page est dans une section si sa racine en est une, sans le porter elle-même. Une racine de
 * section ne se déplace ni ne se supprime. {@code content} est le document de l'éditeur (JSON de Tiptap), que le serveur
 * garde sans l'interpréter ; {@code null} pour une page neuve.
 */
public record Page(String id, String title, String parentId, String section, String content) {

    /** Une page ordinaire, hors de toute section. */
    public Page(String id, String title, String parentId, String content) {
        this(id, title, parentId, null, content);
    }

    public Page withTitle(String title) {
        return new Page(id, title, parentId, section, content);
    }

    public Page withParent(String parentId) {
        return new Page(id, title, parentId, section, content);
    }

    public Page withContent(String content) {
        return new Page(id, title, parentId, section, content);
    }

    /** Vrai si la page est la racine de la section d'un module. */
    public boolean isSectionRoot() {
        return section != null;
    }
}
