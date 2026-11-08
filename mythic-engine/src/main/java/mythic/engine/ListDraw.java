package mythic.engine;

/** Ce que donne un tirage dans une liste d'aventure. */
public sealed interface ListDraw {

    AdventureList.Kind kind();

    /**
     * La ligne tirée, numérotée de 1 à 25. {@code sectionDie} et {@code sectionRoll} sont nuls avec une seule section
     * active. Une ligne vide vaut « Choisir » : le joueur prend l'entrée la plus logique, ou relance.
     */
    record Line(AdventureList.Kind kind, int line, Integer sectionDie, Integer sectionRoll, int lineRoll, boolean blank)
            implements ListDraw {}

    /** Aucune entrée : le focus devient « Contexte actuel ». */
    record Empty(AdventureList.Kind kind) implements ListDraw {}

    /** Une entrée au-delà de la 25e ligne : rien n'est tiré, la liste est à nettoyer. */
    record Full(AdventureList.Kind kind) implements ListDraw {}
}
