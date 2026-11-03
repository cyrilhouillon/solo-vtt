---
status: done
---

# Instruction: caler la grille côté serveur

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── vtt-engine/src/
│   ├── main/java/solovtt/scene/Scene.java              ✏️ rescaled(facteur) : nouvelle scène, dimensions et positions multipliées
│   └── test/java/solovtt/scene/SceneTest.java          ✏️ mise à l'échelle
├── vtt-server/src/
│   ├── main/java/solovtt/server/
│   │   ├── SavedGame.java                               ✏️ commande AlignGrid(width, height, offsetX, offsetY)
│   │   ├── SceneSession.java                            ✏️ gridOffsetX/Y, alignGrid, rejeu, reset
│   │   ├── SceneResource.java                           ✏️ PUT /api/scene/alignment
│   │   └── Dtos.java                                    ✏️ AlignmentRequest ; SceneDto.gridOffsetX/Y
│   └── test/java/solovtt/server/
│       ├── SceneApiTest.java                            ✏️ calage par l'API
│       └── ResumeGameTest.java                          ✏️ calage repris à l'identique
└── aidd_docs/memory/architecture.md                     ✏️ calage de la grille
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | carte 30 × 20 m sur une image envoyée, un Loup en (3, 4,5) | `gridOffsetX` et `gridOffsetY` à 0 | api |
| Happy path   | `PUT /api/scene/alignment` `{width: 33, height: 22, gridOffsetX: 0.25, gridOffsetY: 0.5}` | 200 ; carte 33 × 22, décalages 0,25 et 0,5, Loup en (3,3, 4,95) | api |
| Happy path   | `Scene.rescaled(0.5)` | dimensions et positions divisées par 2, ids et entités gardés ; la scène d'origine ne change pas | system |
| Happy path   | caler, puis relancer la session sur la même partie | même scène, mêmes décalages, mêmes positions | system |
| Edge case    | caler pendant un combat | 409 avec la raison ; carte inchangée ; commande non gardée | api |
| Edge case    | caler pendant une décision | 409 ; décision toujours en attente | api |
| Edge case    | caler une carte sans image | 409 « carte sans image » | api |
| Edge case    | caler après « Terminer le combat » | 200 | api |
| Edge case    | largeur et hauteur d'échelles différentes (33 × 20) | 400 « cases carrées » ; carte inchangée | api |
| Edge case    | décalage hors de [0, 1), dimension ≤ 0 ou champ absent | 400 ; carte inchangée | api |
| Edge case    | aucune carte | 409 | api |
| Edge case    | `Scene.rescaled(0)` ou facteur négatif | `IllegalArgumentException` | system |

## Tasks to do

### `1)` Mettre une scène à l'échelle

> Une scène générique change d'échelle sans toucher la position relative des jetons.

1. `Scene.rescaled(double factor)` : nouvelle `Scene(width × f, height × f, unit)`, chaque jeton reposé dans l'ordre à `position × f` ; refuse `f ≤ 0` ou non fini.
2. `SceneTest` : dimensions, positions, ordre, ids gardés ; refus.

### `2)` Commande de calage

> `SceneSession` garde le décalage de la grille et applique un nouveau calage comme une commande rejouée.

1. Champs `gridOffsetX`, `gridOffsetY` (fraction de case, 0 à la création et au `reset`), lecteurs.
2. `alignGrid(width, height, offsetX, offsetY)` : refuse sans carte (`IllegalStateException`), décision en attente (`requireNoDecision`), combat ou sans image (`ActionRefusedException`) ; refuse (`IllegalArgumentException`) dimension ≤ 0, décalage hors de [0, 1), ou `width / ancienne largeur` et `height / ancienne hauteur` différents de plus de 1e-6 en relatif ; puis `scene = scene.rescaled(width / scene.width())` et décalages posés, dans `recorded(...)` avec `SavedCommand.AlignGrid`.
3. `SavedGame.SavedCommand.AlignGrid(double width, double height, double offsetX, double offsetY, List<Integer> rolls)` ; branche de `replay`.

### `3)` API

> Le client cale la grille et relit le calage.

1. `Dtos.AlignmentRequest(Double width, Double height, Double gridOffsetX, Double gridOffsetY)` ; `SceneDto` gagne `gridOffsetX`, `gridOffsetY`.
2. `SceneResource` : `PUT /api/scene/alignment`, `requireScene()` d'abord (409 sans carte : `ErrorMapper` ne mappe pas `IllegalStateException`), champ absent → 400, rend la `SceneDto` de la scène remise à l'échelle (relue dans la session, pas la `Scene` d'avant) ; `@APIResponse` 200, 400, 409.
3. `SceneApiTest`, `ResumeGameTest` : scénarios ci-dessus.

### `4)` Documentation

> La mémoire décrit le calage.

1. `architecture.md` : remplacer « sa grille tombe sur celle de la carte » (puce image de fond) par le calage ; calage = commande `AlignGrid` gardée ; carte = aire de l'image ; décalage en fraction de case, affichage et calage client ; taille de case = mise à l'échelle par `Scene.rescaled` ; refus en combat, pendant une décision, sans image. Piège : deux créatures au contact peuvent se chevaucher après un agrandissement de case.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `SceneTest` passe : une scène mise à l'échelle 0,5 a ses dimensions et positions divisées par 2, l'originale est intacte. |
| 2 | Une partie calée puis relancée redonne scène, positions et décalages identiques (`ResumeGameTest`). |
| 3 | `PUT /api/scene/alignment` rend la carte mise à l'échelle avec ses décalages ; combat, décision, carte sans image → 409 ; échelles différentes ou décalage hors bornes → 400, carte inchangée. |
| 4 | `architecture.md` décrit la commande, la représentation du décalage et les refus. |
