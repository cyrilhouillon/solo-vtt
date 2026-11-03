package solovtt.server;

import solovtt.scene.DistanceUnit;

import java.util.List;

/**
 * Une carte enregistrée sous un nom : un modèle de départ, pas une partie. Elle garde les dimensions, l'unité, l'image
 * de fond ({@code null} sans image), l'affichage de la grille, son calage (décalage en fraction de case, 0 dans les
 * cartes enregistrées avant le calage) et les créatures posées (entrée du catalogue et position), dans l'ordre de pose ;
 * ni PV, ni effets, ni combat, ni journal.
 */
public record SavedMap(String name, double width, double height, DistanceUnit unit, String imageId, boolean gridVisible,
                       double gridOffsetX, double gridOffsetY, List<SavedToken> tokens) {

    public SavedMap {
        tokens = List.copyOf(tokens);
    }

    /** Une créature à poser : son entrée du catalogue et sa position. */
    public record SavedToken(String catalogId, double x, double y) {}
}
