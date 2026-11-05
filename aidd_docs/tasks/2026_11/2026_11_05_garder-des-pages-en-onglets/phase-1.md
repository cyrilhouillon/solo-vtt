---
status: done
---

# Instruction: le serveur garde les onglets du Carnet

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
    │   └── persistence/JsonFileNotebookStore.java ✏️
    └── test/java/solovtt/server/
        ├── NotebookApiTest.java ✏️
        ├── NotebookResourceTest.java ✅
        ├── NotebookStoreTest.java ✏️
        └── persistence/JsonFileNotebookStoreTest.java ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `Campagnes.creerUneNeuve()`, pages « Session 1 », « Bertrand », « Auberge » | Carnet sans onglet : `openPageIds` vide, `activePageId` `null` | api |
| Happy path   | `PUT …/notebook/tabs` `{ pageIds: [session, bertrand], activeId: bertrand }` puis `GET …/notebook` | 204 ; `openPageIds` = `[session, bertrand]` dans cet ordre, `activePageId` = `bertrand` | api |
| Happy path   | `PUT …/notebook/tabs` `{ pageIds: [], activeId: null }` | 204 ; aucun onglet | api |
| Happy path   | Adaptateur JSON : `setTabs`, puis un nouvel `JsonFileNotebookStore` sur le même dossier | Mêmes onglets, même actif | system |
| Edge case    | `carnet.json` d'avant : `{ "lastPageId": "<id de Session 1>" }` | `tabs()` = `[session]`, actif `session` | system |
| Edge case    | `carnet.json` qui cite une page dont la fiche n'existe pas, active ou non | `GET …/notebook` l'omet ; si c'était l'actif, `activePageId` `null` | api |
| Edge case    | `PUT …/notebook/tabs` avec une page inconnue | 404, onglets inchangés | api |
| Edge case    | Actif absent de la liste, actif `null` avec une liste non vide, un id en double, corps absent | 400, onglets inchangés | api |
| Edge case    | `carnet.json` illisible | Aucun onglet, fichier non réécrit | system |
| Teardown     | `@TempDir` effacé ; campagnes de test en mémoire | Rien sur le disque du joueur | system |

## Tasks to do

### `1)` Le port garde les onglets

> `Tabs` remplace la dernière page consultée.

1. `NotebookStore` : record `Tabs(List<String> pageIds, String activeId)` (`Tabs.NONE` vide) ; remplacer `lastPage()` / `setLastPage(id)` par `tabs()` et `setTabs(Tabs)`, qui lève `IllegalArgumentException` pour une page inconnue, un doublon, ou un actif qui n'est pas dans la liste (`null` exigé si et seulement si la liste est vide).
2. Une vérification statique partagée (`checkTabs`) appelée par les deux adaptateurs sous leur verrou.
3. `inMemory` : `AtomicReference<Tabs>`.

### `2)` Le fichier garde les onglets

> `carnet.json` garde `lastPageId` (l'actif) et gagne `openPageIds`.

1. `JsonFileNotebookStore` : `NotebookFile(String lastPageId, List<String> openPageIds)` ; lire `openPageIds` absent comme `[lastPageId]` (ou vide si `lastPageId` est `null`) ; illisible → `Tabs.NONE`, fichier intact.
2. `setTabs` vérifie chaque id par `known` (aucun chemin construit depuis un id), puis écrit atomiquement.

### `3)` La route des onglets

> Un seul `PUT` par geste du client ; le `GET` ne rend que des onglets vivants.

1. `Dtos` : `TabsRequest(List<String> pageIds, String activeId)` remplace `LastPageRequest` ; `NotebookDto(pages, openPageIds, activePageId)` remplace `lastPageId`.
2. `NotebookResource.get` : filtre les onglets sur les ids de `notebook.list()` ; actif retiré → `null`.
3. `PUT /tabs` : corps ou liste absents → 400 ; chaque id passé par `known` (404) ; puis `notebook.setTabs` (400 pour doublon ou actif). `@APIResponse` 204, 400, 404. Supprimer `PUT /last-page`.

### `4)` Tests

> Les tests de la dernière page deviennent ceux des onglets.

1. `NotebookStoreTest`, `JsonFileNotebookStoreTest` : réécrire les cas `lastPage` (vide, relu, inconnue, illisible) sur `tabs` ; ajouter l'ancien `carnet.json`, le doublon et l'actif hors liste.
2. `NotebookApiTest` : `uneCampagneNeuve_aUnCarnetVide` (`openPageIds` vide), `lesOngletsEtLActif_sontGardes`, refus 404 / 400.
3. Onglet d'une page disparue omis : le port refuse de l'écrire, donc test unitaire de `NotebookResource.get()` sur un `NotebookStore` écrit à la main (délègue à `inMemory`, `tabs()` cite un id inconnu), sans librairie de mock.
4. `mvn -q test`.

### `5)` Mémoire

> La doc dit les onglets côté serveur.

1. `architecture.md` : la « dernière page consultée » devient les onglets du Carnet (`Tabs`, `PUT …/notebook/tabs`, `carnet.json` `lastPageId` + `openPageIds`, ancien fichier lu en un onglet, pages disparues omises au `GET`) ; retirer `setLastPage` / `last-page` de la liste du port.
2. `codebase-map.md` : `notebook/last-page` → `notebook/tabs`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `setTabs` puis `tabs()` rend les mêmes ids dans le même ordre et le même actif ; page inconnue, doublon ou actif hors liste lèvent `IllegalArgumentException` sans rien changer |
| 2 | Un nouvel adaptateur relit les onglets ; un `carnet.json` à `lastPageId` seul se lit en un onglet actif ; un fichier illisible rend aucun onglet et reste intact |
| 3 | `PUT …/notebook/tabs` rend 204 puis `GET …/notebook` montre `openPageIds` et `activePageId` ; 404 pour une page inconnue, 400 pour les autres refus ; `…/last-page` n'existe plus |
| 4 | `mvn -q test` passe |
| 5 | `architecture.md` et `codebase-map.md` ne citent plus `last-page` |
