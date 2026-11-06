---
type: story
status: done
parent: aidd_docs/backlog/epics/jouer-sa-campagne-avec-mythic-gme-depuis-le-carnet.md
order: 2
---

# Story : importer ses tables de mots et tirer un sens

**En tant que** joueur solo qui possède le livre
**Je veux** importer une fois mes tables de sens de Mythic, puis tirer un sens à la demande
**Afin d'**interpréter un événement ou une réponse ouverte sans feuilleter le livre

## Acceptance

- J'importe un fichier de tables : elles sont gardées dans mes données, communes à mes campagnes, jamais dans le dépôt. Un fichier mal formé est refusé avec la raison.
- Sans tables importées, le tirage de sens est indisponible et le panneau dit pourquoi ; l'oracle oui/non reste utilisable.
- Je tire deux mots dans la table choisie (actions, descriptions, éléments) ; ils s'écrivent dans la page ouverte.
- Un événement aléatoire tire aussi ses deux mots de sens.
