# Review: créer un personnage en saisie libre

- Rounds: 1
- Started: 2026-11-12

## Round 1 · r-7c3a

- Date: 2026-11-12
- By: Cyril Houillon
- Diff: `main...ad95f1f` plus the working tree
- Axes: code, functional, relevancy
- Verdict: changes-requested
- Score: 5/9 met, 0 unmet, 4 unverified at runtime, 0 out of the diff

### Criteria

- [x] Phase 1 · Task 1 — entrée de module créée sous sa section, renommée, déplacée, corbeille et retour, cherchée par titre ; sous-page, contenu, parent hors section en 400 ; relue du disque — `NotebookStore.java:478` (`newEntry`), `NotebookStore.java:170-178` (`checkParent`, `checkContent`), `JsonFileNotebookStore.java:575`, tests `NotebookStoreTest`, `JsonFileNotebookStoreTest`, `NotebookApiTest`
- Unverified at runtime: Phase 1 · Task 2 — build vert et vue du module ou « Module dnd inactif » à l'écran ; le code est là (`NotebookScreen.vue:899-913`, `PageBranch.vue:107`, `tree.ts:122-123`), mais `run.md` est sur `deb0bfb` et `vite.config.ts` a changé depuis (working tree)
- [x] Phase 2 · Task 1 — dérivés du SRD et refus hors bornes avec raison — `CharacterSheet.java:69-135`, `CharacterSheetTest`
- [x] Phase 2 · Task 2 — pli, histoire sans création refusée, `dnd-engine` sans dépendance runtime — `SheetHistory.java:232-247`, `dnd-engine/pom.xml` inchangé
- Unverified at runtime: Phase 3 · Task 1 — `mvn -q verify` et `GET /api/modules` sur 8081 listant `dnd` ; seul l'absence de `dnd.server` dans `vtt-server/src` se lit dans le code ; run enregistré sur un autre head
- [x] Phase 3 · Task 2 — création, lecture, saisie avec dérivés, persistance par `ModuleData`, 400 sans écriture (`toSheet` avant `store.write`), 404 hors fiche — `SheetResource.java:62-112`, `SheetResourceTest`
- Unverified at runtime: Phase 4 · Task 1 — section et « Nouveau personnage » ouvrant un onglet, à l'écran ; run enregistré sur un autre head
- Unverified at runtime: Phase 4 · Task 2 — saisie, rechargement, redémarrage, refus affiché, lien, recherche, renommage, désactivation ; run enregistré sur un autre head (voir aussi les deux premiers findings)
- [x] Phase 5 · Task 1 — mémoire citant les noms réels — `architecture.md` (`Page.entryOf`, `createEntry`, `isEntry`, `newEntry`, `entryView`, `openPage`, `SheetResource`, `SheetStore`), `codebase-map.md`, `package.md`, `project-brief.md`, `testing.md`

### Findings

- `vtt-web/modules/dnd/CharacterSheetView.vue:81` : la fiche s'enregistre au démontage sans que personne n'attende ni ne voie l'issue ; ni `visibilitychange` ni `pagehide` ; les gardes du Carnet (`saved()`) ignorent la vue d'entrée. Quitter la fiche (autre onglet, autre page, rechargement) moins d'une seconde après la frappe, ou avec une valeur refusée, perd la saisie en silence → exposer un `saved()` que `NotebookScreen` attend comme pour l'éditeur (ou émettre `error` au démontage), et envoyer en `keepalive` sur `visibilitychange` / `pagehide` — 🟡 major, error-handling
- `vtt-web/modules/dnd/CharacterSheetView.vue:101` : « Ajouter une attaque » pousse une ligne sans nom ; une seconde plus tard le serveur refuse toute la fiche (« Une attaque a un nom »), et quitter la fiche avant de la nommer perd toutes les saisies non envoyées (finding précédent) → omettre dans `toRequest` les attaques sans nom, comme les emplacements vides — 🟡 major, frontend
- `dnd-server/src/main/java/dnd/server/SheetResource.java:109` : `state.history(pageId)` (qui appelle `toSheet`) est hors du `try` : un `dnd.json` lisible mais aux valeurs hors bornes rend 400, un `type` inconnu ou un `edited` sans `values` rend 500, au lieu du 409 « illisible » promis par `SheetStore` et l'`@APIResponse` → plier l'histoire dans le `try` et y attraper aussi `NullPointerException`, ou convertir dans `SheetStore.read` — 🟡 major, error-handling
- `vtt-web/vite.config.ts:8` : `host: '0.0.0.0'` dans le working tree expose Vite et le proxy `/api` (sans authentification, écriture des campagnes) au réseau local ; sans lien avec la fiche → le retirer de la branche (ou le garder hors commit) — 🟡 major, security
- `dnd-server/src/main/java/dnd/server/SheetResource.java:70` : l'entrée est créée avant l'écriture de `SheetCreated` ; si `store.write` échoue, la page reste sans histoire et rend 404 pour toujours → lire une entrée vivante sans histoire comme une fiche vide (`SheetCreated` implicite) — 🟢 minor, error-handling
- `dnd-server/src/main/java/dnd/server/SheetStore.java:41` : chaque enregistrement automatique ajoute une copie complète de la fiche, et tout `dnd.json` est relu, replié et réécrit à chaque `PUT` ; croissance sans borne → acceptable en solo, à surveiller (compacter en un `SheetEdited` final si besoin) — 🟢 minor, performance
- `vtt-server/src/main/java/solovtt/server/Page.java:53` : `isEntry()` n'a aucun appelant → le retirer — 🟢 minor, code-health
- `.mcp.json:1` : configuration locale de l'IDE, non suivie → la garder hors des commits (ou l'ignorer) — 🟢 minor, conform
