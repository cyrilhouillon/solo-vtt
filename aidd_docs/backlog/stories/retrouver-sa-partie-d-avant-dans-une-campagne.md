---
type: story
status: ready
parent: aidd_docs/backlog/epics/ranger-ses-parties-dans-des-campagnes.md
depends_on:
  - aidd_docs/backlog/stories/creer-et-ouvrir-une-campagne-depuis-l-accueil.md
order: 2
---

# Story : retrouver sa partie d'avant dans une campagne

**En tant que** joueur solo qui jouait avant l'arrivée des campagnes
**Je veux** retrouver ma partie et mes cartes enregistrées dans une campagne
**Afin de** ne rien perdre à la mise à jour

## Acceptance

- Au premier lancement après la mise à jour, si une partie ou des cartes enregistrées existent, l'accueil montre une campagne « Ma campagne » qui les contient.
- L'ouvrir redonne la partie à l'identique, décision en attente comprise, et toutes les cartes enregistrées.
- Les lancements suivants ne recréent pas cette campagne.
- Sans partie ni carte existante, l'accueil n'a aucune campagne.
- Une partie d'avant illisible est mise de côté comme aujourd'hui ; les cartes enregistrées sont quand même reprises.
