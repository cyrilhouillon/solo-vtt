---
type: story
status: done
depends_on:
  - aidd_docs/backlog/stories/poser-une-question-oui-non-ou-lancer-un-d100-avec-l-oracle-generique.md
---

# Story : tirer un mot dans une de ses listes de l'oracle générique

**En tant que** joueur solo
**Je veux** tenir mes propres listes de mots dans des pages du Carnet et y tirer un mot
**Afin d'**improviser avec mon vocabulaire, propre à cette campagne

## Acceptance

- Le Carnet a une section « Oracle » avec une page racine « Listes ». Chacune de ses sous-pages est une liste, que je crée, renomme et écris comme toute page.
- Une ligne de la liste est un élément de premier niveau d'une liste à puces de la page.
- Le panneau me fait choisir une liste parmi ces sous-pages. Le tirage donne une ligne au hasard, à chances égales, et l'écrit dans la page ouverte avec le nom de la liste. Un lien `[[…]]` reste un lien.
- Une liste sans ligne, ou l'absence de page ouverte : le tirage est refusé avec la raison.
- Les listes appartiennent à cette campagne seulement.
