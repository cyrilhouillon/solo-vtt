---
type: story
status: done
parent: aidd_docs/backlog/epics/ranger-ses-parties-dans-des-campagnes.md
order: 1
---

# Story : créer et ouvrir une campagne depuis l'accueil

**En tant que** joueur solo
**Je veux** choisir ou créer une campagne en ouvrant l'application, chacune avec sa partie et ses cartes
**Afin de** mener plusieurs histoires sans que l'une écrase l'autre

## Acceptance

- En ouvrant l'application, j'arrive sur un accueil qui liste les campagnes existantes par leur nom et propose d'en créer une.
- Je crée une campagne en lui donnant un nom, non vide ; elle apparaît dans la liste.
- Ouvrir une campagne mène à l'écran de jeu actuel, qui affiche son nom ; une campagne neuve n'a ni carte, ni combat, ni carte enregistrée.
- Dans une campagne, je ne vois que ses cartes enregistrées ; en enregistrer une n'apparaît pas dans les autres campagnes.
- Je reviens à l'accueil depuis l'écran de jeu et j'ouvre une autre campagne.
- Revenir à une campagne la retrouve à l'identique : carte, positions, PV, combat, tour en cours, journal et décision en attente.
- Les campagnes et leur contenu survivent au redémarrage du serveur.
