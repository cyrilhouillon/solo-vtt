# Review: ranger les pages du Carnet dans un arbre

- Rounds: 2
- Started: 2026-11-04

## Round 1 · r-7c3e

- Date: 2026-11-04
- By: Cyril Houillon
- Diff: `main...0359f9b`
- Axes: code, functional, relevancy
- Verdict: changes-requested
- Score: 3/10 met, 0 unmet, 7 unverified at runtime, 0 out of the diff

### Criteria

- Unverified at runtime: P1.1 `PageTreeTest` passe — tests présents (`PageTreeTest.java`, 5 cas : boucle à toute profondeur, orphelin et boucle remis à la racine) ; aucun run enregistré
- Unverified at runtime: P1.2 `mvn -q test` passe, relecture identique, fiche sans `parentId` à la racine, boucle sans écriture — cas présents dans `NotebookStoreTest` et `JsonFileNotebookStoreTest` ; aucun run enregistré
- Unverified at runtime: P1.3 `NotebookApiTest` passe — 6 tests ajoutés couvrant création sous parent, renommage, déplacement, 400, 404 ; aucun run enregistré
- [x] P1.4 La mémoire nomme `parentId`, `PageTree` et les deux routes — `aidd_docs/memory/architecture.md`, `aidd_docs/memory/codebase-map.md:41`
- Unverified at runtime: P2.1 `npm run build` passe ; « Session 2 » avant « Session 10 » ; jamais sous soi — `Intl.Collator` `numeric` (`vtt-web/src/notebook/tree.ts:10`) et `canMove` (`tree.ts:68`) présents ; build non lancé
- Unverified at runtime: P2.2 L'arbre s'affiche, clic, repli, branche dépliée au montage — `PageTree.vue`, `PageBranch.vue`, `reveal` dans `NotebookScreen.vue:105` ; affichage à voir
- Unverified at runtime: P2.3 Créer, renommer (arbre et titre), déplacer, relu après redémarrage, tout au clavier — code présent ; parcours à faire
- [x] P2.4 `DESIGN.md` et la mémoire décrivent l'arbre, son menu, le dialogue et le titre éditable — `DESIGN.md`, `architecture.md`, `project-brief.md`
- Unverified at runtime: P3.1 Glisser à la souris, cible interdite sans requête, clic court ouvre, doigt défile — `PageTree.vue:52-157` ; parcours à faire
- [x] P3.2 `DESIGN.md` et `architecture.md` décrivent le glisser ; story et plan clos — story `status: done`, plan `status: implemented`

### Findings

- `vtt-web/src/components/NotebookScreen.vue:126` : titre en tête modifié puis clic sur une autre page de l'arbre → le `blur` envoie le renommage (`busy` passe à vrai) avant le `click`, et `open` sort sur `busy` : le clic est perdu, il faut recliquer → faire attendre l'ouverture plutôt que l'ignorer (enchaîner `show` après le renommage en cours, ex. une file comme `saving`), ou ne pas garder `open` par `busy` quand seul un renommage tourne — 🟡 major, frontend
- `vtt-web/src/components/PageBranch.vue:210` : le menu de ligne (`position: absolute; top: 100%`, 3 boutons) vit dans `.tree` en `overflow-y: auto` (`PageTree.vue:279`) avec 0,5rem de marge basse : sur les dernières lignes, il est coupé et fait apparaître une barre de défilement (cas courant : un Carnet de 1 à 3 pages) → ouvrir le menu vers le haut quand la place manque, ou le sortir du conteneur qui défile (`Teleport` + position calculée) — 🟡 major, frontend
- `vtt-web/src/components/NotebookScreen.vue:170` : `rename` n'est pas gardé par `busy`, contrairement à `createPage` et `moveTo` ; deux appels qui se chevauchent remettent `busy` à faux au premier `finally`, pendant que l'autre tourne → `if (busy.value) return` en tête, comme les autres gestes — 🟢 minor, error-handling
- `vtt-web/src/components/PageTree.vue:189` : `submitEdit` ferme le champ de sous-page avant que `createPage` (`NotebookScreen.vue:154`) ne refuse (occupé, ou frappe non enregistrée) : le titre tapé est perdu sans message → ne fermer le champ qu'au succès (rappel `done`, comme pour la création à la racine) — 🟢 minor, error-handling
- `vtt-web/src/notebook/tree.ts:30` : `seen` est mort (chaque id n'est que dans un seul seau de `children`, et une boucle n'est jamais atteinte depuis la racine) et `&& seen.add(...)` repose sur la valeur vraie d'un `Set` → retirer `seen` et le `filter` — 🟢 minor, rot

## Round 2 · r-b4d9

- Date: 2026-11-04
- By: Cyril Houillon
- Diff: `main...1a50efb`
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 3/10 met, 0 unmet, 7 unverified at runtime, 0 out of the diff

### Criteria

- Unverified at runtime: P1.1 `PageTreeTest` passe — tests présents (`PageTreeTest.java`) ; aucun run enregistré à côté du plan
- Unverified at runtime: P1.2 `mvn -q test` passe, relecture identique, fiche sans `parentId` à la racine, boucle sans écriture — cas présents dans `NotebookStoreTest` et `JsonFileNotebookStoreTest` ; aucun run enregistré
- Unverified at runtime: P1.3 `NotebookApiTest` passe — 6 tests d'API ajoutés ; aucun run enregistré
- [x] P1.4 La mémoire nomme `parentId`, `PageTree` et les deux routes — `aidd_docs/memory/architecture.md`, `aidd_docs/memory/codebase-map.md:41`
- Unverified at runtime: P2.1 `npm run build` passe ; « Session 2 » avant « Session 10 » ; jamais sous soi — `Intl.Collator` `numeric` (`vtt-web/src/notebook/tree.ts:10`), `canMove` (`tree.ts`) ; aucun build enregistré
- Unverified at runtime: P2.2 L'arbre s'affiche, clic, repli, branche dépliée au montage — `PageTree.vue`, `PageBranch.vue`, `reveal` (`NotebookScreen.vue`) ; affichage à voir
- Unverified at runtime: P2.3 Créer, renommer (arbre et titre), déplacer, relu après redémarrage, tout au clavier — code présent ; parcours à faire
- [x] P2.4 `DESIGN.md` et la mémoire décrivent l'arbre, son menu, le dialogue et le titre éditable — `DESIGN.md:296`, `architecture.md`, `project-brief.md`
- Unverified at runtime: P3.1 Glisser à la souris, cible interdite sans requête, clic court ouvre, doigt défile — `PageTree.vue` (`onTreePointerDown` à `cancelDrag`) ; parcours à faire
- [x] P3.2 `DESIGN.md` et `architecture.md` décrivent le glisser ; story et plan clos — story `status: done`, plan `status: implemented`

### Findings

- `vtt-web/src/components/PageTree.vue:183` : `right = window.innerWidth - rect.right` compte la barre de défilement verticale, alors qu'un élément `fixed` se place dans la fenêtre sans elle : quand la page défile, le menu est décalé à gauche de la largeur de la barre (≈15px) → `document.documentElement.clientWidth - rect.right` (et `clientHeight` pour `bottom`) — 🟢 minor, frontend
- `DESIGN.md:296` : le menu de ligne « ouvre sous lui » ; il s'ouvre désormais au-dessus quand la fenêtre manque de place et se ferme au défilement ou au redimensionnement (`PageTree.vue`, `toggleMenu`, `onViewportChange`) → compléter la phrase — 🟢 minor, conform
