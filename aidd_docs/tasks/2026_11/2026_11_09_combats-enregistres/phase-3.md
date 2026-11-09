---
status: pending
---

# Instruction: Client : l'entrée combat dans le Carnet

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/src/
├── types.ts                         ✏️ PageSummary.combatId, CombatEntry
├── api.ts                           ✏️ combatEntry(id), resumeCombat(id), combatReplay(id)
├── notebook/tree.ts                 ✏️ visiblePages garde la section du cœur « combats »
└── components/
    ├── NotebookScreen.vue           ✏️ ouvre une entrée combat sans éditeur
    ├── CombatEntryView.vue          ✅ titre, état, miniature, ouvrir le combat
    ├── CombatThumbnail.vue          ✅ image de fond et pastilles, sans Konva
    ├── PageTree.vue                 ✏️ titre « Combats » de la section du cœur
    └── PageBranch.vue               ✏️ pas de « Nouvelle sous-page » sur un combat
```

## Test Scope

| Scenario | Action | Observable outcome | Actor |
| -------- | ------ | ------------------ | ----- |
| Setup | Serveurs de test 8081/5174 ; campagne avec un combat terminé et un en cours | Section « Combats » visible dans l'arbre | browser |
| Happy path | Clic sur « Combat 1 » dans l'arbre | Un onglet s'ouvre ; à la place de l'éditeur, titre, « Terminé » et la miniature de la carte finale | browser |
| Happy path | Clic sur la miniature | Écran de combat du combat (phase 4) | browser |
| Happy path | `[[Comb` dans une page | « Combat 1 » proposé ; le lien ouvre l'entrée (miniature) | browser |
| Edge case | Renommer, déplacer sous une page, supprimer puis restaurer l'entrée | Comme une page ; l'onglet suit le titre | browser |
| Edge case | Campagne sans module actif | La section « Combats » reste visible | browser |
| Edge case | Combat illisible (409) | L'entrée dit « Combat illisible », sans miniature | browser |
| Teardown | Arrêter 8081 et 5174 | Ports du joueur intacts | system |

## Wireframe

```txt
┌──────────────┬──────────────────────────────────────────┬──────────┐
│ (1) Pages    │ (2) Onglets : Session 2 · Combat 1 ×     │ modules  │
│  …           ├──────────────────────────────────────────┤          │
│  Combats     │ (3) Combat 1                Terminé       │          │
│   Combat 1   │ ┌──────────────────────────────────────┐ │          │
│   Combat 2   │ │ (4) miniature : image + pastilles     │ │          │
│              │ └──────────────────────────────────────┘ │          │
│              │ (5) [ Revoir le combat ]                 │          │
│              │ (6) Pages qui citent ce combat           │          │
└──────────────┴──────────────────────────────────────────┴──────────┘
```

1. Arbre : la section du cœur « Combats » et ses entrées.
2. Onglets : l'entrée combat est un onglet comme une page.
3. Titre (renommable comme une page) et état : « En cours » ou « Terminé ».
4. Miniature cliquable : fond de carte et créatures à l'état actuel ou final.
5. Action : « Revoir le combat » (terminé) ou « Reprendre le combat » (en cours) ; même effet que le clic sur la miniature.
6. Pages citantes, comme sous une page.

## Tasks to do

### `1)` Le type d'entrée

> Le client distingue une entrée combat d'une page.

1. `combatId: string | null` dans `PageSummary` / `PageView` ; `CombatEntry { id, ended, current, scene }`.
2. `api.combatEntry`, `api.resumeCombat`, `api.combatReplay`.
3. `visiblePages` garde toujours la section `combats` ; `PageTree` la titre « Combats ».
4. `PageBranch` : ni « Nouvelle sous-page » ni dépôt d'une page sous une entrée combat (`canMove` refuse un parent combat).

### `2)` Afficher l'entrée

> Ouvrir une entrée combat montre sa miniature au lieu de l'éditeur.

1. `NotebookScreen.show` : pour une page à `combatId`, pas de `PageEditor` ni de verrou ; monte `CombatEntryView`.
2. `CombatEntryView` lit `api.combatEntry(combatId)` et montre titre, état, `CombatThumbnail`, action et `CitingPages`.
3. `CombatThumbnail` : `<img>` de `imageUrl(imageId)` (fond neutre sans image) et une pastille par créature placée en `x / width`, `y / height`, à terre barrée.
4. Clic sur la miniature ou l'action → route du combat (phase 4), après `saved()`.

## Test acceptance criteria

| Task | Acceptance criteria |
| ---- | ------------------- |
| 1 | La section « Combats » s'affiche même sans module ; aucune sous-page ne peut être créée ni déposée sous un combat ; `npm run build` passe |
| 2 | Ouvrir une entrée combat (arbre, lien `[[`, onglet, recherche) montre la miniature de son état actuel ou final ; le clic mène à l'écran du combat |
