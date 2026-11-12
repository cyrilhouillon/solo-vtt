---
status: pending
---

# Instruction: entrées de module dans le Carnet

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── vtt-module-api/src/main/java/solovtt/module/
│   └── NotebookPages.java                         ✏️ createEntry, isEntry
├── vtt-server/src/main/java/solovtt/server/
│   ├── Page.java                                  ✏️ entryOf, isEntry
│   ├── NotebookStore.java                         ✏️ createEntry, refus généralisés aux entrées
│   ├── NotebookPagesAdapter.java                  ✏️ createEntry, isEntry
│   ├── Dtos.java                                  ✏️ entryOf dans PageSummaryDto, PageDto, SearchResultDto
│   ├── NotebookResource.java                      ✏️ PageDto avec entryOf
│   └── persistence/JsonFileNotebookStore.java     ✏️ entryOf dans la fiche (NON_NULL)
├── vtt-server/src/test/java/solovtt/server/
│   ├── NotebookStoreTest.java                     ✏️
│   ├── NotebookApiTest.java                       ✏️
│   └── persistence/ (test du JsonFileNotebookStore) ✏️
└── vtt-web/src/
    ├── types.ts                                   ✏️ entryOf
    ├── modules/notebookModule.ts                  ✏️ entryView
    ├── notebook/tree.ts                           ✏️ canMove refuse une entrée comme parent
    ├── components/PageBranch.vue                  ✏️ pas de « Nouvelle sous-page » sous une entrée
    └── components/NotebookScreen.vue              ✏️ monte l'entryView du module
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | campagne neuve, un module de test qui crée une racine de section puis une entrée dessous par `NotebookPages.createEntry` | l'entrée est listée par `GET …/notebook` avec `entryOf` | api |
| Happy path   | renommer, déplacer, supprimer puis restaurer l'entrée ; la chercher par son titre | chaque geste réussit comme pour une page ; la recherche la trouve | api |
| Edge case    | créer une page sous l'entrée, déplacer une page sous elle, écrire son contenu | 400 à chaque fois, rien ne change | api |
| Edge case    | relire le Carnet après redémarrage (`JsonFileNotebookStore` sur `@TempDir`) | l'entrée revient avec `entryOf` ; une fiche d'avant, sans `entryOf`, se lit en page ordinaire | system |
| Edge case    | `createEntry` sous une page hors de la section du module, ou avec un id de module invalide | `IllegalArgumentException` | system |
| Happy path   | `npm run build` | typage et build verts ; les entrées de combat s'affichent comme avant | cli |

## Tasks to do

### `1)` Porter l'entrée de module côté serveur

> Une page peut être l'entrée d'un module, sans texte ni sous-page, que le module crée sous une page de sa section.

1. `Page` : champ `entryOf` (id du module, `null` sinon) ; `isEntry()` vrai pour une entrée de combat ou de module ; constructeurs et `with…` le gardent.
2. `NotebookStore.checkParent` et `checkContent` refusent toute entrée (`isEntry`) ; le message d'une entrée de combat reste tel quel (`NotebookStoreTest.java:247` le vérifie), une entrée de module a le sien.
3. `NotebookStore.createEntry(moduleId, parentId, title)` dans l'implémentation mémoire et `JsonFileNotebookStore` : parent vivant de la section du module, sinon `IllegalArgumentException`.
4. `JsonFileNotebookStore.PageFile` : `entryOf` en `NON_NULL`, lu et écrit.
5. `NotebookPages` : `createEntry(campaignId, moduleId, parentId, title)` qui rend la `SectionPage` créée, `isEntry(campaignId, moduleId, pageId)` vrai pour une entrée vivante de ce module ; `NotebookPagesAdapter` les implémente.
6. `Dtos.PageSummaryDto`, `PageDto`, `SearchResultDto` et `NotebookResource` portent `entryOf`.
7. Tests : création, refus (sous-page, contenu, déplacement sous l'entrée, parent hors section), renommage, corbeille et restauration, recherche par titre, relecture fichier.

### `2)` Montrer l'entrée par le module côté client

> Le Carnet ouvre une entrée de module dans la vue que ce module déclare.

1. `types.ts` : `entryOf: string | null` dans `PageSummary`.
2. `NotebookModule.entryView?: Component`, monté avec les props `campaignId` et `pageId`, et un événement `error`.
3. `NotebookContext.openPage(pageId)` : relit le Carnet puis ouvre la page (par `open`, qui attend l'enregistrement), pour qu'un module ouvre ce qu'il vient de créer.
4. `tree.ts` (`canMove`) et `PageBranch.vue` traitent une entrée de module comme une entrée de combat : ni parent, ni « Nouvelle sous-page ».
5. `NotebookScreen.vue` : pour une page à `entryOf`, l'article montre le titre, puis l'`entryView` du module actif, sinon « Module <id> inactif dans cette campagne. », puis `CitingPages`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Une entrée de module se crée sous sa section, se renomme, se déplace, va à la corbeille et en revient, se trouve par son titre ; sous-page, contenu et parent hors section sont refusés en 400 ; elle survit à une relecture du disque. |
| 2 | Le build passe ; une entrée de module s'ouvre dans la vue de son module, ou dit que le module est inactif ; elle n'offre pas « Nouvelle sous-page » et ne reçoit pas de page glissée. |
