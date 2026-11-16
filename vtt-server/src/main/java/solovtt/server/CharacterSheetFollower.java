package solovtt.server;

import dnd.engine.events.GameEvent;

import java.util.List;

/**
 * Fait suivre à la fiche d'un personnage joueur ce qui arrive à sa copie posée sur la carte (dégâts subis). La session
 * l'appelle une fois par commande vécue, après l'avoir gardée, jamais au rejeu.
 */
@FunctionalInterface
public interface CharacterSheetFollower {

    /** Ne suit rien : une session sans fiches. */
    CharacterSheetFollower NONE = (characterId, events) -> {};

    /**
     * @param characterId l'id du personnage au catalogue, qui est celui de sa fiche
     * @throws RuntimeException si sa fiche n'a pas pu suivre, avec la raison
     */
    void sustained(String characterId, List<GameEvent> events);
}
