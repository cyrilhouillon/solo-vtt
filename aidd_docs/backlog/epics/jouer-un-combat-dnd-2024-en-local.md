---
type: epic
status: done
source: aidd_docs/tasks/2026_11/2026_11_01-solo-vtt-prd.md
---

# Epic : jouer un combat D&D 2024 en local, de bout en bout

Le joueur solo mène un combat D&D 2024 complet : il place les créatures, joue les tours, agit depuis les feuilles en ciblant sur la carte et termine le combat, sans quitter l'outil.

## Context and Value

- **Pour qui :** le joueur solo, qui tient à la fois le rôle du joueur et celui du MJ.
- **Besoin :** c'est la première version du PRD. Si le combat est jouable, l'outil a déjà de la valeur.
- **Constat aujourd'hui :** `dnd-engine` résout une attaque, mais ce n'est qu'une démo console. Il n'y a ni interface, ni tours, ni carte.
- **Valeur :** valider ensemble, sur un vrai cas de jeu, le moteur, la feuille et la carte. Ce sont les trois fondations du produit.

## Boundaries

- **Inclut :**
  - une carte minimale : placer et déplacer les créatures, avec une échelle en mètres ou en pieds et l'affichage des distances ;
  - des feuilles de personnage interactives, pour les personnages comme pour les adversaires, où une action se lance au clic et sa cible se choisit sur la carte ;
  - les attaques, résolues par le moteur jusqu'aux dégâts ;
  - l'enchaînement des tours selon l'initiative D&D 2024. Le moteur générique gère l'enchaînement des tours (le tour, la créature qui joue, la suivante), tandis que l'initiative relève de D&D. Un système sans initiative doit rester possible plus tard sans réécrire le moteur ;
  - un déplacement limité par la vitesse de la créature ;
  - la fin du combat, déclenchable par le joueur à tout moment ;
  - la persistance locale : une partie fermée puis rouverte reprend à l'identique.
- **Exclut :**
  - les sorts, les conditions à durée et les réactions au-delà de ce qu'a déjà `dnd-engine` ;
  - la lumière, la vision et les murs ;
  - les comptes utilisateurs et l'hébergement ;
  - le wiki, les oracles, les générateurs et les modules solo ;
  - l'IA ;
  - un deuxième système de jeu.

## Success Evidence

- Le joueur joue de bout en bout un combat de deux personnages contre trois adversaires qu'il contrôle lui-même. Il ne touche ni au code ni à l'état, et le journal retrace chaque action.
- Le combat reprend à l'identique après fermeture puis réouverture.

## Dependencies and Unknowns

| Élément | Nature | Traitement |
| --- | --- | --- |
| Principes communs tirés de `dnd-engine` | dépendance | séparer le générique de ce qui est propre à D&D avant de bâtir dessus |
| Limite entre moteur générique et D&D pour les tours | décision | prise : l'initiative relève de D&D, l'enchaînement des tours relève du moteur |
| Contrôle des adversaires | décision | prise : le joueur les contrôle |
| Contenu D&D disponible : créatures, armes, feuilles | dépendance | le SRD 5.2 (CC-BY-4.0) |
