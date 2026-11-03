---
status: pending
---

# Instruction: enregistrer et charger une carte par l'API

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── vtt-server/src/main/java/solovtt/server/
│   ├── SceneSession.java     ✏️ snapshot(nom), load(SavedMap), mapName()
│   ├── SavedGame.java        ✏️ Create.mapName, facultatif
│   ├── MapResource.java      ✅ GET /api/maps, POST /api/maps, POST /api/maps/load
│   └── Dtos.java             ✏️ SaveMapRequest, LoadMapRequest, SavedMapDto ; SceneDto.mapName
├── vtt-server/src/test/java/solovtt/server/
│   ├── MapApiTest.java       ✅
│   ├── ResumeGameTest.java   ✏️ une carte chargée se reprend à l'identique, nom compris
│   └── persistence/JsonFileGameStoreTest.java  ✏️ Create sans mapName se relit avec null
└── aidd_docs/memory/
    ├── architecture.md       ✏️
    └── codebase-map.md       ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | carte 30 × 20 m, grille masquée, Gobelin (12 ; 1,5) et deux Loups posés | 201 | api |
| Happy path   | `POST /api/maps` `{name}` puis `GET /api/maps` | 201 ; la liste contient le nom, ses dimensions, son unité, son nombre de créatures | api |
| Happy path   | le Gobelin attaque, le combat est lancé, puis `POST /api/maps/load` `{name}` | 200, `SceneDto` : mêmes dimensions, unité, image, grille masquée, trois jetons aux positions enregistrées, PV pleins, ids `gobelin-1`, `loup-1`, `loup-2`, `mapName` égal au nom ; `/api/combat` 204 ; `/api/journal` vide | api |
| Happy path   | charger deux fois la même carte | deux `SceneDto` identiques | api |
| Happy path   | charger, puis session neuve sur la partie gardée (`ResumeGameTest`) | même scène, mêmes créatures | system |
| Edge case    | « Nouvelle carte » après un chargement | `mapName` null | api |
| Edge case    | partie gardée dont le `Create` n'a pas de `mapName` | se relit et se rejoue, `mapName` null | system |
| Edge case    | charger pendant une décision Bouclier | 200, plus de décision en attente | api |
| Edge case    | enregistrer en combat | 201 ; le combat continue, rien n'entre au journal | api |
| Edge case    | nom vide ou blanc | 400 | api |
| Edge case    | enregistrer sans carte (`SceneWithoutMapTest`) | 409 | api |
| Edge case    | charger un nom inconnu | 404 ; la carte en cours ne change pas | api |
| Edge case    | carte enregistrée dont une entrée n'est plus au catalogue (`MapStore` mémoire injecté) | 409 avec le nom de l'entrée ; la carte en cours ne change pas | system |
| Edge case    | image effacée depuis l'enregistrement | la carte se charge, `imageId` gardé (fond absent côté client, comme à la reprise) | system |

## Tasks to do

### `1)` Tirer et rejouer une carte dans la session

> La carte en cours devient un modèle ; un modèle redevient une carte neuve.

1. `snapshot(String name)` : dimensions, unité, `imageId`, `gridVisible`, et pour chaque jeton dans l'ordre de la scène l'id de son entrée (`entries`) et sa position ; sans carte, `IllegalStateException`. Ne touche ni la partie ni le journal.
2. `SavedCommand.Create` gagne `String mapName` (`null` pour une nouvelle carte et dans les parties gardées avant) ; `create` public garde sa signature et passe `null` à un `create` privé qui porte le nom ; le rejeu de `Create` le rend ; `mapName()` le lit. Information seule : aucune règle ni aucun refus n'en dépend.
3. `load(SavedMap map)` : résoudre d'abord toutes les entrées du catalogue (`ActionRefusedException` si l'une manque, avant tout changement), puis `create` avec le nom de la carte, `showGrid(false)` si la grille est masquée, `place` de chaque jeton dans l'ordre ; rend la `Scene`. Permis en combat et pendant une décision, comme `create`.

### `2)` Ressource `/api/maps`

> Exposer enregistrer, lister et charger.

1. `GET /api/maps` : `SavedMapDto(name, width, height, unitSymbol, imageId, creatureCount)` par nom.
2. `POST /api/maps` `{name}` : nom rogné, non vide, 80 caractères au plus (400 sinon) ; 409 sans carte ; `MapStore.save(session.snapshot(name))` ; 201 avec le `SavedMapDto`.
3. `POST /api/maps/load` `{name}` : 404 si inconnu ; sinon `SceneDto` de `session.load(map)`.
4. `SceneDto` gagne `mapName` (`null` hors carte chargée).
5. `@APIResponse` sur chaque code.

### `3)` Tests et documentation

> Verrouiller la story côté serveur.

1. `MapApiTest` (noms de carte uniques par test : le `MapStore` mémoire vit le temps de la JVM) et le cas `ResumeGameTest`.
2. `architecture.md` : décision « cartes enregistrées » (modèle, port `MapStore`, chargement en commandes ordinaires : la partie copie la carte, ne la référence pas, et n'en garde le nom qu'à titre d'information ; même nom remplacé) ; piège : une image effacée laisse la carte se charger sans fond. `codebase-map.md` : `MapResource`, `MapStore`, `JsonFileMapStore`, `MapStoreProducer`, routes `maps` et `maps/load`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Une carte chargée a les créatures enregistrées, neuves, aux mêmes positions, sans combat ni journal, et porte son nom, rejoué à la reprise ; une entrée manquante laisse la carte en cours intacte ; une partie gardée sans nom se rejoue. |
| 2 | Les codes 201, 200, 400, 404, 409 des scénarios sont rendus. |
| 3 | `mvn -q test` passe ; la carte chargée se reprend à l'identique après relance. |
