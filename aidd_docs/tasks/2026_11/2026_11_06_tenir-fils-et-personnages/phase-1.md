---
status: done
---

# Instruction: le cœur range des pages dans la section d'un module

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── aidd_docs/memory/
│   ├── architecture.md ✏️
│   └── codebase-map.md ✏️
├── vtt-module-api/src/main/java/solovtt/module/
│   └── NotebookPages.java ✅
└── vtt-server/src/
    ├── main/java/solovtt/server/
    │   ├── Page.java ✏️
    │   ├── PageTree.java ✏️
    │   ├── NotebookStore.java ✏️
    │   ├── NotebookResource.java ✏️
    │   ├── Dtos.java ✏️
    │   ├── NotebookPagesAdapter.java ✅
    │   └── persistence/JsonFileNotebookStore.java ✏️
    └── test/java/solovtt/server/
        ├── NotebookStoreTest.java ✏️
        ├── NotebookApiTest.java ✏️
        ├── PageLinksTest.java ✏️
        ├── PageSearchTest.java ✏️
        ├── PageTreeTest.java ✏️
        └── persistence/JsonFileNotebookStoreTest.java ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Carnet en mémoire, puis sur `@TempDir` | Vide | system |
| Happy path   | `createSectionRoot("mythic", "Fils", doc)` | Page à la racine, `section = "mythic"`, contenu gardé ; `list()` la rend | system |
| Happy path   | Sous-page créée sous Fils, déplacée à la racine et de retour, supprimée puis restaurée | Tout permis ; sa `section` reste `null` | system |
| Happy path   | `rename` de Fils | Permis | system |
| Happy path   | Nouvel adaptateur JSON sur le même dossier | La racine se relit avec sa section | system |
| Edge case    | `move` ou `delete` d'une racine de section | `IllegalArgumentException`, rien ne change | system |
| Edge case    | Section `../x` ou `A` | `IllegalArgumentException` | system |
| Edge case    | Fiche d'avant, sans `section` | Lue avec `section = null` | system |
| Happy path   | Port `NotebookPages` injecté : création, puis `GET …/notebook` | `pages[].section` vaut `mythic` pour la racine, `null` ailleurs | api |
| Edge case    | `PUT …/pages/{racine}/parent`, `DELETE …/pages/{racine}` | 400 avec la raison | api |
| Edge case    | `sectionRoots` d'une campagne inconnue | `IllegalArgumentException` | system |

## Tasks to do

### `1)` Le marqueur de section

> Une page peut être la racine de la section d'un module.

1. `Page(id, title, parentId, section, content)` ; Javadoc : `section`, l'id du module dont elle est une racine de section, `null` pour toute autre page ; une page est dans une section si sa racine en est une.
2. Mettre à jour chaque `new Page(` (main et tests) en gardant la section.
3. `JsonFileNotebookStore` : `PageFile.section` (`NON_NULL`), lu et écrit.

### `2)` Les règles de la section

> Seul un module crée une racine de section, qui ne se déplace ni ne se supprime.

1. `NotebookStore.createSectionRoot(String section, String title, String content)` : à la racine, id de section vérifié par `CampaignStore.checkModuleId` ; `inMemory` et `JsonFileNotebookStore`.
2. `move` et `delete` refusent une racine de section (`IllegalArgumentException` : « Une page de section ne se déplace pas » / « ne se supprime pas »), dans les deux adaptateurs, par un contrôle commun de `NotebookStore`.
3. `NotebookResource` : `@APIResponse 400` complétés sur `parent` et `DELETE`.
4. `Dtos.PageSummaryDto`, `PageDto` et `SearchResultDto` gagnent `section` : côté client, `PageView` et `SearchResult` étendent `PageSummary` (`types.ts` l. 66, 71).

### `3)` Le port des modules

> Un module lit et crée les racines de sa section, sans connaître le stockage.

1. `solovtt.module.NotebookPages` : `List<SectionPage> sectionRoots(String campaignId, String moduleId)`, `SectionPage createSectionRoot(String campaignId, String moduleId, String title, String contentJson)`, `record SectionPage(String id, String title)` ; Javadoc des refus.
2. `NotebookPagesAdapter` (`@ApplicationScoped`) sur `CampaignStore.notebook`, comme `ModuleDataAdapter` ; campagne inconnue : `IllegalArgumentException`.

### `4)` Tests

> Chaque ligne du Test Scope.

1. `NotebookStoreTest`, `JsonFileNotebookStoreTest`, `NotebookApiTest` (port injecté par `@Inject NotebookPages`).

### `5)` Mémoire

> La doc dit le marqueur et le port.

1. `architecture.md` : marqueur de section, règles, port `NotebookPages`.
2. `codebase-map.md` : `NotebookPages`, `NotebookPagesAdapter`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Une racine de section garde sa section après relecture ; une fiche d'avant se lit sans section. |
| 2 | Déplacer ou supprimer une racine de section rend 400 ; ses sous-pages vont et viennent librement. |
| 3 | `vtt-module-api` compile sans dépendance ; le port injecté crée une racine visible dans `GET …/notebook`. |
| 4 | `mvn -q test -pl vtt-server -am` vert. |
| 5 | La mémoire décrit le marqueur, ses règles et le port. |
