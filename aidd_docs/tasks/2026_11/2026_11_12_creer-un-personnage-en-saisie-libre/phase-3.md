---
status: done
---

# Instruction: module serveur `dnd-server`

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── pom.xml                                         ✏️ <module>dnd-server</module>
├── vtt-server/pom.xml                              ✏️ dépendance dnd:dnd-server
└── dnd-server/
    ├── pom.xml                                     ✅ comme oracle-server, + dnd-engine
    └── src/
        ├── main/resources/META-INF/beans.xml       ✅ vide
        ├── main/java/dnd/server/
        │   ├── DndModule.java                      ✅ InstalledModule « dnd »
        │   ├── SheetResource.java                  ✅ /api/campaigns/{id}/dnd/…
        │   ├── SheetStore.java                     ✅ les événements des fiches dans ModuleData
        │   └── DndDtos.java                        ✅ fiche lue et saisie
        └── test/java/dnd/server/
            └── SheetResourceTest.java              ✅ JUnit simple, ports en mémoire
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `ModuleData` et `NotebookPages` en mémoire, une campagne | ressource prête | system |
| Happy path   | `POST …/dnd/sections` deux fois | une seule racine « Personnages joueurs », même id les deux fois | api |
| Happy path   | `POST …/dnd/sheets { "title": "Aria" }` | une entrée « Aria » sous la racine ; `GET …/dnd/sheets/{pageId}` rend la fiche vide | api |
| Happy path   | `PUT …/dnd/sheets/{pageId}` avec des valeurs, puis `GET` | les valeurs et leurs dérivés ; les événements `SheetCreated`, `SheetEdited` sont dans `modules/dnd.json` | api |
| Edge case    | `PUT` hors bornes (Dex 31) | 400 avec la raison, fiche inchangée | api |
| Edge case    | `GET` / `PUT` d'une page qui n'est pas une fiche D&D de la campagne, campagne inconnue | 404 | api |
| Edge case    | racine renommée, ou id gardé perdu | retrouvée par son id, sinon par son titre, sinon recréée | api |
| Edge case    | `modules/dnd.json` abîmé | lecture refusée sans écraser le fichier (409), comme une fiche illisible | api |
| Happy path   | serveur 8081 assemblé : `GET /api/modules` | contient `dnd` ; campagne sans D&D : `…/dnd/…` en 404 « Module inactif » | api |

## Tasks to do

### `1)` Créer et brancher le module

> Le module D&D se déclare au serveur sans que le cœur le nomme.

1. `dnd-server/pom.xml` (groupId `dnd`) sur le modèle de `oracle-server`, avec `dnd-engine` ; `beans.xml` vide.
2. `DndModule implements InstalledModule`, id `dnd`.
3. Ajouter le module au `pom.xml` racine et la dépendance à `vtt-server/pom.xml`.

### `2)` Tenir la section et les fiches

> La section « Personnages joueurs » existe, et chaque fiche s'y crée, se lit et se modifie.

1. `POST …/dnd/sections` (`synchronized`, idempotent) : assure la racine « Personnages joueurs », gardée par son id dans le document du module, sinon reprise par son titre, sinon créée.
2. `SheetStore` : lit et écrit `{ "playersPageId": …, "sheets": { <pageId>: [événements] } }` par `ModuleData` ; document abîmé : `ClientErrorException` en 409 (comme `AttackResource.java:28` ; `ErrorMapper` ne connaît pas `dnd-server`), jamais écrasé.
3. `POST …/dnd/sheets { title }` : crée l'entrée sous la racine (`NotebookPages.createEntry`), puis `SheetCreated` ; rend l'id de page.
4. `GET …/dnd/sheets/{pageId}` : la fiche pliée et ses dérivés ; 404 si `NotebookPages.isEntry` est faux ou sans événement.
5. `PUT …/dnd/sheets/{pageId}` : valide, ajoute `SheetEdited`, rend la fiche ; 400 avec la raison sinon.
6. `@APIResponse` sur chaque route ; tests `SheetResourceTest`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `mvn -q verify` passe ; `GET /api/modules` du serveur 8081 liste `dnd` ; aucun `dnd.server` dans `vtt-server/src`. |
| 2 | Une fiche se crée sous « Personnages joueurs », se lit vide, se modifie et se relit avec ses dérivés, même après redémarrage ; une valeur hors bornes rend 400 sans rien changer ; une page qui n'est pas une fiche rend 404. |
