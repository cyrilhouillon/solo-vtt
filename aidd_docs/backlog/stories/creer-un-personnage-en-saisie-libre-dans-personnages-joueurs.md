---
type: story
status: ready
parent: aidd_docs/backlog/epics/tenir-ses-personnages-dnd-2024-dans-le-carnet.md
order: 1
---

# Story : créer un personnage en saisie libre et le retrouver dans « Personnages joueurs »

**En tant que** joueur solo
**Je veux** créer la fiche d'un personnage en tapant ses valeurs
**Afin de** tenir dans le Carnet un personnage que l'assistant ne sait pas encore construire

## Acceptance

- Le module D&D est installé et s'active ou se désactive par campagne, comme Mythic. Actif, l'arbre du Carnet montre la section « Personnages joueurs ».
- « Nouveau personnage » crée une fiche vide, rangée dans la section.
- La fiche reprend les rubriques de la fiche D&D 2024 :
  - identité (nom, classe, niveau, espèce, historique) ;
  - les 6 caractéristiques et leurs modificateurs, le bonus de maîtrise, les sauvegardes et les compétences ;
  - CA, PV maximum et actuels, dés de vie, vitesse, initiative, Perception passive ;
  - attaques, capacités, équipement, maîtrises, langues, emplacements de sort.
- Chaque valeur se modifie à la main et se garde, même après un redémarrage.
- La fiche passe pour une page : elle s'ouvre en onglet, se renomme, se lie par `[[…]]` et se trouve par son titre dans la recherche.
- Un module D&D désactivé fait disparaître la section, sans perte. Réactivé, il retrouve ses fiches.
