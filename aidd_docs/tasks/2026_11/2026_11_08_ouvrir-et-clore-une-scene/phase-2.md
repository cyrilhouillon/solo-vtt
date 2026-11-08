---
status: pending
---

# Instruction: le serveur Mythic teste et clôt une scène

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── mythic-server/src/
    ├── main/java/mythic/server/
    │   ├── MythicDtos.java ✏️
    │   └── MythicResource.java ✏️
    └── test/java/mythic/server/
        └── MythicResourceTest.java ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `ModuleData` et `PagesDeTest` en mémoire, dés scriptés | Deux campagnes, chaos 5 | api |
| Happy path   | `POST scenes { expected: "  On entre au donjon  " }`, d10 = 6 | Scène rendue nettoyée, chaos 5, jet 6, `EXPECTED` | api |
| Happy path   | Chaos 7, d10 = 5, d10 = 4 | `ALTERED`, ajustement Intensifier une activité et son jet | api |
| Happy path   | d10 = 2, focus 25, Personnages écrit | `INTERRUPTED`, `randomEvent` avec l'entrée tirée (même forme que pour une question) | api |
| Happy path   | `POST scenes/end { inControl: true }` à 5 | `{ inControl, before: 5, after: 4 }`, chaos gardé 4 dans `mythic.json`, fils et personnages conservés | api |
| Edge case    | `scenes/end` perdu à 9 | `before` 9, `after` 9 | api |
| Edge case    | `scenes/end` sans corps ou sans `inControl` | 400, rien d'écrit | api |
| Edge case    | Scène de plus de 500 caractères | 400, aucun dé | api |
| Edge case    | Campagne inconnue | 404 | api |
| Edge case    | Interrompue, Personnages perdu | Focus effectif Contexte actuel | api |

## Tasks to do

### `1)` Les échanges

> Ce que la scène et sa clôture rendent.

1. `MythicDtos` : `SceneRequest(expected)`, `SceneDto(expected, chaos, roll, outcome, outcomeLabel, adjustments [code, label], adjustmentRolls, randomEvent)`, `SceneEndRequest(Boolean inControl)`, `SceneEndDto(inControl, before, after)`.

### `2)` Tester une scène

> Même lecture des listes que la question.

1. Extraire de `ask` (l. 156-160) la lecture paresseuse des listes ; `ask` et la scène la partagent.
2. `POST /scenes` : campagne connue, scène facultative ≤ 500 caractères, `SceneCheck.test` au chaos gardé ; `@APIResponse` 200, 400, 404.

### `3)` Clore une scène

> Le chaos suit le contrôle.

1. `POST /scenes/end`, `synchronized` comme `setChaos` : `afterScene`, écrit l'état sans perdre les ids de listes ; `@APIResponse` 200, 400, 404.

### `4)` Tests

> Chaque ligne du Test Scope.

1. `MythicResourceTest`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Les DTO se lisent en JSON sans champ inattendu (vérifié en phase 3 au navigateur). |
| 2 | Les trois issues reviennent avec leurs dés ; les tests de question restent verts. |
| 3 | Le chaos gardé suit −1 / +1 aux bornes, `threadsPageId` et `charactersPageId` intacts. |
| 4 | `mvn -q test -pl mythic-server -am` vert. |
