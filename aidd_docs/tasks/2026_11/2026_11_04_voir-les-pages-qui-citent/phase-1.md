---
status: done
---

# Instruction: le serveur trouve les pages qui citent une page

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── aidd_docs/memory/
│   ├── architecture.md ✏️
│   └── codebase-map.md ✏️
└── vtt-server/src/
    ├── main/java/solovtt/server/
    │   ├── NotebookResource.java ✏️
    │   └── PageLinks.java ✅
    └── test/java/solovtt/server/
        ├── NotebookApiTest.java ✏️
        └── PageLinksTest.java ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Campagne neuve ; pages Bertrand, Session 1, Session 5, Lore | Carnet de quatre pages | api |
| Happy path   | Session 5 et Session 1 contiennent un `pageLink` vers Bertrand (dont un dans une liste imbriquée) ; `GET …/pages/{Bertrand}/citing` | 200, `[Session 1, Session 5]` triées par titre, chacune avec id, titre, parent | api |
| Happy path   | Session 5 cite Bertrand deux fois | Session 5 n'apparaît qu'une fois | api |
| Happy path   | Retirer le lien de Session 1 (nouveau contenu), relire | `[Session 5]` | api |
| Edge case    | Lore ne contient aucun lien vers Bertrand, ou un `pageLink` vers une autre page | Lore absent de la liste | api |
| Edge case    | Bertrand se cite lui-même | Bertrand absent de sa propre liste | api |
| Edge case    | Page que rien ne cite ; page neuve sans contenu | 200, `[]` | api |
| Edge case    | Id inconnu | 404 | api |
| Edge case    | Une fiche au contenu non JSON (test unitaire de `PageLinks`) | Page sautée, les autres listées | system |

## Tasks to do

### `1)` Trouver les liens d'une page

> Une fonction pure qui dit quelles pages contiennent un nœud `pageLink` vers une page donnée.

1. Créer `PageLinks.citing(List<Page> pages, String id)` : parcourt récursivement le contenu JSON de chaque page (`content`, tableaux et objets), retient celles qui ont un nœud `type: "pageLink"` dont `attrs.pageId` vaut `id`, exclut la page `id` elle-même, garde l'ordre reçu.
2. Contenu `null` ou illisible : page sautée, avertissement journalisé comme pour une fiche illisible.
3. `PageLinksTest` : lien à la racine du document, dans une liste imbriquée, deux liens dans une page, lien vers une autre page, autocitation, contenu illisible.

### `2)` Exposer la route

> `GET /api/campaigns/{id}/notebook/pages/{pageId}/citing` rend les pages citantes.

1. Dans `NotebookResource`, ajouter `citing(pageId)` : `known(pageId)`, puis `PageLinks.citing(notebook.list(), pageId)` mappé en `PageSummaryDto`, trié par titre (`list()` l'est déjà).
2. `@APIResponse` 200 (tableau de `PageSummaryDto`) et 404.
3. `NotebookApiTest` : scénarios API ci-dessus, contenus construits avec le nœud `pageLink` du client.

### `3)` Documenter

> La mémoire dit que le serveur lit les liens.

1. `architecture.md` : le serveur lit en lecture seule les nœuds `pageLink` pour `…/citing`, à la demande, sans index ; autocitation exclue.
2. `codebase-map.md` : `PageLinks` et la route `notebook/pages/{id}/citing`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1    | `PageLinksTest` passe : seules les pages qui contiennent un `pageLink` vers la cible, une fois chacune, sans la cible, sans échouer sur un contenu illisible. |
| 2    | `NotebookApiTest` passe : 200 et la liste triée par titre, `[]` sans citation, 404 pour un id inconnu ; `mvn -q test` vert. |
| 3    | `architecture.md` et `codebase-map.md` nomment la route et `PageLinks`. |
