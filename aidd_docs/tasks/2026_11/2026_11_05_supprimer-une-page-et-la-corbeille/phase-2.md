---
status: done
---

# Instruction: le Carnet supprime, restaure et vide

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md ✏️
├── aidd_docs/
│   ├── backlog/stories/supprimer-une-page-et-la-restaurer-depuis-la-corbeille.md ✏️
│   ├── tasks/2026_11/2026_11_05_supprimer-une-page-et-la-corbeille/plan.md ✏️
│   └── memory/
│       ├── architecture.md ✏️
│       ├── codebase-map.md ✏️
│       └── project-brief.md ✏️
└── vtt-web/src/
    ├── api.ts ✏️
    ├── types.ts ✏️
    ├── notebook/context.ts ✏️
    └── components/
        ├── DeletePageDialog.vue ✅
        ├── NotebookScreen.vue ✏️
        ├── PageBranch.vue ✏️
        ├── PageTree.vue ✏️
        └── TrashDialog.vue ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081 / 5174 (`testing.md`) ; campagne avec PNJ > Bertrand > Fils, Sessions > Session 1 (lie Bertrand et Fils), Auberge (lie Fils) ; onglets Bertrand, Session 1, Fils | Carnet ouvert | browser |
| Happy path   | Menu de Bertrand > « Supprimer… » | Dialogue : « Supprimer « Bertrand » et sa sous-page ? 2 pages y renvoient : leurs liens s'afficheront morts. » ; Annuler ou Échap ne change rien, le focus revient au bouton de menu | browser |
| Happy path   | Confirmer, Bertrand étant l'onglet actif | Bertrand et Fils quittent l'arbre ; leurs onglets se ferment, Session 1 s'active ; ses liens vers Bertrand et Fils sont barrés et n'ouvrent rien | browser |
| Happy path   | « Corbeille » au pied du panneau Pages | Dialogue : Bertrand, « et 1 sous-page », date de suppression, bouton Restaurer | browser |
| Happy path   | Restaurer Bertrand | Bertrand revient sous PNJ, déplié jusqu'à lui, Fils dessous ; les liens de Session 1 revivent ; la corbeille est vide | browser |
| Happy path   | Supprimer PNJ (sans sous-page restante à part), puis « Vider la corbeille », confirmer | Corbeille vide : « La corbeille est vide. » ; l'entrée ne se restaure plus | browser |
| Edge case    | Supprimer une page sans sous-page ni page citante | Le message ne dit que « Supprimer « … » ? » | browser |
| Edge case    | Supprimer PNJ alors que Bertrand est déjà à la corbeille, puis restaurer Bertrand | Bertrand revient à la racine | browser |
| Edge case    | Taper dans Bertrand déverrouillé puis le supprimer avant une seconde ; puis `…/content` bloqué et recommencer | La frappe part avant la suppression (relue après restauration) ; bloquée : erreur au bandeau, rien n'est supprimé | browser |
| Edge case    | Supprimer la dernière page du Carnet | Carnet vide qui invite à créer la première page, avec le bouton « Corbeille » ; la restaurer rend l'arbre | browser |
| Edge case    | Au clavier seul, puis au doigt à 375px : supprimer, ouvrir la corbeille, restaurer, vider | Chaque geste aboutit ; le focus reste dans les dialogues puis revient au déclencheur | browser |
| Edge case    | `DELETE …/pages/{id}` bloqué | Erreur au bandeau, l'arbre et les onglets inchangés | browser |
| Teardown     | Arrêter 8081 puis 5174 ; effacer le dossier scratchpad | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌──────────────────────────────────────────────────────────────┐
│ (1) Barre : Accueil · Solo VTT · campagne · Combat · thème    │
├───────────────────┬──────────────────────────────────────────┤
│ PAGES             │ Session 1 ✕│ Auberge ✕│                    │
│ Chercher          │ Session 1                              🔒 │
│ ▾ PNJ        ⋯    │ Nous retrouvons ~~Bertrand~~ à l'auberge… │
│    Bertrand  ⋯ ┌──────────────┐                               │
│ ▾ Sessions     │(2) Nouvelle sous-page                         │
│    Session 1   │    Renommer  │                               │
│   Auberge      │    Déplacer sous…                             │
│                │    Supprimer…│                               │
│ Nouvelle page  └──────────────┘                               │
│ [Titre ] Créer    │                                          │
│ (3) 🗑 Corbeille  │                                          │
└───────────────────┴──────────────────────────────────────────┘

┌ (4) Supprimer « Bertrand » ───────────────┐
│ ─────────────                              │
│ Supprimer « Bertrand » et sa sous-page ?   │
│ 2 pages y renvoient : leurs liens          │
│ s'afficheront morts.                       │
│ Elle ira à la corbeille.                   │
│                 [Supprimer]  [Annuler]     │
└────────────────────────────────────────────┘

