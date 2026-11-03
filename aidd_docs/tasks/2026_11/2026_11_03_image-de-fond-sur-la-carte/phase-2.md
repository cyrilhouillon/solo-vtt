---
status: done
---

# Instruction: carte avec image et grille affichable, gardées avec la partie

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-server/src/
├── main/java/solovtt/server/
│   ├── SceneSession.java                   ✏️ image de fond et grille de la carte ; create(…, imageId), showGrid(visible) ; rejeu
│   ├── SceneResource.java                  ✏️ imageId à la création (image inconnue refusée), PUT /api/scene/grid
│   ├── Dtos.java                           ✏️ NewSceneRequest.imageId, GridRequest, SceneDto.imageId / gridVisible
│   ├── SavedGame.java                      ✏️ Create.imageId, commande ShowGrid
│   └── persistence/JsonFileGameStore.java  ✏️ sous-type « showGrid »
└── test/java/solovtt/server/
    ├── SceneApiTest.java                   ✏️ carte avec image, image inconnue, bascule de grille
    ├── ResumeGameTest.java                 ✏️ image et grille reprises ; image disparue
    └── persistence/JsonFileGameStoreTest.java ✏️ ShowGrid aller-retour ; ancienne partie sans imageId
aidd_docs/memory/
├── architecture.md                         ✏️ image et grille dans la session, ShowGrid, piège de l'image disparue
└── codebase-map.md                         ✏️ route /api/scene/grid
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Image PNG envoyée par `POST /api/images` | Son `id` | api |
| Happy path   | `POST /api/scene` `{ width: 45, height: 45, unit: METRE, imageId }` | 201 ; `SceneDto` avec `imageId` et `gridVisible: true` | api |
| Happy path   | `PUT /api/scene/grid { visible: false }` puis `GET /api/scene` | 200 ; `gridVisible: false` | api |
| Happy path   | Session reprise sur la même partie (`ResumeGameTest`) | Même `imageId`, même `gridVisible`, mêmes jetons | system |
| Edge case    | `POST /api/scene` sans `imageId` | Carte sans image (`imageId: null`), comme aujourd'hui | api |
| Edge case    | `imageId` inconnu | 400 « Image inconnue », la carte précédente reste | api |
| Edge case    | `PUT /api/scene/grid` sans carte | 409 | api |
| Edge case    | `PUT /api/scene/grid` pendant une décision en attente, ou en combat | 200 : l'affichage n'est pas une action de jeu | api |
| Edge case    | `PUT /api/scene/grid` sans `visible` | 400 | api |
| Edge case    | Partie gardée par l'ancien format (`create` sans `imageId`) | Se rejoue : carte sans image, grille affichée | system |
| Edge case    | Reprise d'une partie dont l'image a disparu du store | La partie se rejoue (pas mise de côté) ; `imageId` conservé, `GET /api/images/{id}` → 404 | system |
| Edge case    | Nouvelle carte après avoir masqué la grille | Grille de nouveau affichée | api |
| Teardown     | Aucun : profil de test sans fichier | — | system |

## Tasks to do

### `1)` La session porte l'image et la grille

> La carte en cours sait son image de fond et si sa grille s'affiche, et les rejoue.

1. `SceneSession` : champs `imageId` (nullable) et `gridVisible` ; `create(width, height, unit, imageId)` les pose (`gridVisible = true`) et enregistre `Create(width, height, unit, imageId, rolls)` ; `reset()` les vide.
2. `showGrid(boolean visible)` : exige une carte (`IllegalStateException` en garde interne, comme `place` ; le 409 vient de `SceneResource.requireScene()`), pas de garde de décision ni de combat ; enregistré par `recorded` en `ShowGrid(visible, rolls)`.
3. Accesseurs `imageId()` et `gridVisible()` ; rejeu de `ShowGrid` et de `Create` avec `imageId` dans `replay`.
4. `SavedGame.SavedCommand.Create` gagne `String imageId` (après `unit`) ; nouveau `record ShowGrid(boolean visible, List<Integer> rolls)` ; `JsonFileGameStore` : `@JsonSubTypes.Type(ShowGrid.class, name = "showGrid")`.

### `2)` API

> Créer une carte depuis une image et basculer la grille.

1. `Dtos.NewSceneRequest(width, height, unit, imageId)` ; `SceneResource.create` refuse un `imageId` absent de l'`ImageStore` (400) avant d'appeler la session.
2. `Dtos.SceneDto` gagne `imageId` et `gridVisible`, lus dans la session par `SceneDto.of`.
3. `PUT /api/scene/grid` avec `Dtos.GridRequest(Boolean visible)` : appelle d'abord `requireScene()` (409 sans carte), `null` → 400 ; rend le `SceneDto` ; `@APIResponse` 200, 400, 409.

### `3)` Tests et mémoire

> Verrouiller création, bascule, reprise et compatibilité ; documenter.

1. `SceneApiTest` : carte avec image, sans image, image inconnue, bascule de grille, sans carte, pendant une décision (scénario Bouclier de `DecisionApiTest`).
2. `ResumeGameTest` : image et grille reprises ; image disparue du store mémoire au rejeu.
3. `JsonFileGameStoreTest` : aller-retour avec `imageId` et `ShowGrid` ; lecture d'un JSON écrit à la main sans `imageId`.
4. `architecture.md` : image de fond et grille dans `SceneSession`, `ShowGrid` hors règles (permis pendant une décision), piège « image disparue : carte sans fond » ; `codebase-map.md` : `/api/scene/grid`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Une session reprise redonne l'image et le choix de grille de la partie ; une ancienne partie sans `imageId` se rejoue. |
| 2 | `POST /api/scene` avec une image connue rend `imageId` et `gridVisible: true` ; image inconnue → 400 ; `PUT /api/scene/grid` change `gridVisible`, même pendant une décision, et rend 409 sans carte. |
| 3 | `mvn -q test` passe ; la mémoire décrit image, grille et piège. |
