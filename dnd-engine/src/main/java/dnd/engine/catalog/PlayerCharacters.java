package dnd.engine.catalog;

import dnd.engine.events.GameEvent;
import dnd.engine.state.CreatureProfile;

import java.util.List;
import java.util.Optional;

/**
 * Les personnages joueurs d'une campagne, que le catalogue de préparation propose à côté des monstres. Le système qui tient
 * les fiches l'implémente ; le serveur le collecte sans le nommer et ne l'interroge que si son module est actif dans la
 * campagne.
 */
public interface PlayerCharacters {

    /** L'id du module qui tient ces personnages. */
    String moduleId();

    /** Les personnages de la campagne, triés par nom. */
    List<Listed> list(String campaignId);

    /**
     * La copie d'un personnage à cet instant : son nom et ce qu'il est maintenant. Vide s'il n'est pas un personnage de la
     * campagne.
     *
     * @throws IllegalArgumentException si sa fiche ne peut pas jouer, avec la raison
     */
    Optional<Taken> take(String campaignId, String id);

    /**
     * Ce qui vient d'arriver, sur une carte, à la copie posée du personnage {@code id} (dégâts subis, emplacements dépensés…), pour que sa fiche
     * le suive. Appelé une fois par commande vécue, jamais au rejeu d'une partie ou d'un combat.
     *
     * @return {@code false} si {@code id} n'est pas l'un de ses personnages ; {@code true} si sa fiche a suivi
     * @throws RuntimeException si sa fiche n'a pas pu suivre, avec la raison
     */
    default boolean sustained(String campaignId, String id, List<GameEvent> events) {
        return false;
    }

    /** Un personnage proposé : l'id de son entrée au catalogue et son nom. */
    record Listed(String id, String name) {}

    /** Un personnage copié : son nom et son profil à cet instant. */
    record Taken(String name, CreatureProfile profile) {}
}
