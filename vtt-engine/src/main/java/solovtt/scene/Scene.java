package solovtt.scene;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Une carte et les pions qu'elle porte. Elle ne connaît que des distances réelles,
 * dans son unité : chaque système de jeu décide de leur usage (portée, déplacement…).
 */
public final class Scene {

    private final double width;
    private final double height;
    private final DistanceUnit unit;
    private final Map<String, Token> tokens = new LinkedHashMap<>();

    public Scene(double width, double height, DistanceUnit unit) {
        if (!(width > 0) || !(height > 0)) {
            throw new IllegalArgumentException("Les dimensions de la carte doivent être positives : %s × %s".formatted(width, height));
        }
        this.width = width;
        this.height = height;
        this.unit = Objects.requireNonNull(unit, "unit");
    }

    public double width() { return width; }
    public double height() { return height; }
    public DistanceUnit unit() { return unit; }

    public Collection<Token> tokens() {
        return Collections.unmodifiableCollection(tokens.values());
    }

    public boolean contains(String tokenId) {
        return tokens.containsKey(tokenId);
    }

    public Token token(String tokenId) {
        Token token = tokens.get(tokenId);
        if (token == null) throw new IllegalArgumentException("Jeton inconnu : " + tokenId);
        return token;
    }

    public void place(Token token) {
        if (tokens.containsKey(token.id())) {
            throw new IllegalArgumentException("Un jeton porte déjà l'id " + token.id());
        }
        requireInside(token.position());
        tokens.put(token.id(), token);
    }

    /**
     * Déplace un jeton, sans règle : la distance permise est l'affaire du système de jeu.
     *
     * @throws IllegalArgumentException jeton inconnu ou position hors de la carte ; le jeton ne bouge pas
     */
    public Token move(String tokenId, Position position) {
        Token moved = new Token(tokenId, token(tokenId).entityId(), position);
        requireInside(position);
        tokens.put(tokenId, moved);
        return moved;
    }

    private void requireInside(Position p) {
        if (p.x() < 0 || p.y() < 0 || p.x() > width || p.y() > height) {
            throw new IllegalArgumentException("Position hors de la carte : (%s, %s) pour une carte de %s × %s %s"
                    .formatted(p.x(), p.y(), width, height, unit.symbol()));
        }
    }

    /**
     * La même carte à une autre échelle : dimensions et positions multipliées par {@code factor}, jetons gardés dans
     * leur ordre. Chaque jeton reste au même endroit relatif de la carte ; cette scène ne change pas.
     *
     * @throws IllegalArgumentException facteur nul, négatif ou non fini
     */
    public Scene rescaled(double factor) {
        if (!(factor > 0) || Double.isInfinite(factor)) {
            throw new IllegalArgumentException("Le facteur d'échelle doit être positif : " + factor);
        }
        Scene scaled = new Scene(width * factor, height * factor, unit);
        for (Token t : tokens.values()) {
            scaled.place(new Token(t.id(), t.entityId(),
                    new Position(Math.min(t.position().x() * factor, scaled.width), Math.min(t.position().y() * factor, scaled.height))));
        }
        return scaled;
    }

    /** Retire un jeton ; un id inconnu est refusé. */
    public Token remove(String tokenId) {
        Token removed = tokens.remove(tokenId);
        if (removed == null) throw new IllegalArgumentException("Jeton inconnu : " + tokenId);
        return removed;
    }

    /** Distance euclidienne de centre à centre, dans l'unité de la carte. */
    public double distance(String tokenIdA, String tokenIdB) {
        return token(tokenIdA).position().distanceTo(token(tokenIdB).position());
    }
}
