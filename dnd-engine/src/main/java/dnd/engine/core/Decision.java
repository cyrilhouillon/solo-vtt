package dnd.engine.core;

import java.util.List;

/**
 * Une question posée à un joueur (ou à une IA) au milieu d'une résolution.
 * Le moteur s'arrête tant qu'elle n'a pas reçu de réponse via {@link Engine#answer}.
 *
 * <p>Note : le handler est une lambda, donc non sérialisable. Pour sauvegarder une partie
 * en attente de décision (jeu en ligne, tour par tour asynchrone), remplace-le par un
 * type de décision + des paramètres, et retrouve le handler à partir du type.</p>
 */
public record Decision(String id, String chooserId, String prompt, List<String> options, DecisionHandler handler) {

    public Decision {
        options = List.copyOf(options);
    }

    @FunctionalInterface
    public interface DecisionHandler {
        void onChoice(String option, Engine engine);
    }
}
