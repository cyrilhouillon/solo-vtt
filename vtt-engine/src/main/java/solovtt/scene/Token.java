package solovtt.scene;

import java.util.Objects;

/**
 * Pion posé sur la carte. {@code entityId} désigne l'entité du système de jeu qu'il représente
 * (une créature D&D, par exemple) ; la scène ne sait rien de cette entité.
 */
public record Token(String id, String entityId, Position position) {

    public Token {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(position, "position");
    }
}
