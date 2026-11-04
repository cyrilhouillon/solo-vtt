package solovtt.server;

/** Une campagne : une histoire du joueur, avec sa propre partie et ses propres cartes enregistrées. Elle n'a qu'un nom. */
public record Campaign(String id, String name) {}
