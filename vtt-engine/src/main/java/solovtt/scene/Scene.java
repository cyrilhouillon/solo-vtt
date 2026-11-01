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
        Position p = token.position();
        if (p.x() < 0 || p.y() < 0 || p.x() > width || p.y() > height) {
            throw new IllegalArgumentException("Position hors de la carte : (%s, %s) pour une carte de %s × %s %s"
                    .formatted(p.x(), p.y(), width, height, unit.symbol()));
        }
        tokens.put(token.id(), token);
    }

    /** Distance euclidienne de centre à centre, dans l'unité de la carte. */
    public double distance(String tokenIdA, String tokenIdB) {
        return token(tokenIdA).position().distanceTo(token(tokenIdB).position());
    }
}
