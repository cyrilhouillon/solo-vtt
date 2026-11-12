---
type: epic
status: proposed
goal: aidd_docs/tasks/2026_11/2026_11_01-solo-vtt-prd.md
source: aidd_docs/tasks/2026_11/2026_11_12_feuilles-personnage-dnd/brainstorm.md
depends_on:
  - aidd_docs/backlog/epics/jouer-un-combat-dnd-2024-en-local.md
  - aidd_docs/backlog/epics/jouer-sa-campagne-avec-mythic-gme-depuis-le-carnet.md
---

# Epic : tenir ses personnages D&D 2024 dans le Carnet et les jouer en combat

Le joueur crée ses personnages D&D 2024 de niveau 1, dans n'importe quelle classe du SRD, et les tient dans la section « Personnages joueurs » du Carnet. Il les pose en combat, et leur fiche suit ce qui leur arrive.

## Context and Value

- **Pour qui :** le joueur solo qui joue D&D 2024.
- **Constat aujourd'hui :** le catalogue n'offre que deux personnages prétirés, une Guerrière et une Magicienne. Une créature ne porte ni caractéristiques, ni compétences, ni sauvegardes. Le joueur ne peut donc ni créer son personnage, ni le suivre d'un combat à l'autre : chaque pose part d'une copie neuve, et ses PV sont perdus à la fin.
- **Valeur :** objectif 1 du PRD, « feuille de personnage, carte, résolution par le moteur ». Objectif 7 : D&D devient un module activable par campagne, comme Mythic. Objectif 6 : les personnages vivent dans la mémoire de la campagne.

## Boundaries

- **Inclut :**
  - un module D&D activable par campagne, qui porte les fiches et les règles affichées ;
  - les 12 classes du SRD 5.2.1 au niveau 1, avec espèces et historiques du SRD ;
  - la création guidée (classe, espèce, historique, caractéristiques, choix de classe, équipement) et la saisie libre. Toute valeur se retouche à la main. Changer un choix guidé recalcule ce qui en dérive : changer d'armure recalcule la CA, même retouchée ;
  - la fiche, un écran structuré qui passe pour une page du Carnet, rangé dans la section « Personnages joueurs » ;
  - la fiche, son propre agrégat à événements. Poser un personnage en combat en fait une copie, prise à la pose. Chaque coup émet aussi un événement sur la fiche : PV perdus, emplacements de sort dépensés. Le rejeu d'un combat ne relit jamais les événements de la fiche ;
  - les PJ de la campagne, dans le catalogue de préparation, à la place de la Guerrière et de la Magicienne ;
  - l'aide au survol, façon BG3, sur les termes de la fiche : une infobulle donne la règle du SRD français. T la fige, et un terme dans l'infobulle ouvre la sienne ;
  - le texte des règles que montre une fiche niveau 1, tiré du SRD 5.2.1 français (CC-BY 4.0, avec attribution) ;
  - la liste des capacités niveau 1 que le moteur ne sait pas jouer (sorts, Rage…), chacune devenant sa propre story.
- **Exclut :**
  - les niveaux au-delà de 1 et les sous-classes ;
  - la section Règles du Carnet (parcourir tout le SRD) : chantier à part ;
  - les repos court et long ;
  - mettre en œuvre dans le moteur les capacités listées : elles ont leurs stories, hors de cet epic ;
  - réserver le combat aux campagnes où D&D est actif : ce chantier attend un deuxième système ;
  - empêcher un même personnage d'être dans deux combats en cours.

## Success Evidence

- Le joueur crée en guidé un personnage de chacune des 12 classes, niveau 1, avec espèce et historique.
- Chaque fiche est complète et lisible dans « Personnages joueurs », et ses capacités ouvrent leur règle au survol.
- Il pose un de ces personnages en combat, l'attaque avec son arme, et ses PV baissent sur sa fiche, même après un redémarrage.
- Le rejeu de ce combat ne change pas la fiche.
- Chaque capacité niveau 1 que le moteur ne joue pas a sa story.
- Une campagne sans module D&D n'a ni la section, ni les fiches.

## Dependencies and Unknowns

| Item | Kind | Handling |
| --- | --- | --- |
| Epic combat D&D | dependency | `done` |
| Socle de module du Carnet (section, panneau, données) | dependency | `done`, livré avec l'epic Mythic |
| SRD 5.2.1 français (`Compendio SRD/Français`, notes Obsidian) | dependency | source du texte des règles, versable au dépôt avec attribution |
| Parties d'avant qui citent `guerriere` ou `magicienne` (combats enregistrés, cartes, parties) | decision | à trancher dans la story du catalogue : garder ces entrées cachées, ou accepter que ces parties deviennent illisibles |
| Création guidée : méthode des caractéristiques, équipement de départ, dons d'origine, sorts connus au niveau 1 | decision | à trancher dans les stories de création |
| Copie prise à la pose : une retouche de la fiche pendant le combat ne touche pas la créature posée | assumption | acceptée |
| Fiche à événements, distincte du combat : comment un coup du combat atteint-il la fiche, et qu'en est-il au rejeu ? | spike | à cadrer avant la story « les PV baissent sur la fiche » |
