package solovtt.server;

/**
 * Une page du Carnet d'une campagne. {@code content} est le document de l'éditeur (JSON de Tiptap), que le serveur garde
 * sans l'interpréter ; {@code null} pour une page neuve.
 */
public record Page(String id, String title, String content) {}
