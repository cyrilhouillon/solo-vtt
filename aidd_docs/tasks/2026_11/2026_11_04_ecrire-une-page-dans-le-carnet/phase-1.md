---
status: pending
---

# Instruction: garder les pages du Carnet sur le serveur

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── aidd_docs/memory/
│   ├── architecture.md ✏️
│   └── codebase-map.md ✏️
└── vtt-server/src/
    ├── main/java/solovtt/server/
    │   ├── CampaignResource.java ✏️
    │   ├── CampaignStore.java ✏️
    │   ├── Dtos.java ✏️
    │   ├── NotebookResource.java ✅
    │   ├── NotebookStore.java ✅
    │   ├── Page.java ✅
    │   └── persistence/
    │       ├── FileCampaignStore.java ✏️
    │       └── JsonFileNotebookStore.java ✅
    └── test/java/solovtt/server/
        ├── CampaignStoreTest.java ✏️
        ├── NotebookApiTest.java ✅
        ├── NotebookStoreTest.java ✅
        └── persistence/
            ├── FileCampaignStoreTest.java ✏️
            └── JsonFileNotebookStoreTest.java ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `Campagnes.creerUneNeuve()` dans le `@BeforeEach` ; `@TempDir` pour l'adaptateur | Une campagne sans page | system |
| Happy path   | `GET …/notebook` sur une campagne neuve | `{ pages: [], lastPageId: null }` | api |
| Happy path   | `POST …/notebook/pages` `{ title: " Session 1 " }` | 201, `{ id, title: "Session 1", content: null }` ; la page apparaît dans `GET …/notebook` | api |
| Happy path   | `PUT …/notebook/pages/{id}/content` avec un document `{ type: "doc", content: [...] }`, puis `GET …/notebook/pages/{id}` | 204, puis le même document rendu à l'identique | api |
| Happy path   | `PUT …/notebook/last-page` `{ pageId }`, puis `GET …/notebook` | 204, puis `lastPageId` = cette page | api |
| Happy path   | Créer deux pages et leur contenu, poser la dernière page, puis relire par un nouvel adaptateur sur le même dossier | Pages, contenus et dernière page relus à l'identique | system |
| Edge case    | Titre absent, vide, blanc ou de plus de 120 caractères | 400 avec la raison | api |
| Edge case    | Contenu absent, non objet, ou sans `type: "doc"` | 400, la page garde son contenu d'avant | api |
| Edge case    | Page inconnue (`GET`, `PUT …/content`, `PUT last-page`), ou id qui n'est pas celui d'une page (`../campagne`) | 404, aucun fichier lu ni écrit hors de `carnet/` | api |
| Edge case    | Campagne inconnue sur `…/notebook` | 404 | api |
| Edge case    | Deux campagnes | Les pages de l'une n'apparaissent pas dans l'autre | api |
| Edge case    | Fiche de page illisible dans `carnet/` | La page est ignorée de la liste, la fiche n'est jamais écrasée | system |
| Teardown     | `@TempDir` effacé ; campagnes en mémoire en profil de test | Aucun fichier du joueur touché | system |

## Tasks to do

### `1)` Le port du Carnet

> Une campagne garde ses pages et sa dernière page consultée, derrière un port, en mémoire par défaut.

1. `Page` : record `id`, `title`, `content` (texte JSON du document Tiptap, `null` pour une page neuve).
2. `NotebookStore` : `list()` (pages triées par titre, insensible à la casse), `find(id)`, `create(title)` (id UUID), `saveContent(id, content)`, `lastPage()` (`Optional<String>`), `setLastPage(id)` ; `IllegalArgumentException` sur une page inconnue ; `NotebookStore.inMemory()`.
3. `CampaignStore.notebook(id)` : `inMemory` garde un `NotebookStore.inMemory()` par campagne ; `IllegalArgumentException` si la campagne est inconnue.

### `2)` L'adaptateur sur disque

> Chaque page est un fichier de la campagne, écrit atomiquement ; une fiche abîmée n'est jamais écrasée.

1. `persistence.JsonFileNotebookStore(Path dir)` sur `<campagne>/carnet/` : `<pageId>.json` (`id`, `title`, `content` en objet JSON imbriqué, pas en chaîne) et `carnet.json` (`lastPageId`), par fichier voisin `.tmp` puis déplacement atomique.
2. Un id n'entre dans un chemin qu'après avoir été trouvé parmi les pages lues (comme `FileCampaignStore.dirOf`).
3. Fiche illisible ou incohérente (id ≠ nom du fichier) : ignorée avec un avertissement, jamais réécrite ; `carnet.json` illisible : `lastPage()` vide.
4. `FileCampaignStore.notebook(id)` rend une seule instance par campagne (cache), pour que `synchronized` protège ses écritures.

### `3)` Les routes du Carnet

> Le client lit le Carnet, crée une page, enregistre son contenu et note la dernière page consultée.

1. `CampaignResource` : localisateur `/{campaignId}/notebook` → `new NotebookResource(campaigns.notebook(id))` après `campaigns.find(id)`, qui lève `NotFoundException` (404) si la campagne est inconnue ; sans passer par `CampaignSessions`, qui rejouerait la partie.
2. `NotebookResource` : `GET` → `NotebookDto(pages: PageSummaryDto[id, title], lastPageId)` ; `POST /pages` (`PageTitleRequest`, titre nettoyé, 1 à 120 caractères) → 201 `PageDto(id, title, content)` ; `GET /pages/{pageId}` → `PageDto` ; `PUT /pages/{pageId}/content` (`PageContentRequest(JsonNode content)`, objet de `type` `"doc"`) → 204 ; `PUT /last-page` (`LastPageRequest(pageId)`) → 204 ; page inconnue → `NotFoundException` levée par la ressource après `find(pageId)` (404), car `ErrorMapper` rend 400 pour une `IllegalArgumentException`.
3. `@APIResponse` sur chaque route, comme `MapResource`.

### `4)` Mémoire

> La mémoire du projet décrit le Carnet côté serveur.

1. `architecture.md` : port `NotebookStore`, `carnet/` (une fiche par page, `carnet.json`), contenu JSON Tiptap opaque, dernière page gardée par le serveur ; piège : pas de verrou entre deux onglets, le dernier enregistrement l'emporte.
2. `codebase-map.md` : `Page`, `NotebookStore`, `JsonFileNotebookStore`, `NotebookResource`, routes `notebook`, `notebook/pages`, `notebook/pages/{id}/content`, `notebook/last-page`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `NotebookStoreTest` et `CampaignStoreTest` : créer, lister par titre, enregistrer un contenu, poser la dernière page ; page ou campagne inconnue lève `IllegalArgumentException` ; deux campagnes ont des Carnets séparés. |
| 2 | `JsonFileNotebookStoreTest` et `FileCampaignStoreTest` : un nouvel adaptateur relit pages, contenus et dernière page ; une fiche illisible est ignorée et garde ses octets ; un id hors des pages lues ne produit aucun chemin. |
| 3 | `NotebookApiTest` : chaque ligne du Test Scope « api » passe ; `mvn -q test` vert, tests existants compris. |
| 4 | Les deux fichiers de mémoire nomment le port, l'adaptateur, les routes et le format. |
