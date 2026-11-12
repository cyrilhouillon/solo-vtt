package dnd.engine.sheet;

/** Ce qui arrive à une fiche de personnage : son histoire, dont l'état de la fiche est le pli ({@link SheetHistory}). */
public sealed interface SheetEvent {

    /** La fiche est créée, vide ({@link CharacterSheet#blank()}). */
    record SheetCreated() implements SheetEvent {}

    /** Le joueur a saisi ces valeurs : elles remplacent toutes les précédentes. */
    record SheetEdited(CharacterSheet values) implements SheetEvent {
        public SheetEdited {
            if (values == null) throw new IllegalArgumentException("Une saisie porte ses valeurs");
        }
    }
}
