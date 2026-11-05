# Review: garder des pages en onglets

- Rounds: 1
- Started: 2026-11-05

## Round 1 · r-7c3e

- Date: 2026-11-05
- By: Cyril Houillon
- Diff: `main...50fa3ce` plus the working tree
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 7/10 met, 0 unmet, 3 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1.1 `setTabs` puis `tabs()` rend mêmes ids, ordre et actif ; inconnue, doublon, actif hors liste lèvent sans rien changer — `NotebookStore.java:154-163`, `NotebookStoreTest.java:30-69`
- [x] P1.2 Nouvel adaptateur relit les onglets ; ancien `carnet.json` lu en un onglet ; illisible vide et intact — `JsonFileNotebookStore.java:94-104`, `JsonFileNotebookStoreTest.java:38-50,99-132`
- [x] P1.3 `PUT …/notebook/tabs` 204 puis `GET` ; 404 / 400 ; `last-page` supprimé — `NotebookResource.java:44-51,142-154`, `NotebookApiTest.java:85-112`
- Unverified at runtime: P1.4 `mvn -q test` passe — aucune exécution consignée sur 50fa3ce
- [x] P1.5 `architecture.md` et `codebase-map.md` ne citent plus `last-page` — grep vide
- Unverified at runtime: P2.1 `npm run build` passe sans `lastPageId` ni `setLastPage` dans `vtt-web/src` — grep vide, build non consigné
- Unverified at runtime: P2.2 titre actuel, actif souligné, fermeture souris et clavier — rendu de `PageTabs.vue` à voir à l'écran
- [x] P2.3 Toute ouverture entre en onglet ou active le sien ; fermer l'actif active le voisin ; frappe envoyée avant, échec garde la page — `NotebookScreen.vue:207-253` (`show`, `closing`), `open` partagé par arbre, lien, recherche, citantes et onglets
- [x] P2.4 Onglets et actif reviennent au retour du combat, à la réouverture et après redémarrage — montage `NotebookScreen.vue:275-289` sur `openPageIds` / `activePageId`, persistance fichier testée en phase 1
- [x] P2.5 Mémoire, `DESIGN.md`, story et plan à jour — story `status: done`, plan `status: implemented`, diff de `architecture.md`, `codebase-map.md`, `project-brief.md`, `DESIGN.md`

### Findings

- `vtt-web/src/components/NotebookScreen.vue:248` : fermer l'onglet actif met `page` à `null` avant `show(next)` ; si `api.page(next)` échoue, l'écran reste sans page ni « Aucune page ouverte. » (le `v-else-if` exige `!tabs.length`) et les onglets réduits ne sont pas envoyés au serveur → ne vider `page` qu'après la lecture réussie du voisin, ou appeler `persistTabs` et afficher l'invite quand `page` est `null` — 🟢 minor, error-handling
- `vtt-web/src/components/NotebookScreen.vue:283` : le montage rouvre la première page de l'arbre quand tous les onglets étaient fermés ; « aucun onglet » ne survit donc pas au retour du combat (critère P2.4 : « mêmes onglets ») → n'ouvrir la première page que si le Carnet n'a jamais eu d'onglet, ou accepter et le dire dans `architecture.md` (déjà écrit : « sinon la première page de l'arbre ») — 🟢 minor, fit
- `vtt-web/src/api.ts:78` : la doc de `notebook()` dit encore « la dernière page consultée » → « et ses onglets ouverts » — 🟢 minor, standards
- `vtt-server/src/test/java/solovtt/server/NotebookApiTest.java:74-83` : deux `onglets` surchargés, l'un envoie, l'autre construit le corps (`onglets(onglets(…), 204)`), et `new java.util.HashMap` qualifié en ligne → nommer `envoyerOnglets` / `corpsOnglets`, importer `HashMap` — 🟢 minor, standards
- `vtt-server/src/main/java/solovtt/server/NotebookStore.java:115` : ligne de 148 caractères, `JsonFileNotebookStore.java:23` de 135 → couper comme le reste du fichier — 🟢 minor, standards
