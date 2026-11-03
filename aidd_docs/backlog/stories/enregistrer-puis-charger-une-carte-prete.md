---
type: story
status: ready
source: aidd_docs/tasks/2026_11/2026_11_01-solo-vtt-prd.md
order: 2
---

# Story : enregistrer puis charger une carte prête

**En tant que** joueur solo
**Je veux** enregistrer une carte préparée (image et adversaires placés) sous un nom, puis la recharger
**Afin de** lancer une scène vite, sans refaire la préparation

## Acceptance

- J'enregistre la carte en cours sous un nom : elle garde son image, son échelle, son affichage de la grille et les créatures posées avec leur position.
- Une liste montre les cartes enregistrées ; en choisir une la charge à la place de la carte en cours, comme « Nouvelle carte » : la partie repart de zéro, sans combat.
- Les créatures chargées sont neuves (PV pleins, aucun effet), aux positions enregistrées.
- Une fois la carte chargée, je pose mes personnages puis je lance le combat, comme d'habitude.
- Recharger la même carte plus tard redonne la même scène de départ.
- Les cartes enregistrées survivent au redémarrage du serveur.
