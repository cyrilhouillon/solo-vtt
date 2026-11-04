# Review: écrire une page dans le Carnet

- Rounds: 2
- Started: 2026-11-04

## Round 1 · r-7c3e

- Date: 2026-11-04
- By: Cyril Houillon
- Diff: `main...51d22fe` plus the working tree (`vtt-web/.impeccable/`, non suivi)
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 5/12 met, 0 unmet, 7 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1·1 `NotebookStoreTest` et `CampaignStoreTest` : créer, lister par titre, contenu, dernière page, inconnue → `IllegalArgumentException`, Carnets séparés — `NotebookStoreTest.java:17,30,43`, `CampaignStoreTest` `enMemoire_unCarnetParCampagne`
- [x] P1·2 `JsonFileNotebookStoreTest` et `FileCampaignStoreTest` : relecture, fiche illisible ignorée et intacte, id hors des pages sans chemin — `JsonFileNotebookStoreTest.java:36,56,70`, `FileCampaignStoreTest` `leCarnet_vitDansLeDossierDeSaCampagne_unParCampagne`
- Unverified at runtime: P1·3 `NotebookApiTest` couvre chaque ligne « api » du Test Scope (`NotebookApiTest.java:44-117`) — `mvn -q test` vert, aucun run consigné
- [x] P1·4 mémoire : port, adaptateur, routes, format — `architecture.md` (`NotebookStore`, `carnet/`, JSON Tiptap opaque), `codebase-map.md`
- Unverified at runtime: P2·1 `npm run build` passe ; appels sous `/api/campaigns/{id}/notebook…` vus dans `api.ts` — le build (`vue-tsc`)
- Unverified at runtime: P2·2 ouverture sur le Carnet, état vide, création, dernière page après redémarrage 8081 — parcours navigateur
- Unverified at runtime: P2·3 Combat ↔ Carnet sur la même page — parcours navigateur
- [x] P2·4 `DESIGN.md` et mémoire décrivent l'écran et sa navigation — `DESIGN.md`, `architecture.md`, `project-brief.md`
- Unverified at runtime: P3·1 mise en forme à la frappe, Ctrl+Z / Ctrl+Y, build — parcours navigateur
- Unverified at runtime: P3·2 page rouverte verrouillée et inaltérable, déverrouillée sans déplacement du texte, page créée éditable — parcours navigateur
- Unverified at runtime: P3·3 texte gardé au rechargement, au passage immédiat au combat, à la fermeture d'onglet, au redémarrage — parcours navigateur
- [x] P3·4 doc à jour, story `done`, plan `implemented` — `stories/ecrire-une-page-dans-le-carnet.md`, `plan.md`, `package.md`

### Findings

- `vtt-server/src/main/java/solovtt/server/persistence/JsonFileNotebookStore.java:87` : `known` passe par `find` → `list()`, qui lit et parse toutes les fiches, contenus compris ; chaque enregistrement automatique (une par seconde de frappe) le fait deux fois (`NotebookResource.java:72` puis `saveContent`), coût qui croît avec la taille du Carnet → surcharger `find` : refuser un id qui n'est pas un UUID (`UUID.fromString`), puis lire la seule fiche `<id>.json` si elle existe ; la garde contre un chemin construit depuis un id quelconque reste — 🟢 minor, performance
- `vtt-web/src/components/NotebookScreen.vue:155` : barre (Accueil, nom, campagne, thème), `toggleTheme`, bandeau d'erreur et leurs styles (`:252-312`) sont la troisième copie de `HomeScreen.vue:110-137` et `GameScreen.vue:1358-1536` → extraire un `AppBar` (slot pour les actions de droite) avant le prochain écran, ou l'assumer — 🟢 minor, rot
- `vtt-web/.impeccable/` : non suivi ni ignoré ; `config.json` porte une règle de projet (exception `side-tab` pour `PageEditor.vue`) mais `hook.cache.json` et `live/` sont de l'état local → committer `config.json` et ignorer `hook.cache.json` et `live/` dans `.gitignore` — 🟢 minor, code-health

## Round 2 · r-a91d

- Date: 2026-11-04
- By: Cyril Houillon
- Diff: `main...51d22fe` plus the working tree (correctifs de la passe 1, `ThemeButton.vue`, `.gitignore`)
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 12/12 met, 0 unmet, 0 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1·1 `NotebookStoreTest` et `CampaignStoreTest` — `mvn -q test` : 159 tests serveur, 0 échec
- [x] P1·2 `JsonFileNotebookStoreTest` et `FileCampaignStoreTest` — 6 tests de l'adaptateur verts, dont `uneFicheIllisibleNommeeParUnId_nEstPasTrouvee_etJamaisEcrasee`
- [x] P1·3 `NotebookApiTest`, `mvn -q test` vert, tests existants compris — même run
- [x] P1·4 mémoire : port, adaptateur, routes, format — `architecture.md`, `codebase-map.md`
- [x] P2·1 `npm run build` passe ; appels sous `/api/campaigns/{id}/notebook…` — build vert ; requêtes `PUT …/notebook/pages/{id}/content` observées sur 5174
- [x] P2·2 Carnet à l'ouverture, état vide (champ en focus, « Créer » éteint sur titre blanc, `maxlength` 120), création qui ouvre la page, dernière page rouverte après Accueil et après redémarrage de 8081 — navigateur 5174 → 8081
- [x] P2·3 Combat ouvre l'écran de jeu, Carnet ramène sur « Session 1 » — navigateur
- [x] P2·4 `DESIGN.md` et mémoire — fichiers lus
- [x] P3·1 titre, citation, liste, gras, italique à la frappe (`<h1>`, `<blockquote>`, `<ul>`, `<strong>`, `<em>`) ; Ctrl+Z défait, Ctrl+Y refait ; build vert — navigateur
- [x] P3·2 page rouverte verrouillée : frappe, Ctrl+Z, Ctrl+B, coller sans effet et sans envoi ; déverrouillée au clavier (Tab, Entrée, `aria-pressed` bascule), positions du texte identiques ; page créée éditable et en focus — navigateur
- [x] P3·3 texte gardé au rechargement, au clic sur Combat moins d'une seconde après la frappe, à la fermeture de l'onglet aussitôt après la frappe, au redémarrage de 8081 ; serveur arrêté : bandeau « Page non enregistrée », Combat refusé, texte gardé puis enregistré à la frappe suivante — navigateur et fiche `carnet/<id>.json`
- [x] P3·4 doc à jour, story `done`, plan `implemented`

### Findings

- `vtt-web/src/components/NotebookScreen.vue:155` : sous 375 px, le nom de campagne de la barre est réduit à son ellipse (même règle `.campaign-name` qu'avant le partage) → hors périmètre de la story, à reprendre avec la barre mobile si besoin — 🟢 minor, frontend
