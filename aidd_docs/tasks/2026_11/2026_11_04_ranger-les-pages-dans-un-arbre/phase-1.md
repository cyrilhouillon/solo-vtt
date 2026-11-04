---
status: done
---

# Instruction: garder l'arbre des pages sur le serveur

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
    │   ├── Page.java ✏️
    │   ├── PageTree.java ✅
    │   └── persistence/
    │       └── JsonFileNotebookStore.java ✏️
    └── test/java/solovtt/server/
        ├── CampaignStoreTest.java ✏️
        ├── NotebookApiTest.java ✏️
        ├── NotebookStoreTest.java ✏️
        ├── PageTreeTest.java ✅
        └── persistence/
            ├── FileCampaignStoreTest.java ✏️
            └── JsonFileNotebookStoreTest.java ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `Campagnes.creerUneNeuve()` dans le `@BeforeEach` ; `@TempDir` pour l'adaptateur | Une campagne sans page | system |
| Happy path   | `POST …/notebook/pages` `{ title: "PNJ" }`, puis `{ title: "Bertrand", parentId: <PNJ> }` | 201 ; `GET …/notebook` rend PNJ (`parentId: null`) et Bertrand (`parentId: <PNJ>`) | api |
| Happy path   | `PUT …/pages/{id}/title` `{ title: " Bertrand le Rouge " }` | 204 ; titre nettoyé dans `GET …/notebook` et `GET …/pages/{id}`, contenu inchangé | api |
| Happy path   | Lore > Villes > Barovie ; `PUT …/pages/{Villes}/parent` `{ parentId: <PNJ> }`, puis `{ parentId: null }` | 204 ; Villes sous PNJ, Barovie toujours sous Villes ; puis Villes à la racine | api |
| Happy path   | Arbre de trois niveaux, titres et contenus, relu par un nouvel adaptateur sur le même dossier | Parents, titres et contenus à l'identique | system |
| Edge case    | Déplacer une page sous elle-même, ou sous une de ses sous-pages (petite-fille comprise) | 400 avec la raison ; arbre inchangé | api |
| Edge case    | Créer ou déplacer sous un parent inconnu ; renommer ou déplacer une page inconnue | 404 ; rien n'est écrit | api |
| Edge case    | Renommer avec un titre absent, blanc ou de plus de 120 caractères | 400 ; titre inchangé | api |
| Edge case    | Fiche écrite avant cette story (sans `parentId`) | Lue à la racine | system |
| Edge case    | Fiche dont le parent est illisible, ou deux fiches qui se citent (éditées à la main) | `GET …/notebook` les rend à la racine (`parentId: null`) ; aucune fiche réécrite | system |
| Teardown     | `@TempDir` effacé ; campagnes en mémoire en profil de test | Aucun fichier du joueur touché | system |

## Tasks to do

### `1)` Le parent d'une page

> Chaque page connaît sa page parente, `null` à la racine.

1. `Page` : ajouter `parentId` (après `title`), Javadoc : `null` à la racine.
2. `PageTree` (final, statique) : `isWithin(pages, id, ancestorId)` remonte les parents de `id` (borné par le nombre de pages) et dit s'il croise `ancestorId` ; `rooted(pages)` rend les pages dont le parent est inconnu ou dont la chaîne boucle avec `parentId` à `null`.
3. `PageTreeTest` : sous soi-même, sous une petite-fille, sous une sœur ; parent inconnu et boucle remis à la racine.

### `2)` Le port range, renomme et déplace

> Créer sous un parent, renommer, déplacer, sans boucle possible.

1. `NotebookStore` : `create(title, parentId)` (parent inconnu : `IllegalArgumentException`) ; `rename(id, title)` ; `move(id, parentId)` (`IllegalArgumentException` pour page ou parent inconnu, et pour `PageTree.isWithin(list, parentId, id)`).
2. `inMemory` : `create`, `rename`, `move` sous un même verrou ; `saveContent` garde le parent.
3. `JsonFileNotebookStore` : `PageFile` gagne `parentId` ; `read`, `write`, `saveContent` le portent ; `rename` et `move` réécrivent la seule fiche de la page, dans `synchronized`.
4. Adapter `NotebookStoreTest`, `JsonFileNotebookStoreTest`, `CampaignStoreTest`, `FileCampaignStoreTest` au nouveau `Page` et à `create(title, null)` ; ajouter les cas du Test Scope (port en mémoire et adaptateur : relecture, fiche sans `parentId`).

### `3)` Les routes de l'arbre

> Le client crée sous une page, renomme et déplace par l'API.

1. `Dtos` : `PageTitleRequest` (sert aussi au renommage) et `CreatePageRequest(title, parentId)` ; `ParentRequest(parentId)` ; `PageSummaryDto` et `PageDto` gagnent `parentId` (le client hérite `PageView` de `PageSummary`).
2. `NotebookResource` : un seul nettoyage de titre partagé par création et renommage ; `POST /pages` lit `parentId` (inconnu : 404) ; `PUT /pages/{pageId}/title` ; `PUT /pages/{pageId}/parent` (page ou parent inconnu : 404, boucle : 400) ; `GET` passe la liste par `PageTree.rooted`. `@APIResponse` sur chaque route.
3. `NotebookApiTest` : les cas api du Test Scope.

### `4)` Mémoire du projet

> La mémoire décrit l'arbre côté serveur.

1. `architecture.md` : `parentId`, boucle refusée dans le port, page à parent illisible rendue à la racine sans réécriture, routes `title` et `parent`.
2. `codebase-map.md` : `PageTree`, routes `notebook/pages/{id}/title` et `…/parent`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `PageTreeTest` passe : boucles détectées à toute profondeur, pages orphelines ou en boucle rendues à la racine. |
| 2 | `mvn -q test` passe ; un arbre écrit se relit à l'identique par un nouvel adaptateur ; une fiche sans `parentId` se lit à la racine ; un déplacement qui boucle ne change aucune fiche. |
| 3 | `NotebookApiTest` passe : création sous un parent, renommage nettoyé, déplacement avec sous-pages qui suivent, 400 pour une boucle ou un titre invalide, 404 pour une page ou un parent inconnu. |
| 4 | La mémoire nomme `parentId`, `PageTree` et les deux routes. |
