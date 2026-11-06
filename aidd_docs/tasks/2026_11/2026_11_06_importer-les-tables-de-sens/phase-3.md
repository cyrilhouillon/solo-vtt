---
status: pending
---

# Instruction: le serveur Mythic importe les tables et tire un sens

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── mythic-server/src/
    ├── main/java/mythic/server/
    │   ├── MeaningResource.java ✅
    │   ├── MeaningStore.java ✅
    │   ├── MythicDtos.java ✏️
    │   └── MythicResource.java ✏️
    └── test/java/mythic/server/
        ├── MeaningResourceTest.java ✅
        └── MythicResourceTest.java ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `CommonModuleData` en mémoire, dés scriptés, texte de mots inventés | Aucune table | api |
| Happy path   | `GET /api/mythic/meaning-tables` sans import | `{ imported: false, tables: [] }` | api |
| Happy path   | `PUT /api/mythic/meaning-tables` (texte valide) | 200, tables listées : Actions, Descriptions, puis les éléments dans l'ordre du fichier | api |
| Happy path   | `POST /api/mythic/meanings` `{ table: "ACTIONS" }`, dés 1, 57 | Table, deux mots, deux jets | api |
| Happy path   | `{ table: "ELEMENT", element: "<nom>" }` | Deux mots de cette table | api |
| Happy path   | `POST …/campaigns/{id}/mythic/questions`, tables importées, dés 33, 25, 4, 90 | `randomEvent.meaning` porte les deux mots | api |
| Edge case    | Import mal formé | 400 avec la raison et la ligne ; tables précédentes gardées | api |
| Edge case    | Tirage sans tables | 409 « Tables de sens non importées » | api |
| Edge case    | Table absente, inconnue, élément inconnu | 400 | api |
| Edge case    | Question sans tables, événement | `randomEvent.meaning` nul ; réponse inchangée | api |
| Edge case    | Document commun abîmé | Lu comme sans tables, remplacé au prochain import | api |
| Edge case    | Serveur 8081 : `PUT` en `text/plain; charset=utf-8` d'un fichier accentué (`curl --data-binary`) | 200 ; `<scratchpad>/modules/mythic.json` écrit, accents intacts au tirage | api |

## Tasks to do

### `1)` Garder les tables

> Les tables vivent dans les données communes de Mythic.

1. `MeaningStore` (sur `CommonModuleData`, id `mythic`, `@ApplicationScoped`) : `find()` → `Optional<MeaningTables>` (document illisible : vide), `save(MeaningTables)` en JSON (`{ "meaningTables": { … } }`, Jackson).

### `2)` Les routes communes

> Importer, lister, tirer.

1. `MeaningResource` `@Path("/api/mythic")` : `GET meaning-tables` (`MeaningTablesDto { imported, tables: [{ kind, element, label }] }`), `PUT meaning-tables` `@Consumes(TEXT_PLAIN)` (lit puis garde ; rend la liste), `POST meanings` (`MeaningRequest { table, element }` → `MeaningDto { table, label, first, firstRoll, second, secondRoll }`) ; sans tables : 409 avec la raison (`WebApplicationException`, rendue par l'`ErrorMapper` du cœur).
2. `@APIResponse` sur chaque route, comme `MythicResource`.

### `3)` Le sens de l'événement

> La question tire le sens d'un événement quand les tables existent.

1. `MythicResource` reçoit `MeaningStore`, passe `find()` à `FateQuestion.ask` ; `RandomEventDto` gagne `meaning` (`MeaningDto`, nul sans tables).
2. Tests (`MeaningResourceTest`, `MythicResourceTest`) en JUnit simple, `CommonModuleData` en mémoire, selon le Test Scope.
3. Assemblage : `mvn -q install -DskipTests`, serveur 8081 avec les trois chemins du scratchpad, requêtes du Test Scope.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Les tables importées se relisent par un nouveau `MeaningStore` sur les mêmes données ; un document abîmé vaut « sans tables ». |
| 2 | `mvn -q verify` vert ; import mal formé refusé en 400 sans écraser ; tirage sans tables en 409. |
| 3 | Sur 8081, un `PUT` texte accentué puis un `POST meanings` rendent les mots intacts ; une question qui déclenche un événement porte son sens. |
