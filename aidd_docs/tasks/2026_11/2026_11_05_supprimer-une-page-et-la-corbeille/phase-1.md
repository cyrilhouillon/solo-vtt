---
status: done
---

# Instruction: le serveur met les pages à la corbeille

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── aidd_docs/memory/
│   ├── architecture.md ✏️
│   └── codebase-map.md ✏️
└── vtt-server/src/
    ├── main/java/solovtt/server/
    │   ├── Dtos.java ✏️
    │   ├── NotebookResource.java ✏️
    │   ├── NotebookStore.java ✏️
    │   ├── PageLinks.java ✏️
    │   ├── PageTree.java ✏️
    │   └── persistence/JsonFileNotebookStore.java ✏️
    └── test/java/solovtt/server/
        ├── NotebookApiTest.java ✏️
        ├── NotebookStoreTest.java ✏️
        ├── PageLinksTest.java ✏️
        ├── PageTreeTest.java ✏️
        └── persistence/JsonFileNotebookStoreTest.java ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `Campagnes.creerUneNeuve()` ; PNJ > Bertrand > Fils ; Sessions > Session 1 (lie Bertrand et Fils) ; Auberge (lie Fils) | Six pages, corbeille vide | api |
| Happy path   | `GET …/pages/{bertrand}/deletion` | `subpageCount` 1, `citingCount` 2 (Session 1, Auberge ; ni Bertrand ni Fils) | api |
| Happy path   | `DELETE …/pages/{bertrand}` | 204 ; `GET …/notebook` sans Bertrand ni Fils ; `GET …/notebook/trash` = une entrée Bertrand, `subpageCount` 1 ; citations de Bertrand et Fils : 404 ; recherche « Fils » : seulement les pages vivantes | api |
| Happy path   | Onglets Bertrand, Session 1 (actif Bertrand), puis supprimer Bertrand | `GET …/notebook` : `openPageIds` = `[session]`, `activePageId` `null` | api |
| Happy path   | `POST …/notebook/trash/{bertrand}/restore` | 204 ; Bertrand sous PNJ, Fils sous Bertrand, contenus intacts ; corbeille vide ; `…/pages/{session}/citing`… inchangés | api |
| Edge case    | Supprimer PNJ alors que Bertrand est à la corbeille, puis restaurer Bertrand | Bertrand revient à la racine (`parentId` `null`), Fils sous lui ; PNJ reste à la corbeille | api |
| Edge case    | Supprimer Fils, puis Bertrand, puis restaurer Bertrand | Bertrand revient sans Fils ; Fils reste une entrée à part | api |
| Edge case    | Supprimer PNJ, vider la corbeille, restaurer Bertrand (déjà vidé) | `DELETE …/notebook/trash` 204, corbeille vide ; restaurer : 404 | api |
| Edge case    | Contenu, titre, parent, sous-page ou onglet sur une page à la corbeille ; supprimer une page inconnue ou déjà supprimée ; restaurer un id qui n'est pas une entrée (sous-page Fils, page vivante) | 404, rien ne change | api |
| Edge case    | Adaptateur JSON : supprimer, puis nouvel adaptateur sur le même dossier ; fiche d'avant sans `deletedWith` | Corbeille relue (titre, date, sous-pages) ; la fiche d'avant est vivante | system |
| Edge case    | Fiches écrites à la main avec deux `deletedAt` différents | `trash()` rend la plus récente d'abord | system |
| Edge case    | Vider : une fiche à la corbeille et une fiche illisible | Seule la fiche à la corbeille est effacée ; l'illisible reste | system |
| Teardown     | `@TempDir` effacé ; campagnes de test en mémoire | Rien sur le disque du joueur | system |

## Tasks to do

### `1)` L'arbre et les liens d'un lot

> Calculer ce qu'une suppression emporte.

1. `PageTree.branch(pages, id)` : la page et toutes ses sous-pages (`Set<String>`), protégé contre une boucle comme `isWithin`.
2. `PageLinks.citingAny(pages, ids)` : les pages hors de `ids` qui lient l'une d'elles ; `citing(pages, id)` délègue à `citingAny(pages, Set.of(id))`.
3. Tests dans `PageTreeTest` et `PageLinksTest`.

### `2)` Le port met à la corbeille

> `list()` et `find()` ne voient que les pages vivantes ; la corbeille garde les lots.

1. `NotebookStore` : `delete(id)` (page vivante et ses sous-pages vivantes marquées du même lot et de la même date ; `IllegalArgumentException` si inconnue), `trash()` (`List<TrashEntry>`, `record TrashEntry(Page page, Instant deletedAt, int subpageCount)`, la plus récente d'abord), `restore(id)` (démarque le lot ; la page reprend son parent s'il est vivant, sinon la racine ; `IllegalArgumentException` si `id` n'est pas une entrée), `emptyTrash()`.
2. `inMemory` : pages à la corbeille dans une seconde table (page, lot, date).
3. `JsonFileNotebookStore` : `PageFile` gagne `deletedWith` et `deletedAt` (`String` ISO-8601 par `Instant.toString()` : le mapper de `JsonFileNotebookStore.java:37` n'a pas de module `java.time` ; absents, un record les lit `null` : vivante) ; `list()` et `find()` ne rendent que les vivantes ; une lecture commune donne aussi les marquées ; `delete` et `restore` réécrivent chaque fiche du lot atomiquement ; `emptyTrash` efface les fiches marquées, jamais une illisible.

### `3)` Les routes

> Confirmer, supprimer, lister, restaurer, vider.

1. `Dtos` : `DeletionDto(int subpageCount, int citingCount)`, `TrashedPageDto(String id, String title, String deletedAt, int subpageCount)` (ISO-8601).
2. `NotebookResource` : `GET /pages/{id}/deletion` (sur `list()`, `PageTree.branch`, `PageLinks.citingAny`), `DELETE /pages/{id}` 204, `GET /trash`, `POST /trash/{id}/restore` 204 (404 si l'id n'est pas une entrée de `trash()`), `DELETE /trash` 204 ; `@APIResponse` sur chacune, 404 pour une page inconnue.

### `4)` Tests

> Chaque ligne du Test Scope.

1. `NotebookApiTest` : confirmation, suppression, onglets, restauration, place disparue, lots séparés, vider, refus 404.
2. `NotebookStoreTest` et `JsonFileNotebookStoreTest` : mêmes règles sur le port ; relecture, fiche d'avant, ordre par date, fiche illisible gardée.
3. `mvn -q test`.

### `5)` Mémoire

> La doc dit la corbeille côté serveur.

1. `architecture.md` : port `NotebookStore` (`delete`, `trash`, `restore`, `emptyTrash`), fiche marquée `deletedWith` / `deletedAt`, routes, restauration à la racine, vider sans toucher aux images.
2. `codebase-map.md` : routes `notebook/pages/{id}/deletion`, `notebook/trash`, `notebook/trash/{id}/restore`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `branch` rend la page et ses sous-pages à toute profondeur ; `citingAny` exclut les pages du lot ; `citing` inchangé |
| 2 | Une page supprimée et ses sous-pages disparaissent de `list()`, `find()`, des citations et de la recherche ; restaurées, elles reviennent intactes, à la racine si le parent n'est plus vivant ; vidées, elles ne reviennent plus |
| 3 | Chaque route rend 204 / 200 comme décrit, 404 pour une page ou une entrée inconnue ; Swagger les décrit |
| 4 | `mvn -q test` passe |
| 5 | `architecture.md` et `codebase-map.md` décrivent la corbeille et ses routes |
