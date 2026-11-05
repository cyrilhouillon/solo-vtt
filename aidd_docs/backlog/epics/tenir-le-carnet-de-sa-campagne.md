---
type: epic
status: ready
goal: aidd_docs/tasks/2026_11/2026_11_01-solo-vtt-prd.md
source: aidd_docs/tasks/2026_11/2026_11_04_journal-de-campagne/brainstorm.md
depends_on:
  - aidd_docs/backlog/epics/ranger-ses-parties-dans-des-campagnes.md
---

# Epic : tenir le carnet de sa campagne

Ouvrir une campagne mène à son Carnet : le joueur y relit où il en était, raconte sa session et tient son lore dans des pages liées, puis passe au combat et revient.

## Context and Value

- **Pour qui :** le joueur solo, qui passe l'essentiel de sa partie à raconter ; le combat n'en est qu'un moment.
- **Constat aujourd'hui :** une campagne ne garde que sa carte et son combat ; le récit, les PNJ et le lore vivent hors de l'outil.
- **Valeur :** objectif 6 du PRD, « retrouver en quelques secondes, dans un wiki fluide et ergonomique, ce qui s'est passé lors des sessions précédentes ». Le Carnet devient l'écran principal de la campagne, où viendront ensuite les oracles et aides solo.

## Boundaries

- **Inclut :**
  - ouvrir une campagne mène au Carnet, sur l'onglet actif ; Carnet et combat sont deux écrans séparés, on passe de l'un à l'autre ;
  - un arbre libre de pages : créer, renommer, déplacer, supprimer ; une session est une page ordinaire ;
  - l'édition riche au fil du texte (titres, gras, italique, listes, citations, images), enregistrée automatiquement, avec annuler / rétablir ;
  - lier une page existante ; le lien suit le renommage et le déplacement de sa cible ;
  - les liens entrants d'une page (« qui pointe ici ») ;
  - la recherche ;
  - plusieurs pages ouvertes en onglets, retrouvés au retour du combat et à la réouverture de la campagne ;
  - supprimer une page liée avertit (« 3 pages y renvoient ») ; supprimer une page à sous-pages se confirme, et le tout part à la corbeille, d'où il se restaure ; un lien vers une page supprimée s'affiche mort.
- **Exclut :**
  - créer une page à la volée depuis un lien (`[[Bertrand]]`), à rendre possible ensuite ;
  - les liens vers des combats, des donjons ou des cartes ;
  - les oracles et aides solo (Mythic, Adventure Crafter), à droite de l'écran plus tard ;
  - l'historique des versions d'une page ;
  - le type « session » ou une date en métadonnée ;
  - éditer les pages hors de l'application, l'export ;
  - les comptes et l'hébergement.

## Success Evidence

- Le joueur ouvre sa campagne et arrive sur sa dernière page ; il crée « Session 5 » sous « Sessions », y raconte une scène en liant deux PNJ existants, ouvre le combat, le joue, revient : sa page et ses onglets sont là.
- Il renomme un PNJ : les liens de toutes les sessions affichent le nouveau nom ; la page du PNJ liste les sessions qui la citent.
- Il retrouve par la recherche une session d'après un mot de son texte.
- Il supprime par erreur une rubrique et ses sous-pages, puis la restaure depuis la corbeille, liens compris.
- Après redémarrage du serveur, tout est à l'identique.

## Dependencies and Unknowns

| Élément | Nature | Traitement |
| --- | --- | --- |
| La campagne comme unité de la partie | dépendance | epic « ranger ses parties dans des campagnes » (done) |
| Ouvrir une campagne mène au Carnet, et non plus à l'écran de jeu | décision | prise par le joueur ; change le comportement livré par l'epic des campagnes |
| Le nouvel écran s'appelle « Carnet » ; l'onglet Journal du combat garde son nom | décision | prise par le joueur |
| Éditeur Tiptap 3 ; enregistrement automatique et corbeille à notre charge | décision | spike `aidd_docs/backlog/spikes/choisir-l-editeur-du-journal-de-campagne.md` (resolved) |
| Format stocké : Markdown augmenté ou JSON Tiptap | décision | différée à la conception ; sans effet visible |
| Annuler / rétablir de l'éditeur suffit contre une modification malheureuse | décision | prise par le joueur |
| La recherche porte sur le titre et le texte des pages | hypothèse | acceptée |
| Images d'une page dans le magasin d'images commun, comme les cartes | hypothèse | acceptée : sans effet visible |
| La corbeille garde les pages sans limite de durée ; on peut la vider | hypothèse | acceptée |
| Campagne sans page : on arrive sur un Carnet vide qui invite à créer la première | hypothèse | acceptée |
