---
type: story
status: done
parent: aidd_docs/backlog/epics/jouer-un-combat-dnd-2024-en-local.md
depends_on:
  - aidd_docs/backlog/stories/preparer-une-scene-de-combat.md
order: 2
---

# Story : attaquer depuis la feuille en ciblant sur la carte

**En tant que** joueur solo
**Je veux** cliquer sur une attaque de la feuille d'une créature, puis cliquer sur sa cible sur la carte
**Afin que** le moteur résolve l'attaque selon les règles D&D 2024

## Acceptance

- Cliquer sur une attaque qui demande une cible passe la carte en mode ciblage.
- Une cible hors de portée de l'arme est refusée, avec la raison.
- Le jet, le résultat (touché, raté, critique) et les dégâts s'affichent dans le journal.
- Les PV de la cible baissent du montant final, après résistance et vulnérabilité.
- Ça marche de la même façon pour un adversaire, joué par le joueur.
