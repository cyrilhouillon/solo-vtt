---
status: pending
---

# Instruction: mémoire du projet

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── aidd_docs/memory/
    ├── architecture.md      ✏️ entrée de module, module D&D, fiche à événements
    ├── codebase-map.md      ✏️ dnd-server, dnd.engine.sheet, vtt-web/modules/dnd
    ├── package.md           ✏️ dnd-server et ses dépendances
    ├── project-brief.md     ✏️ fonctionnalité « fiches de personnage »
    └── testing.md           ✏️ dnd-server/src/test
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Happy path   | relire chaque fichier modifié contre le code livré | chaque nom de classe, route et fichier cité existe | cli |
| Edge case    | chercher `combatId` dans `architecture.md` | la description des entrées de combat reste vraie à côté des entrées de module | cli |

## Tasks to do

### `1)` Décrire ce qui a changé

> La mémoire dit ce que le code fait désormais.

1. `architecture.md` : entrée de module (`Page.entryOf`, `NotebookPages.createEntry` / `isEntry`, `NotebookModule.entryView`) ; module D&D (`dnd-server`, routes `…/dnd/sections` et `…/dnd/sheets`, `modules/dnd.json`) ; fiche à événements (`dnd.engine.sheet`, nom = titre de page, dérivés calculés).
2. `codebase-map.md`, `package.md`, `project-brief.md`, `testing.md` : nouveaux modules, paquet et tests.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Chaque fichier de mémoire cite les noms réels du code livré ; aucune description d'avant n'est contredite. |
