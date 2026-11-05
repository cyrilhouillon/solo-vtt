# Run

## Phase 1 · Scénario de l'epic en test du Carnet

- Date: 2026-11-05
- Validated on: df698cf

- [pass] Task 1 — `mvn -q test` passe avant l'ajout — `mvn -q test` à la planification : exit 0, 314 tests, 0 échec.
- [pass] Task 2 — `CarnetDeBoutEnBoutTest` passe ; il échoue si un renommage réécrit une page citante, si la restauration perd un lien entrant ou si la relecture diffère — `mvn -q test` : exit 0, 316 tests, 0 échec ; `Tests run: 2, Failures: 0, Errors: 0 -- in solovtt.server.CarnetDeBoutEnBoutTest`. Le test compare le contenu de Session 5 avant et après renommage, `PageLinks.citing` après restauration, puis `list`, `tabs`, `trash`, recherche et `NotebookResource.get()` relus par un nouveau `FileCampaignStore` ; un lot laissé à la corbeille survit au redémarrage, `tabs()` garde l'id d'Ismark et `get()` l'omet.

## Phase 2 · Parcours navigateur du Carnet

- Date: 2026-11-05
- Validated on: df698cf

- [pass] Task 1 — 5174 répond et appelle 8081 ; `~/.solo-vtt/campagnes` inchangé — `mvn -q install -DskipTests` exit 0 ; `GET :8081/api/campaigns` et `GET :5174/api/campaigns` → `[]` ; md5 des fichiers de `~/.solo-vtt/campagnes` identique avant et après (`acfaa96c…`).
- [pass] Task 2 — `verification.md` dit, preuve à l'appui, si chacune des quatre premières preuves de l'epic tient — Chrome 1568 × 733, thème clair, 5174 → 8081 : preuves 1 à 4 tenues (session liée par `[[` puis combat lancé, tour fini, combat terminé, retour sur Session 5 avec ses 5 onglets ; renommage visible dans le lien, l'onglet et l'arbre, et Session 5 listée sous « Pages qui renvoient ici » ; « vigne » → Session 5, mot surligné ; suppression confirmée « 2 sous-pages, 1 page y renvoie », liens barrés, restauration qui les fait revivre), détail dans `verification.md`.
- [pass] Task 3 — `verification.md` dit, preuve à l'appui, si le Carnet reprend à l'identique après redémarrage — relevé `GET` (campagnes, notebook, trash, chaque page et ses citantes, recherche) avant et après le redémarrage de 8081 : `cmp` identique ; chargement direct des adresses d'Ismark le Petit et de Session 5, puis ouverture depuis l'accueil : même page, mêmes liens et citantes ; console sans erreur.
- [pass] Task 4 — Plus rien n'écoute sur 8081 ni 5174 ; 8080 et 5173 inchangés — `netstat` après arrêt : seuls 8080 (PID 50784) et 5173 (PID 40236), les mêmes qu'avant.
