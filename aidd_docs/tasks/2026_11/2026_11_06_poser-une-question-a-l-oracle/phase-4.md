---
status: done
---

# Instruction: le Carnet accueille le panneau d'un module

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md ✏️
├── aidd_docs/memory/
│   ├── architecture.md ✏️
│   └── codebase-map.md ✏️
└── vtt-web/
    ├── tsconfig.app.json ✏️
    ├── modules/.gitkeep ✅
    └── src/
        ├── api.ts ✏️
        ├── modules/
        │   ├── notebookModule.ts ✅
        │   └── registry.ts ✅
        └── components/
            ├── NotebookScreen.vue ✏️
            └── PageEditor.vue ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Aucun module dans `vtt-web/modules/` ; serveurs de test 8081 / 5174 | Carnet ouvert sur une campagne du scratchpad | browser |
| Happy path   | `npm run build` sans module | Build vert ; Carnet sur deux colonnes, identique à avant | system |
| Happy path   | Module jetable non commité (`modules/essai/index.ts`, bouton qui écrit « Essai » par `writeToOpenPage`), page déverrouillée, curseur au milieu | Le paragraphe entre au curseur ; enregistré (relu après rechargement) ; Ctrl+Z le retire | browser |
| Happy path   | Même geste sur une page verrouillée | Le bloc s'ajoute à la fin, la page reste verrouillée ; enregistré | browser |
| Edge case    | Aucune page ouverte | `writeToOpenPage` rend « Aucune page ouverte… », rien n'est écrit | browser |
| Edge case    | Fenêtre étroite (≤ seuil existant) | Panneau empilé sous la page, sans défilement horizontal | browser |
| Teardown     | Supprimer le module jetable ; arrêter 8081 puis 5174 | `vtt-web/modules/` vide, données du joueur intactes | system |

## Wireframe

```txt
┌──────────────────────────────────────────────────────────────────┐
│ (1) Barre : Accueil · Solo VTT · campagne · Combat · thème         │
├────────────┬───────────────────────────────┬─────────────────────┤
│ (2) Pages  │ (3) Onglets                    │ (5) Panneau         │
│  Chercher  │ ┌───────────────────────────┐ │  du module          │
│  arbre     │ │ (4) Titre · Image · verrou │ │  (titre du module)  │
│  Nouvelle  │ │     texte de la page       │ │                     │
│  Corbeille │ │     pages citantes         │ │                     │
│            │ └───────────────────────────┘ │                     │
└────────────┴───────────────────────────────┴─────────────────────┘
```

1. Barre : inchangée.
2. Pages : inchangées.
3. Onglets : inchangés.
4. Page ouverte : inchangée ; reçoit ce qu'écrit un module.
5. Panneau d'un module : une section titrée par module qui en a un, dans l'ordre des ids ; colonne absente sans module, empilée sous la page en écran étroit.

## Tasks to do

### `1)` Le contrat client d'un module

> Un module du Carnet se déclare dans `vtt-web/modules/<id>/index.ts`, que le cœur découvre sans le nommer.

1. `src/modules/notebookModule.ts` : `NotebookModule` (`id`, `title`, `panel` : composant Vue) et `NotebookContext` (`campaignId`, `writeToOpenPage(blocks: JSONContent[]): Promise<string | null>`, raison d'un refus ou `null`).
2. `src/modules/registry.ts` : `import.meta.glob('../../modules/*/index.ts', { eager: true })`, modules triés par `id`.
3. `tsconfig.app.json` : `include` ajoute `modules/**/*.ts` et `modules/**/*.vue` ; `modules/.gitkeep`.
4. `api.ts` : exporter `request` et `ApiError` pour les modules (lecture seule du cœur par un module).

### `2)` Écrire dans la page ouverte

> Un module écrit des nœuds standard au curseur, ou à la fin d'une page verrouillée, et la page s'enregistre.

1. `PageEditor.insertBlocks(blocks)` : déverrouillée, `insertContent` au curseur, un seul pas d'annulation ; verrouillée, `insertContentAt` à la fin du document ; puis `emit('change', …)` explicite, `onUpdate` n'émettant que sur une page éditable.
2. `NotebookScreen` : `writeToOpenPage` refuse sans page (« Aucune page ouverte : ouvrir une page pour y écrire. »), sinon appelle `insertBlocks` ; l'enregistrement suit le chemin de la frappe (`onChange`).
3. `NotebookScreen` : colonne `aside` des panneaux de modules (`<component :is>` avec son contexte), grille à trois colonnes seulement si un module a un panneau ; empilée sous le seuil étroit existant.

### `3)` Mémoire et design

> Le point d'extension client est décrit.

1. `architecture.md` (contrat `NotebookModule`, découverte, écriture dans la page, verrou), `codebase-map.md` (`src/modules/`, `vtt-web/modules/`), `DESIGN.md` (colonne du panneau de module).

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` vert sans module ; avec le module jetable, il est dans le bundle sans que `src/` le nomme. |
| 2 | Sur 5174 : écriture au curseur (déverrouillée) ou en fin de page (verrouillée), enregistrée, annulable en écriture ; refus sans page. |
| 3 | Mémoire et `DESIGN.md` décrivent le contrat et la colonne. |
