---
type: spike
status: open
parents:
  - aidd_docs/backlog/stories/voir-sur-la-fiche-les-pv-perdus-en-combat.md
  - aidd_docs/backlog/stories/creer-en-guide-un-magicien.md
---

# Spike : faire suivre à la fiche ce qui arrive au PJ en combat

## Question

La fiche est son propre agrégat à événements, distinct du combat, qui garde sa copie du PJ. Comment un coup du combat (PV perdus, emplacement dépensé) émet-il un événement sur la fiche ? Et comment le rejeu d'un combat, la reprise d'une partie au démarrage (rejeu de ses commandes), « Nouvelle carte » et la reprise d'un combat en cours évitent-ils de le réémettre ou de le perdre ?

## Decision

La forme des stories « voir sur la fiche les PV perdus en combat » et « créer en guidé un Magicien » (emplacements dépensés).

## Bounds

- Evidence needed : la garde actuelle par commandes et son rejeu (`SceneSession`, `SavedGame`, `CombatStore`, `CombatReplay`) ; le point où un événement du moteur (dégâts, emplacement dépensé) peut atteindre un autre agrégat sans entrer dans le journal du combat ; le stockage de la fiche dans le module D&D ; un essai jetable, hors dépôt, où un coup baisse les PV d'une fiche et où un rejeu ne les rebaisse pas.
- Stop when : un mécanisme esquissé, confirmé par l'essai, qui couvre le direct, la reprise, le rejeu et « Nouvelle carte » ; ou un cas impossible sans changer la garde par commandes, écart nommé.
