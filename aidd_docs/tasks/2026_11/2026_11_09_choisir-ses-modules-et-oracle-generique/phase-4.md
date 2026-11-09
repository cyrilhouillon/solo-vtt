---
status: done
---

# Instruction: l'oracle générique tire une ligne dans une liste du Carnet

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── vtt-module-api/src/main/java/solovtt/module/
│   └── NotebookPages.java ✏️
├── vtt-server/src/
│   ├── main/java/solovtt/server/
│   │   └── NotebookPagesAdapter.java ✏️
│   └── test/java/solovtt/server/
│       └── NotebookPagesAdapterTest.java ✅
├── mythic-server/src/test/java/mythic/server/
│   └── PagesDeTest.java ✏️
├── oracle-server/src/
│   ├── main/java/oracle/server/
│   │   ├── ListLines.java ✅
│   │   ├── OracleResource.java (créé en phase 3, modifié)
│   │   └── OracleDtos.java (créé en phase 3, modifié)
│   └── test/java/oracle/server/
│       ├── PagesDeTest.java ✅
│       └── OracleListsTest.java ✅
└── vtt-web/modules/oracle/
    ├── index.ts (créé en phase 3, modifié)
    ├── api.ts (créé en phase 3, modifié)
    ├── result.ts (créé en phase 3, modifié)
    └── OraclePanel.vue (créé en phase 3, modifié)
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Carnet en mémoire, `Dice.scripted` | Aucun hasard réel | system |
| Happy path   | `POST …/oracle/lists` deux fois | Une seule racine « Listes » dans la section `oracle`, même id | system |
| Happy path   | Sous-pages « Lieux », « Humeurs » de Listes ; `GET …/oracle/lists` | `[Humeurs, Lieux]` (par titre) | system |
| Happy path   | « Lieux » = puces « Tour », « Port », « Forêt », d3 = 2 | « Port », ligne 2, jet 2 | system |
| Happy path   | Ligne écrite en lien `[[Taverne]]` | Entrée en nœud `pageLink`, texte « Taverne » | system |
| Happy path   | Carnet : choisir Lieux, Tirer | Citation « Lieux : Port (d3 : 2) » ; un lien reste un lien | browser |
| Edge case    | Lignes vides entre les écrites | Ignorées : le dé n'a que les lignes écrites | system |
| Edge case    | Liste sans ligne écrite | 400 « La liste Lieux est vide » | system |
| Edge case    | `pageId` hors des sous-pages de Listes, ou à la corbeille | 404 « Liste inconnue » | system |
| Edge case    | Racine « Listes » renommée, puis `POST lists` | Même page reprise par son id | system |
| Edge case    | Deux campagnes : listes de A, `GET …/oracle/lists` de B | B ne voit pas les listes de A | system |
| Edge case    | Aucune page ouverte | Refus dans le panneau, aucun appel | browser |
| Edge case    | Nouvelle sous-page créée pendant que le panneau est ouvert | Proposée à l'ouverture suivante du choix | browser |
| Edge case    | `NotebookPagesAdapter.childPages` sur une page hors de la section | Vide | system |
| Teardown     | Arrêter 8081 puis 5174 | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌──────────────────────────────┐
│ Oracle                        │
│ Question / Oui/non / d100     │
│ (1) Listes                    │
│   [Lieux            v] [Tirer]│
│ (2) Dernier résultat          │
│ (3) message d'erreur          │
└──────────────────────────────┘
```

1. Choix d'une liste parmi les sous-pages de « Listes », relu à chaque ouverture du choix ; aucune liste : « Ajoutez une sous-page à Listes ».
2. Rappel du dernier tirage (`entryText`).
3. Raison d'un refus ou d'un échec.

## Tasks to do

### `1)` Le port lit les pages de sa section

> Un module lit les sous-pages et le contenu de toute page de sa section.

1. `NotebookPages.childPages(campaignId, moduleId, pageId)` : sous-pages directes vivantes d'une page de la section, triées par titre ; vide si la page n'en est pas.
2. `NotebookPages.sectionPageContent(campaignId, moduleId, pageId)` : contenu d'une page vivante de la section (racine ou descendante) ; vide sinon.
3. `NotebookPagesAdapter` (par `PageTree`), `NotebookPagesAdapterTest` ; `PagesDeTest` de Mythic implémente les deux méthodes.

### `2)` Les listes de l'oracle

> Une racine « Listes », ses sous-pages, un tirage.

1. `OracleResource` : état `{listsPageId}` dans `ModuleData` ; `POST lists` (`synchronized`, idempotent : id gardé, sinon racine titrée « Listes », sinon créée avec une liste à puces vide) ; `GET lists` → `[{id, title}]` ; `POST lists/{pageId}/draw` → `{list, roll, sides, line, entry (nœuds), entryText}`.
2. `ListLines` copié de Mythic (lecture des lignes et du texte d'un lien).
3. `OracleListsTest` : lignes system du Test Scope.

### `3)` Le panneau tire

> Choisir, tirer, écrire.

1. `index.ts` : `prepare` appelle `POST lists`.
2. `api.ts`, `result.ts` : citation « Lieux : <entrée en nœuds> (dN : n) ».
3. `OraclePanel.vue` : section Listes (choix relu au focus, Tirer, refus sans page).

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `NotebookPagesAdapterTest` vert ; Mythic inchangé (`MythicResourceTest` vert). |
| 2 | Les lignes system du Test Scope passent ; `mvn -q verify` vert. |
| 3 | Sur 5174 : les lignes browser du Test Scope ; `npm run build` vert. |
