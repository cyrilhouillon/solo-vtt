package dnd.engine.attack;

/**
 * Les phases d'une attaque, dans l'ordre. L'ordre de l'enum EST l'ordre d'exécution.
 *
 * <p>Quand les règles imposent un ordre (ex. dégâts : ajustements, puis résistance,
 * puis vulnérabilité), on le traduit par des phases distinctes plutôt que par des priorités :
 * c'est explicite, et aucun module ajouté plus tard ne peut s'intercaler par erreur.</p>
 */
public enum AttackPhase {
    /** Annonce de l'attaque (et plus tard : vérifications de portée, de ligne de vue…). */
    DECLARE,
    /** Accumulation des sources d'avantage/désavantage et des bonus au jet. Ordre sans importance. */
    MODIFY_ROLL,
    /** Lancer du d20 et des dés de bonus. */
    ROLL,
    /** Comparaison avec la CA : touché « provisoire ». */
    DETERMINE_HIT,
    /** Fenêtre de réaction de la cible quand elle est touchée (Bouclier…). Peut suspendre le moteur. */
    HIT_REACTIONS,
    /** Re-vérification après réactions, puis résultat définitif. */
    CONFIRM_HIT,
    /** Lancer des dés de dégâts (doublés en cas de critique). */
    ROLL_DAMAGE,
    /** Bonus, malus, multiplicateurs. */
    DAMAGE_ADJUST,
    /** Résistance : divise par deux, arrondi inférieur. */
    DAMAGE_RESISTANCE,
    /** Vulnérabilité : multiplie par deux. */
    DAMAGE_VULNERABILITY,
    /** Les dégâts sont infligés. */
    APPLY_DAMAGE,
    /** Effets « quand vous touchez », jets de concentration, etc. */
    AFTER
}