┌ (5) Corbeille ─────────────────────────────┐
│ ─────────────                               │
│ Bertrand  et 1 sous-page                    │
│ 5 nov. 14:32                    [Restaurer] │
│ ─────────────────────────────────────────── │
│ Auberge                                     │
│ 5 nov. 14:10                    [Restaurer] │
│ ─────────────────────────────────────────── │
│ [Vider la corbeille]             [Fermer]   │
│   ↳ (6) Tout effacer ? [Vider] [Annuler]    │
└─────────────────────────────────────────────┘
```

1. Barre de l'application, inchangée.
2. Menu de ligne : « Supprimer… » en dernier, en `danger`.
3. Pied du panneau Pages, sous « Nouvelle page » : bouton « Corbeille » à icône, `ink-muted`.
4. Dialogue modal de confirmation, panneau de bois comme « Déplacer » : titre, filet, message (sous-pages et pages citantes seulement si non nulles), « Supprimer » en `danger` (focus d'entrée sur « Annuler »).
5. Dialogue modal de la corbeille : une ligne par suppression, la plus récente d'abord (titre, sous-pages, date courte), « Restaurer » ; vide : « La corbeille est vide. » ; « Vider la corbeille » en `danger`, éteint si vide.
6. Confirmation en ligne de « Vider », comme « Charger » des cartes enregistrées.

## Tasks to do

### `1)` Appeler les routes de la phase 1

> Le client supprime, lit la corbeille, restaure et vide.

1. `types.ts` : `PageDeletion { subpageCount, citingCount }`, `TrashedPage { id, title, deletedAt, subpageCount }`.
2. `api.ts` : `pageDeletion(id)`, `deletePage(id)`, `trash()`, `restorePage(id)`, `emptyTrash()`.

### `2)` Le menu de ligne

> « Supprimer… » comme « Déplacer sous… ».

1. `context.ts` : `requestDelete(pageId)` ; `PageTree.vue` l'émet (`delete`) après avoir fermé le menu ; `PageBranch.vue` : entrée « Supprimer… » en dernier.

### `3)` Confirmer la suppression

> Un dialogue qui dit ce qui part.

1. `DeletePageDialog.vue` : props `page`, `deletion` (`null` pendant la lecture), `busy` ; émet `confirm` et `close` ; message accordé au nombre (« sa sous-page », « ses 2 sous-pages » ; « 1 page y renvoie », « 3 pages y renvoient ») ; même structure modale que `MovePageDialog` (Échap, `cancel`).
2. `NotebookScreen` : `deleting` ; à l'ouverture, `api.pageDeletion` (échec au bandeau, dialogue fermé).

### `4)` Supprimer, restaurer, vider dans `NotebookScreen`

> Les onglets suivent ; la frappe part d'abord.

1. Généraliser `closing(pageId)` en `closeTabs(ids)` : retire tous les onglets de `ids` ; si l'actif en est, active le voisin de droite du premier retiré, sinon de gauche, sinon aucune page ; `closeTab` l'appelle avec un seul id.
2. `deletePage(id)` par `call` : lot = `branch(pages, id)` (`notebook/tree.ts`) ; si la page ouverte en est, `saved()` d'abord (échec : rien ne part) ; `api.deletePage`, relire le Carnet, puis `closeTabs(lot)` ; le focus revient à l'arbre.
3. `TrashDialog.vue` : props `entries` (`null` pendant la lecture), `busy` ; émet `restore(id)`, `empty`, `close` ; « Vider » se confirme en ligne.
4. Bouton « Corbeille » au pied du panneau Pages, et aussi dans le Carnet vide (sous « Créer ») pour restaurer la dernière page supprimée ; ouvrir lit `api.trash()`.
5. `restore(id)` : `api.restorePage`, relire Carnet et corbeille, `reveal(id)` ; `empty()` : `api.emptyTrash`, relire la corbeille.

### `5)` Valider dans le navigateur

> Le Test Scope, sur 8081 / 5174.

1. `npm run build` dans `vtt-web/`.
2. Dérouler le Test Scope, à 1280px et 375px, thèmes sombre et clair.

### `6)` Documentation et statuts

> La mémoire et le design disent la corbeille.

1. `architecture.md` : suppression confirmée (lot, pages citantes), onglets du lot fermés, frappe envoyée d'abord, corbeille en dialogue, liens qui revivent.
2. `codebase-map.md` : `DeletePageDialog.vue`, `TrashDialog.vue`.
3. `project-brief.md` : suppression et corbeille dans la fonctionnalité Carnet.
4. `DESIGN.md` : « Supprimer… » du menu, dialogues Supprimer et Corbeille, bouton Corbeille.
5. Story `status: done` ; plan `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe avec les cinq appels |
| 2 | « Supprimer… » s'ouvre du menu à la souris, au clavier et au doigt |
| 3 | Le message dit le nombre de sous-pages et de pages citantes, au bon accord, et rien quand ils sont nuls ; Annuler ou Échap ne supprime rien |
| 4 | La page et ses sous-pages quittent l'arbre et leurs onglets se ferment ; une frappe en attente part d'abord, un échec ne supprime rien ; restaurer la remet à sa place (ou à la racine) et ses liens revivent ; vider ne laisse rien à restaurer |
| 5 | Le Test Scope passe à 1280px et 375px, thèmes sombre et clair |
| 6 | Mémoire, `DESIGN.md`, story et plan à jour |
