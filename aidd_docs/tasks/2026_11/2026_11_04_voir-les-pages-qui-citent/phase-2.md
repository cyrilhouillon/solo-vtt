---
status: pending
---

# Instruction: la page montre qui la cite

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md ✏️
├── aidd_docs/
│   ├── backlog/stories/voir-les-pages-qui-citent-une-page.md ✏️
│   └── memory/
│       ├── architecture.md ✏️
│       ├── codebase-map.md ✏️
│       └── project-brief.md ✏️
└── vtt-web/src/
    ├── api.ts ✏️
    └── components/
        ├── CitingPages.vue ✅
        └── NotebookScreen.vue ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081 / 5174 (`testing.md`) ; campagne avec PNJ > Bertrand, Sessions > Session 1, Session 5 | Carnet ouvert | browser |
| Happy path   | Dans Session 1 et Session 5, insérer `[[Bertrand` ; ouvrir Bertrand | Sous le texte, « Pages qui renvoient ici » : Session 1, Session 5 | browser |
| Happy path   | Cliquer « Session 5 » dans la liste ; puis Tab jusqu'à « Session 1 » et Entrée | La page s'ouvre, notée dernière consultée | browser |
| Happy path   | Retirer le lien de Session 1, ouvrir Bertrand aussitôt (moins d'une seconde après la frappe) | Session 1 a disparu de la liste | browser |
| Happy path   | Sur Bertrand, renommer Session 5 en « Session 5 – L'auberge » depuis l'arbre | La liste montre le nouveau titre sans rouvrir la page | browser |
| Edge case    | Ouvrir Session 1 (que rien ne cite) | « Aucune page ne renvoie ici. » | browser |
| Edge case    | Lier Bertrand à lui-même | Bertrand n'apparaît pas dans sa propre liste | browser |
| Edge case    | Page verrouillée puis déverrouillée | La liste reste la même, cliquable dans les deux états | browser |
| Edge case    | L'appel `…/citing` échoue (route bloquée dans les outils du navigateur), puis ouvrir une page | Le texte s'affiche et la page est notée dernière consultée ; erreur dans le bandeau, pas de liste | browser |
| Teardown     | Arrêter 8081 puis 5174 ; effacer le dossier scratchpad | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌─────────────────────────────────────────────────────────────┐
│ (1) Barre : Accueil · Solo VTT · campagne · Combat · thème   │
├──────────────┬──────────────────────────────────────────────┤
│ (2) Pages    │ (3) Bertrand                               🔒│
│  ▾ PNJ       │ ─────────────────                            │
│    Bertrand  │ (4) Aubergiste du Gué, ancien soldat…        │
│  ▾ Sessions  │                                              │
│    Session 1 │ ─────────────────                            │
│    Session 5 │ (5) PAGES QUI RENVOIENT ICI                  │
│              │     Session 1                                │
│ Nouvelle page│     Session 5                                │
└──────────────┴──────────────────────────────────────────────┘
```

1. Barre de l'application, inchangée.
2. Arbre des pages, inchangé.
3. Titre de la page ouverte et verrou, inchangés.
4. Texte de la page (éditeur), inchangé.
5. Pages qui renvoient ici : sous un filet, un titre en petites capitales, puis les titres des pages citantes, triés comme l'arbre, chacun un lien qui ouvre la page ; vide, « Aucune page ne renvoie ici. ».

## Tasks to do

### `1)` Lire les pages citantes

> Le client appelle la route de la phase 1.

1. Dans `api.ts`, ajouter `citingPages(pageId)` → `PageSummary[]` sur `…/notebook/pages/{id}/citing`.

### `2)` Afficher la liste sous la page

> `CitingPages.vue` montre les pages citantes et les ouvre.

1. Créer `CitingPages.vue` : props `pageIds` et `pages` (liste du Carnet) ; titre tiré de `pages` par id (page absente : sautée), tri `byTitle` ; chaque entrée est un bouton ou lien qui émet `open` ; vide : « Aucune page ne renvoie ici. ». Section `aria-labelledby` son titre.
2. Dans `NotebookScreen.vue`, garder `citing: string[] | null` ; `show` le remet à `null`, puis le relit après `display` et `setLastPage` ; un échec laisse `citing` à `null` et pose l'erreur dans le bandeau, sans bloquer la page ; poser `<CitingPages>` sous `PageEditor`, branché sur `open`.
3. Styler selon `DESIGN.md` (filet effilé, petites capitales comme « Pages », titres à l'encre soulignés `brass` comme un lien de page).

### `3)` Documenter et clore

> Mémoire, design et backlog suivent.

1. `DESIGN.md` : bloc « Pages qui renvoient ici ».
2. `architecture.md` : liste relue à l'ouverture, titre lu dans `notebook.pages`.
3. `codebase-map.md` : `components/CitingPages.vue`.
4. `project-brief.md` : la page liste les pages qui la citent.
5. Story `status: done`, plan `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1    | `npm run build` passe ; l'appel rend la liste du serveur. |
| 2    | Ouvrir Bertrand liste Session 1 et Session 5 ; un clic ou Entrée ouvre la page citante ; un lien retiré disparaît à la réouverture ; un renommage se voit sans rouvrir ; une page non citée affiche « Aucune page ne renvoie ici. ». |
| 3    | `DESIGN.md`, `architecture.md`, `codebase-map.md`, `project-brief.md` décrivent la liste ; story `done`, plan `implemented`. |
