# Run

## Phase 1 · L'adresse suit l'écran et la page

- Date: 2026-11-05
- Validated on: f3016c0

- [pass] Task 1 — `npm run build` passe ; une adresse inconnue devient `/` ; une campagne inconnue mène à l'accueil — `npm run build` : exit 0, `✓ built in 773ms` ; navigateur sur 5174 (API 8081, campagnes du scratchpad) : `/n-importe-quoi/x` → `/`, accueil affiché ; `/campagnes/00000000-…/carnet` → `/`, accueil affiché.
- [pass] Task 2 — Accueil, Carnet et combat ont chacun leur adresse ; recharger rouvre le même écran — « Ouvrir Test » → `/campagnes/<id>/carnet/<Auberge>` ; « Combat » → `/campagnes/<id>/combat` ; Accueil → `/` ; chargement direct de `/campagnes/<id>/combat` : écran de combat de « Test » ; chargement direct de `/campagnes/<id>/carnet/<PNJ>` : Carnet sur PNJ.
- [pass] Task 3 — Précédent / Suivant repassent par les pages visitées et attendent l'enregistrement ; page inconnue : Carnet comme sans page, sans bandeau ; campagne sans page : onglet actif ; tous les gestes existants du Carnet marchent — arbre Auberge → Bertrand → PNJ, puis Précédent, Précédent, Suivant : Bertrand, Auberge, Bertrand, sans rechargement ; Combat puis Précédent, Accueil puis Précédent : Carnet sur Bertrand ; frappe en attente et `PUT …/content` en échec, Précédent : bandeau « Page non enregistrée », adresse sur Bertrand ; envoi rétabli, Précédent : PNJ, contenu de Bertrand gardé au serveur ; `/carnet/<uuid inconnu>` et `/carnet/<page à la corbeille>` : onglet actif, adresse remplacée, aucun bandeau ; `/carnet` sans page : onglet actif, adresse complétée ; page hors des onglets ouverte par adresse : ajoutée en fin de barre ; fermer l'onglet actif PNJ et supprimer la page ouverte : voisin affiché, adresse remplacée, `history.length` inchangé ; créer « Nouvelle » : entrée poussée, Précédent / Suivant y repassent ; onglet, lien verrouillé, page citante, résultat de recherche : la page s'ouvre et l'adresse suit ; renommer depuis l'arbre : marche, adresse inchangée ; console sans erreur.
- [pass] Task 4 — Les fichiers de mémoire décrivent les routes telles que codées — `architecture.md` (adresses, `writeAddress`, gardes, replis), `codebase-map.md` (`src/router.ts`), `package.md` (`vue-router` 5), `project-brief.md` (Adresses), commit 7dbce00.

## Phase 2 · Le clic du milieu ouvre une page dans un nouvel onglet

- Date: 2026-11-05
- Validated on: aecd618

- [pass] Task 1 — Clic du milieu sur une ligne : nouvel onglet sur la page ; clic simple, clavier et glisser inchangés ; `npm run build` passe — `npm run build` : exit 0, `✓ built in 715ms` ; clic du milieu réel sur la ligne Bertrand (fait par l'utilisateur, 5174) : nouvel onglet du navigateur sur Bertrand ; lignes `<a>` avec `href` de leur page et `draggable=false` ; clic : la page s'ouvre, focus sur la ligne ; Ctrl+clic : la page s'ouvre dans l'application ; glisser Auberge sur PNJs à la souris : Auberge rangée sous PNJs (`parentId` lu à l'API), page ouverte inchangée.
- [pass] Task 2 — Clic du milieu sur un lien vivant, verrouillé ou non : nouvel onglet, la page d'origine inchangée ; lien mort : rien — clic du milieu réel sur le lien Bertrand, page verrouillée puis déverrouillée (fait par l'utilisateur) : nouvel onglet sur Bertrand ; avant la garde, `mousedown` / `mouseup` du milieu simulés en écriture sélectionnaient le lien (`mouseup` annulé) ; après : rien de sélectionné, `mouseup` non annulé, page et adresse sur Auberge ; clic simple en écriture : lien sélectionné ; Ctrl+clic en écriture : Bertrand s'ouvre ; lien mort rendu sans `href`.
- [pass] Task 3 — Mémoire, story et plan à jour — `architecture.md` (arbre et liens, commit 26cfad1) ; story `status: done` ; plan `status: implemented`.

## Final

- Date: 2026-11-05
- Validated on: aecd618

- [pass] `mvn -q test` — exit 0
- [pass] `npm run build` dans `vtt-web/` — exit 0, `✓ built in 715ms`
