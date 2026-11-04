---
type: story
status: done
parent: aidd_docs/backlog/epics/tenir-le-carnet-de-sa-campagne.md
depends_on:
  - aidd_docs/backlog/stories/ecrire-une-page-dans-le-carnet.md
order: 5
---

# Story : retrouver une page du Carnet par la recherche

**En tant que** joueur solo
**Je veux** chercher un mot dans tout mon Carnet
**Afin de** retrouver en quelques secondes ce qui s'est passé lors d'une session précédente

## Acceptance

- Je tape un ou plusieurs mots, et le Carnet liste les pages dont le titre ou le texte les contient.
- La recherche ignore la casse et les accents (« eveque » trouve « Évêque »).
- Chaque résultat montre le titre et un extrait autour du mot trouvé, et l'ouvre au clic.
- Une recherche sans résultat le dit.
- La recherche ne porte que sur la campagne ouverte.
