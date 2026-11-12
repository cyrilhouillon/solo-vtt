package dnd.engine.sheet;

import java.util.List;

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

    /**
     * Le joueur a fait ces choix guidés ; {@code values} est la fiche qui en est sortie, gardée telle quelle pour qu'un
     * changement des données du SRD ne réécrive pas une fiche d'avant.
     */
    record SheetBuilt(Build build, CharacterSheet values) implements SheetEvent {
        public SheetBuilt {
            if (build == null) throw new IllegalArgumentException("Une construction porte ses choix");
            if (values == null) throw new IllegalArgumentException("Une construction porte ses valeurs");
        }
    }

    /** Les six jets de caractéristique de la fiche, lancés une seule fois. */
    record AbilitiesRolled(List<AbilityRoll> rolls) implements SheetEvent {
        public AbilitiesRolled {
            if (rolls == null || rolls.size() != Ability.values().length || rolls.contains(null)) {
                throw new IllegalArgumentException("Six jets de caractéristique");
            }
            rolls = List.copyOf(rolls);
        }
    }
}
