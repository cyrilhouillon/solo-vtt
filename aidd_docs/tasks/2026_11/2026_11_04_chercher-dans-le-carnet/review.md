# Review: chercher dans le Carnet

- Rounds: 2
- Started: 2026-11-04

## Round 1 · r-7c3e

- Date: 2026-11-04
- By: Cyril Houillon
- Diff: `main...aec3078`
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 3/7 met, 0 unmet, 4 unverified at runtime, 0 out of the diff

### Criteria

- [x] Phase 1, tâche 1 : `NotebookResource` n'appelle que `notebook.search` ; `inMemory` et `JsonFileNotebookStore` héritent sans code propre — `NotebookResource.java:115`, `NotebookStore.java:75`
- Unverified at runtime: Phase 1, tâche 2 : `PageSearchTest` passe — les 11 cas existent (`PageSearchTest.java:61-150`), aucun run enregistré sur ce head
- Unverified at runtime: Phase 1, tâche 3 : `NotebookApiTest` passe, `mvn -q test` vert — scénarios présents (200, `[]`, 400 absent/blanc, autre campagne), aucun run enregistré
- [x] Phase 1, tâche 4 : `architecture.md` et `codebase-map.md` décrivent le port et `PageSearch` — `aidd_docs/memory/architecture.md`, `codebase-map.md`
- Unverified at runtime: Phase 2, tâche 1 : `npm run build` passe — `api.ts:96`, `types.ts:71` présents, aucun build enregistré
- Unverified at runtime: Phase 2, tâche 2 : parcours navigateur (« eveque », ouverture sans fermer, frappe non enregistrée trouvée, « dragon », Échap, autre campagne) — code présent (`NotebookScreen.vue:39-75`, `SearchResults.vue`), aucun parcours enregistré
- [x] Phase 2, tâche 3 : `DESIGN.md`, `architecture.md`, `codebase-map.md`, `project-brief.md` décrivent la recherche ; story `done`, plan `implemented` — diff de ces fichiers

### Findings

- `vtt-web/src/components/NotebookScreen.vue:212` : `onBeforeUnmount` ne vide pas `searchTimer` ; quitter l'écran moins de 250 ms après une frappe lance `saved()` et `searchPages` sur un composant démonté → `clearTimeout(searchTimer)` dans `onBeforeUnmount` — 🟢 minor, frontend
- `vtt-web/src/components/SearchResults.vue:20` : le titre vient de la réponse ; renommer la page ouverte par son titre laisse l'ancien titre dans les résultats, alors que `CitingPages` tire les titres de la liste des pages → lire le titre dans `notebook.pages` par id, comme `CitingPages` — 🟢 minor, fit
- `vtt-web/src/components/SearchResults.vue:13` : `aria-live="polite"` sur toute la région fait relire la liste entière à chaque réponse → `aria-live` sur le seul compte (« N pages » / « Aucune page… ») — 🟢 minor, frontend
- `vtt-server/src/main/java/solovtt/server/PageSearch.java:74` : `fold` normalise et compile `\p{M}` à chaque caractère (mesuré ≈ 100 ms par million de caractères, à chaque recherche) → `Pattern` statique et chemin direct pour l'ASCII — 🟢 minor, performance

## Round 2 · r-b41d

- Date: 2026-11-04
- By: Cyril Houillon
- Diff: `main...c0cd594`
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 7/7 met, 0 unmet, 0 unverified at runtime, 0 out of the diff

### Criteria

- [x] Phase 1, tâche 1 : `NotebookResource` n'appelle que `notebook.search` ; `inMemory` et `JsonFileNotebookStore` héritent sans code propre — `NotebookResource.java:115`, `NotebookStore.java:75`
- [x] Phase 1, tâche 2 : `PageSearchTest` passe — `mvn -q install` sur `c0cd594` : 11 tests, 0 échec
- [x] Phase 1, tâche 3 : `NotebookApiTest` passe, tests verts — même run : 21 tests, 0 échec ; build Maven en succès
- [x] Phase 1, tâche 4 : `architecture.md` et `codebase-map.md` décrivent le port et `PageSearch` — `aidd_docs/memory/architecture.md`, `codebase-map.md`
- [x] Phase 2, tâche 1 : `npm run build` passe — run sur `c0cd594`, `vue-tsc` et Vite sans erreur ; l'appel rend la liste du serveur (parcours ci-dessous)
- [x] Phase 2, tâche 2 : parcours sur 8081 / 5174 — « eveque » : Évêque Aldric puis Session 1, « évêque » surligné ; clic puis Tab + Entrée ouvrent la page, résultats gardés, dernière page notée ; « gargouille » tapé dans Session 2 trouvé moins d'une seconde après ; « dragon » : « Aucune page ne contient « dragon ». » ; Échap rend l'arbre déplié à l'identique ; réponse lente de « crypte » ignorée au profit de « village » ; échec réseau : bandeau « Recherche impossible », page et champ gardés ; Recherche B ne trouve rien de Recherche A
- [x] Phase 2, tâche 3 : `DESIGN.md`, `architecture.md`, `codebase-map.md`, `project-brief.md` décrivent la recherche ; story `done`, plan `implemented` — diff de ces fichiers

Les quatre constats mineurs du round 1 sont corrigés (`dcfecaf`, `c0cd594`) et vérifiés au parcours : renommer la page ouverte change son titre dans les résultats ; quitter le Carnet aussitôt après une frappe ne lance aucune recherche ; un seul `aria-live`, sur le compte.
