package solovtt.scene;

/** Point de la carte, dans l'unité de la carte ; l'origine est le coin haut gauche. */
public record Position(double x, double y) {

    public double distanceTo(Position other) {
        return Math.hypot(other.x - x, other.y - y);
    }
}
