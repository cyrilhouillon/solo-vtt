---
status: pending
---

# Instruction: Mythic crée ses listes Fils et Personnages

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── aidd_docs/memory/
│   ├── architecture.md ✏️
│   └── codebase-map.md ✏️
└── mythic-server/src/
    ├── main/java/mythic/server/
    │   ├── MythicResource.java ✏️
    │   └── MythicDtos.java ✏️
    └── test/java/mythic/server/
        ├── MythicResourceTest.java ✏️
        └── PagesDeTest.java ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `ModuleData` et `NotebookPages` en mémoire (`PagesDeTest`), une campagne | Aucune page | system |
| Happy path   | `POST …/mythic/lists` | Deux racines de section `mythic`, « Fils » et « Personnages », chacune avec une liste à puces vide ; leurs ids rendus et gardés dans `mythic.json` | system |
| Happy path   | Second appel | Aucune page de plus, mêmes ids | system |
| Happy path   | Fils renommée « Intrigues », puis appel | Rien de créé | system |
| Happy path   | Chaos réglé à 7 puis appel ; appel puis chaos réglé | Chaos et ids survivent l'un à l'autre | system |
| Edge case    | `mythic.json` perdu ou abîmé, racines encore là | « Fils » et « Personnages » reprises par leur titre, sans doublon ; chaos 5 | system |
| Edge case    | Id gardé d'une page qui n'existe plus | Une nouvelle page est créée | system |
| Edge case    | Campagne inconnue | 404 | system |

## Tasks to do

### `1)` L'état de Mythic dans la campagne

> Chaos et ids des listes cohabitent dans `mythic.json`.

1. `State(Integer chaos, String threadsPageId, String charactersPageId)`, champs inconnus ignorés ; `setChaos` lit, fusionne, écrit ; un chaos absent ou abîmé reste 5.

### `2)` Les listes

> Une route idempotente assure les deux pages.

1. `MythicResource` reçoit `NotebookPages` ; `POST /lists` (`synchronized`) : pour chaque liste, garder l'id s'il est parmi `sectionRoots`, sinon reprendre une racine non attribuée du même titre, sinon `createSectionRoot` (titre, document `bulletList` d'un `listItem` vide) ; écrire l'état s'il change.
2. `MythicDtos.ListsDto(threadsPageId, charactersPageId)` ; `@APIResponse` 200 et 404.

### `3)` Tests

> Chaque ligne du Test Scope.

1. `PagesDeTest` : `NotebookPages` en mémoire ; `MythicResourceTest` adapté au nouveau constructeur.

### `4)` Mémoire

> La doc dit les listes.

1. `architecture.md` : route `lists`, contenu de `mythic.json`.
2. `codebase-map.md` : route `lists`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Régler le chaos ne perd pas les ids, et l'inverse. |
| 2 | Deux appels créent exactement deux pages, jamais de doublon, même renommées ou sans `mythic.json`. |
| 3 | `mvn -q test -pl mythic-server -am` vert. |
| 4 | La mémoire décrit la route et l'état gardé. |
