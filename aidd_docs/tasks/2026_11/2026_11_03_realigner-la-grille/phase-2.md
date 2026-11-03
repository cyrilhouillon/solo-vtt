---
status: pending
---

# Instruction: garder le calage dans les cartes enregistrées

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── vtt-server/src/
    ├── main/java/solovtt/server/
    │   ├── SavedMap.java                                ✏️ gridOffsetX, gridOffsetY
    │   └── SceneSession.java                            ✏️ snapshot les prend, load les rejoue (AlignGrid)
    └── test/java/solovtt/server/
        ├── MapApiTest.java                              ✏️ carte calée enregistrée puis rechargée
        ├── ResumeGameTest.java                          ✏️ partie d'une carte calée chargée, reprise
        └── persistence/JsonFileMapStoreTest.java        ✏️ aller-retour ; ancien fichier sans calage
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | carte sur image calée (décalages 0,25 / 0,5, taille de case changée), deux créatures posées | carte calée | api |
| Happy path   | `POST /api/maps` « Crypte », nouvelle carte, puis `POST /api/maps/load` « Crypte » | mêmes dimensions, décalages et positions qu'au moment de l'enregistrement | api |
| Happy path   | relancer la session après le chargement | même calage | system |
| Edge case    | `JsonFileMapStore` relit un fichier écrit sans `gridOffsetX`/`gridOffsetY` | carte relue avec des décalages 0 | system |
| Edge case    | carte enregistrée sans décalage | le chargement ne garde aucune commande `AlignGrid` | system |
| Teardown     | `@TempDir` supprimé | aucun fichier laissé | system |

## Tasks to do

### `1)` Le modèle garde le calage

> Une carte enregistrée redonne sa grille calée.

1. `SavedMap` gagne `double gridOffsetX, double gridOffsetY` (après `gridVisible`) ; javadoc.
2. `snapshot` les remplit depuis la session.
3. `load` : après `create` et `showGrid`, `alignGrid(map.width(), map.height(), ox, oy)` seulement si un décalage n'est pas nul (facteur 1), avant les poses.

### `2)` Tests

> Le calage survit à l'enregistrement, au chargement et à la reprise.

1. `MapApiTest`, `ResumeGameTest`, `JsonFileMapStoreTest` : scénarios ci-dessus, dont l'ancien fichier sans champs.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Une carte calée, enregistrée puis chargée, rend les mêmes dimensions, décalages et positions. |
| 2 | Les trois classes de test passent ; un `cartes.json` sans champs de calage se relit avec des décalages 0. |
