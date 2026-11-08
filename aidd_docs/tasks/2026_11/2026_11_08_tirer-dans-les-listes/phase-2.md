---
status: done
---

# Instruction: le serveur Mythic lit les listes du Carnet et rend l'entrée tirée

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
│       └── NotebookApiTest.java ✏️
├── mythic-server/src/
│   ├── main/java/mythic/server/
│   │   ├── ListLines.java ✅
│   │   ├── MythicResource.java ✏️
│   │   └── MythicDtos.java ✏️
│   └── test/java/mythic/server/
│       ├── ListLinesTest.java ✅
│       ├── MythicResourceTest.java ✏️
│       └── PagesDeTest.java ✏️
└── aidd_docs/memory/
    ├── architecture.md ✏️
    └── codebase-map.md ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Campagne, listes créées (`POST …/lists`), contenus de Fils et Personnages posés dans `PagesDeTest` | Deux racines de section avec leurs lignes | system |
| Happy path   | Question qui déclenche Action d'un PNJ ; Personnages : « Garde Ylo », lien vers « Vessa », « Garde Ylo » | `randomEvent.list` : liste, ligne, dés, entrée en nœuds en ligne et en texte | system |
| Happy path   | Ligne tirée = le lien vers la page ordinaire « Vessa » | L'entrée est le nœud `pageLink` d'origine ; texte « Vessa » | system |
| Edge case    | Ligne tirée vide | `blank: true`, sans entrée | system |
| Edge case    | Page Fils vidée, focus de fil | `list.empty: true`, focus effectif Contexte actuel, aucun dé de plus | system |
| Edge case    | Ids de listes absents de `mythic.json`, ou page qui n'est plus une racine de la section | Liste vide | system |
| Edge case    | Contenu illisible | Liste vide, question rendue | system |
| Edge case    | Texte hors liste, sous-liste dans un élément | Ignorés | system |
| Edge case    | `pageTitle` d'une page ordinaire, d'une page à la corbeille | Son titre ; vide | api |
| Edge case    | `sectionRootContent` d'une page ordinaire, d'une racine d'un autre module, ou à la corbeille | Vide (`NotebookApiTest`) | api |

## Tasks to do

### `1)` Le cœur rend le contenu d'une racine de section

> Un module lit ses propres racines, et rien d'autre.

1. `NotebookPages.sectionRootContent(campaignId, moduleId, pageId)` : `Optional<String>`, vide si la page n'est pas une racine vivante de la section du module.
2. `NotebookPages.pageTitle(campaignId, pageId)` : `Optional<String>`, titre d'une page vivante de la campagne, vide sinon (corbeille, inconnue).
3. `NotebookPagesAdapter` les implémente ; tests dans `NotebookApiTest`.

### `2)` Les lignes d'une page de liste

> Du document Tiptap aux lignes de la liste.

1. `ListLines.of(contentJson)` : éléments de premier niveau des listes de premier niveau, premier paragraphe de chacun en nœuds en ligne ; ligne vide sans texte visible ni lien ; illisible : aucune ligne.
2. Texte brut d'une ligne : texte, et pour un lien son `label`, sinon le titre de la cible (`pageTitle`), sinon « page introuvable ».

### `3)` La question tire dans la liste

> `ask` passe les deux listes au moteur et joint l'entrée.

1. `MythicResource.ask` lit l'état (`threadsPageId`, `charactersPageId`), puis `sectionRootContent` ; passe les lignes à `FateQuestion.ask`.
2. `RandomEventDto` : `effectiveFocus`, `effectiveFocusLabel`, `list` (`ListDrawDto` : `list`, `listLabel`, `empty`, `full`, `line`, `sectionRoll`, `sectionDie`, `lineRoll`, `blank`, `entry` en `JsonNode[]`, `entryText`).

### `4)` Tests et mémoire

> Chaque ligne du Test Scope ; la doc dit la lecture.

1. `PagesDeTest` garde un contenu modifiable ; `ListLinesTest`, `MythicResourceTest`.
2. `architecture.md`, `codebase-map.md` : méthode du port, lignes d'une liste, tirage, DTO.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Seule une racine vivante de la section du module rend son contenu ; `pageTitle` rend le titre d'une page vivante seulement. |
| 2 | Une liste à 3 éléments, dont un vide et un lien, rend 3 lignes justes ; texte et sous-listes hors lignes. |
| 3 | La réponse porte l'entrée tirée, « Choisir » ou la liste vide, avec ses dés. |
| 4 | `mvn -q test -pl vtt-server,mythic-server -am` vert ; la mémoire décrit le port et le tirage. |
