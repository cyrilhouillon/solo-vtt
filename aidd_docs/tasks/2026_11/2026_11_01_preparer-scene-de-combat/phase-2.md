---
status: done
---

# Instruction: serveur Quarkus de la scène

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── pom.xml                                         ✏️ module vtt-server, BOM Quarkus en dependencyManagement
└── vtt-server/
    ├── pom.xml                                     ✅ quarkus-rest-jackson, quarkus-junit5, rest-assured (test), plugin quarkus-maven-plugin
    ├── src/main/resources/application.properties   ✅ port 8080, CORS limité à l'origine Vite en dev
    ├── src/main/java/solovtt/server/
    │   ├── SceneSession.java                       ✅ @ApplicationScoped : Scene + GameState courants, compteur d'instances
    │   ├── CatalogResource.java                    ✅ GET /api/catalog
    │   ├── SceneResource.java                      ✅ POST/GET /api/scene, POST /api/scene/tokens, GET /api/scene/distance
    │   ├── CreatureResource.java                   ✅ GET /api/creatures/{id} (feuille)
    │   ├── Dtos.java                               ✅ records JSON (requêtes et réponses)
    │   └── ErrorMapper.java                        ✅ IllegalArgumentException → 400 { message }
    └── src/test/java/solovtt/server/
        └── SceneApiTest.java                       ✅ @QuarkusTest
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `POST /api/scene {width:30,height:20,unit:"METRE"}` | 201, scène vide | api |
| Happy path   | `GET /api/catalog` | 5 entrées avec id, nom, type | api |
| Happy path   | `POST /api/scene/tokens {catalogId:"gobelin",x:3,y:4}` deux fois | 201, jetons `gobelin-1`, `gobelin-2` | api |
| Happy path   | `GET /api/scene/distance?from=…&to=…` | `{ value, unit }` exact | api |
| Happy path   | `GET /api/creatures/gobelin-1` | feuille : nom, CA, PV/PV max, vitesse, attaque, résistances, vulnérabilités | api |
| Edge case    | jeton hors carte, catalogue inconnu, créature inconnue | 400 / 404 avec message en français | api |
| Edge case    | ajouter un jeton sans scène créée | 409 « aucune carte » | api |
| Edge case    | recréer une scène | l'ancienne scène et ses créatures disparaissent | api |
| Teardown     | `POST /api/scene` entre deux tests | scène vide | api |

## Tasks to do

### `1)` Module vtt-server

> Un serveur Quarkus démarre et embarque les deux moteurs, sans leur ajouter de dépendance.

1. Ajouter le module et le BOM Quarkus (version stable courante, figée) au `pom.xml` racine.
2. Créer `vtt-server/pom.xml` (dépend de `dnd-engine`) et `application.properties`.
3. Vérifier `mvn -q dependency:tree -pl dnd-engine` : toujours aucune dépendance runtime hors `vtt-engine`.

### `2)` Session de scène

> Le serveur tient une scène et l'état D&D qui l'accompagne.

1. `SceneSession` : `create(width, height, unit)`, `place(catalogId, x, y)` qui crée la `Creature` via `SrdCatalog` (id `<catalogId>-<n>`), l'ajoute au `GameState` et pose le jeton (`entityId` = id de créature), méthodes synchronisées.

### `3)` API REST

> Le client pilote la préparation en HTTP JSON.

1. Écrire les ressources et les DTO du Test Scope ; la feuille est construite depuis `Creature` dans `Dtos`.
2. Les ressources testent l'existence (scène, créature, entrée de catalogue) avant d'appeler le domaine et lèvent `NotFoundException` (404) ou `ClientErrorException` 409 « aucune carte » ; `ErrorMapper` ne traduit en 400 que les `IllegalArgumentException` restantes (bornes, unicité). Raison : `GameState.get` lève aussi `IllegalArgumentException` pour un id inconnu.
3. Écrire `SceneApiTest` couvrant le Test Scope.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `mvn -q verify` passe à la racine ; `mvn quarkus:dev -pl vtt-server` répond sur `http://localhost:8080/api/catalog` |
| 2 | deux placements d'une même entrée donnent deux créatures distinctes dans le `GameState` |
| 3 | `SceneApiTest` passe ; chaque erreur rend un code HTTP et un message lisible |
