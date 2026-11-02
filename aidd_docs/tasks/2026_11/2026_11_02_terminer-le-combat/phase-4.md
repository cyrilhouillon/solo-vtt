---
status: pending
---

# Instruction: Mettre à jour la mémoire projet, le design et la story

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md                                                        ✏️
└── aidd_docs/
    ├── backlog/stories/terminer-le-combat-a-tout-moment.md          ✏️
    └── memory/
        ├── architecture.md                                          ✏️
        ├── codebase-map.md                                          ✏️
        └── project-brief.md                                         ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Phases 1 à 3 livrées | Code et parcours vérifiés | system |
| Happy path   | Relire chaque document modifié contre le code | Chaque phrase ajoutée décrit le code livré | system |
| Edge case    | Listes existantes à compléter (`architecture.md` : résolutions du combat, actions refusées pendant une décision) | Complétées, pas doublées | system |

## Tasks to do

### `1)` Mémoire

> La mémoire dit comment le combat se termine.

1. `architecture.md` : ajouter `EndCombat` aux résolutions simples du combat (et `CombatEnded`) ; ajouter la fin du combat aux actions refusées pendant une décision.
2. `codebase-map.md` : `EndCombat` dans `combat/`, route `combat/end`.
3. `project-brief.md` : « Terminer le combat » dans la fonctionnalité Combat au tour par tour.

### `2)` Design et story

> Le contrôle de combat et la story suivent.

1. `DESIGN.md`, Contrôle de combat : bouton ghost « Terminer le combat » et sa confirmation sur place ; décrire aussi la confirmation en deux temps du retrait en combat, qui n'y figure pas.
2. Story : `status: done`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `architecture.md`, `codebase-map.md` et `project-brief.md` nomment `EndCombat`, la route et la fonctionnalité, sans contradiction. |
| 2 | `DESIGN.md` décrit le bouton et la confirmation ; la story est `done`. |
