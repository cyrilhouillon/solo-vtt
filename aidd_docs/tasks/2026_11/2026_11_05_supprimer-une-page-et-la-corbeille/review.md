# Review: supprimer une page et la restaurer depuis la corbeille

- Rounds: 1
- Started: 2026-11-05

## Round 1 · r-2220

- Date: 2026-11-05
- By: Cyril Houillon
- Diff: `main...58f5105` plus the working tree
- Axes: code, functional, relevancy
- Verdict: changes-requested
- Score: 5/11 met, 0 unmet, 6 unverified at runtime, 0 out of the diff

### Criteria

- [x] Phase 1 · 1 — `branch` rend la page et ses sous-pages à toute profondeur ; `citingAny` exclut le lot ; `citing` inchangé — `vtt-server/src/main/java/solovtt/server/PageTree.java:36`, `PageLinks.java:27-36`
- [x] Phase 1 · 2 — page supprimée et sous-pages hors de `list()`, `find()`, citations, recherche ; restaurées intactes, à la racine sans parent vivant ; vidées, perdues — `NotebookStore.java:204-235`, `persistence/JsonFileNotebookStore.java:60-142`
- [x] Phase 1 · 3 — routes 200 / 204, 404 pour page ou entrée inconnue, `@APIResponse` sur chacune — `NotebookResource.java:143-193`
- Unverified at runtime: Phase 1 · 4 — `mvn -q test` passe — tests écrits (`NotebookApiTest`, `NotebookStoreTest`, `JsonFileNotebookStoreTest`, `PageLinksTest`, `PageTreeTest`), aucun run enregistré sur ce head
- [x] Phase 1 · 5 — `architecture.md` et `codebase-map.md` décrivent la corbeille et ses routes — `aidd_docs/memory/architecture.md:46`, `aidd_docs/memory/codebase-map.md:41`
- Unverified at runtime: Phase 2 · 1 — `npm run build` passe avec les cinq appels — `vtt-web/src/api.ts:109-119`, build non enregistré
- Unverified at runtime: Phase 2 · 2 — « Supprimer… » s'ouvre à la souris, au clavier et au doigt — entrée présente (`PageBranch.vue:106`), gestes non joués
- Unverified at runtime: Phase 2 · 3 — message accordé, rien quand nul ; Annuler / Échap ne suppriment rien — `DeletePageDialog.vue:15-33,56-58`, affichage non vu
- Unverified at runtime: Phase 2 · 4 — lot hors de l'arbre, onglets fermés, frappe d'abord, restauration à sa place, liens qui revivent, vider — `NotebookScreen.vue:440-510`, parcours non joué
- Unverified at runtime: Phase 2 · 5 — Test Scope à 1280px et 375px, deux thèmes — aucun parcours enregistré
- [x] Phase 2 · 6 — mémoire, `DESIGN.md`, story et plan à jour — `DESIGN.md:302-304`, `aidd_docs/memory/project-brief.md:47`, story `status: done`, `plan.md:3`

### Findings

- `vtt-server/src/main/java/solovtt/server/persistence/JsonFileNotebookStore.java:101` : `delete` marque les fiches dans l'ordre des titres et `restore` (l.117) dans l'ordre du disque ; une écriture qui échoue en cours de lot laisse des sous-pages marquées dont la page racine est vivante : invisibles de `list()`, absentes de `trash()` (filtre `page.id == deletedWith`), donc irrécupérables, puis effacées sans le dire par `emptyTrash` → dans `delete`, marquer la page racine d'abord ; dans `restore`, la démarquer en dernier : un échec laisse l'entrée visible et le geste rejouable — 🟡 major, error-handling
- `vtt-web/src/components/TrashDialog.vue:93` : « Restaurer » retire sa ligne, et le focus tombe hors de tout contrôle dans le modal ; un échec (`NotebookScreen.vue:468`) ferme la corbeille sans rendre le focus au bouton « Corbeille », contre le Test Scope (« le focus revient au déclencheur ») → après restauration, focus sur le « Restaurer » suivant, sinon « Fermer » ; dans `inTrash`, fermer par `closeTrash()` — 🟢 minor, frontend
- `vtt-web/src/components/NotebookScreen.vue:453` : si la relecture du Carnet échoue après un `DELETE` réussi, `closeTabs(lot)` ne part pas : la page supprimée reste ouverte et son prochain enregistrement rend 404 → fermer les onglets du lot (sur `lot`, déjà calculé) avant de relire le Carnet, ou dans un `finally` — 🟢 minor, error-handling
- `aidd_docs/tasks/2026_11/2026_11_04_voir-les-pages-qui-citent/review.md:1` : rapport d'une autre feature, non suivi, dans l'arbre de travail de cette branche → le committer sur sa propre branche ou le retirer, hors de cette PR — 🟢 minor, fit
