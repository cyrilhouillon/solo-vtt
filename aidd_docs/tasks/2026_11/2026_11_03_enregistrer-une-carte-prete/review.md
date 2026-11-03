# Review: enregistrer puis charger une carte prête

- Rounds: 2
- Started: 2026-11-03

## Round 1 · r-2a55

- Date: 2026-11-03
- By: Cyril Houillon
- Diff: `main...2df8d0e`
- Axes: code, functional, relevancy
- Verdict: changes-requested
- Score: 4/10 met, 1 unmet, 5 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1·1 `MapStore.inMemory()` remplace une carte de même nom et liste par nom — `vtt-server/src/main/java/solovtt/server/MapStore.java:34`, `MapStoreTest.java:18`
- Unverified at runtime: P1·2 `JsonFileMapStoreTest` passe — les quatre cas sont écrits (`JsonFileMapStoreTest.java:29-71`, dossier créé via `sous-dossier/`) ; seul un `mvn -q test` sur ce head le montrerait
- [x] P1·3 `testing.md` donne la commande avec les trois chemins du scratchpad — `aidd_docs/memory/testing.md:27`
- [x] P2·1 Carte chargée neuve, aux positions, sans combat ni journal, nom rejoué ; entrée manquante sans changement ; partie sans nom rejouée — `SceneSession.java:399-410`, `:463`, `JsonFileGameStoreTest.java:91`, `ResumeGameTest`, `SceneWithoutMapTest.java:28`
- [x] P2·2 Codes 201, 200, 400, 404, 409 rendus — `MapResource.java:38-78` (400 par `ErrorMapper.invalid`, 409 par `ClientErrorException` et `ActionRefusedException`)
- Unverified at runtime: P2·3 `mvn -q test` passe ; carte chargée reprise à l'identique — tests présents (`ResumeGameTest`), aucune exécution enregistrée sur ce head
- Unverified at runtime: P3·1 `npm run build` passe (`vue-tsc`) — aucune exécution enregistrée
- Unverified at runtime: P3·2 Le tiroir montre champ, liste, avertissement de nom pris et confirmation — présents dans `SavedMapsPanel.vue:66-99` ; l'affichage demande un parcours navigateur
- Unverified at runtime: P3·3 Refus affiché avec son propre message ; parcours navigateur sur 5174 — le message passe hors `attempt` (`App.vue:325`) ; le parcours (enregistrer, charger après combat, poser, relancer 8081) n'est pas enregistré
- [ ] P3·4 Docs à jour — `codebase-map.md:42` décrit encore « tiroirs catalogue / nouvelle carte » sans `SavedMapsPanel`, que l'Impact du plan prévoit pour `codebase-map.md`

### Findings

- `vtt-server/src/main/java/solovtt/server/Dtos.java:69` : deux javadocs successifs sur `SceneDto` ; seul le dernier s'attache, celui d'`imageId` devient orphelin → fusionner les deux phrases en un seul bloc — 🟢 minor, standards
- `vtt-web/src/components/SavedMapsPanel.vue:19` : après un enregistrement réussi, le nom reste dans le champ et `taken` affiche aussitôt « Remplace la carte enregistrée « Crypte » », comme si l'action restait à faire → vider `name` quand la liste revient avec ce nom (ou sur un événement de succès émis par `App.vue`) — 🟢 minor, frontend

## Round 2 · r-9df7

- Date: 2026-11-03
- By: Cyril Houillon
- Diff: `main...fe1c500`
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 10/10 met, 0 unmet, 0 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1·1 `MapStore.inMemory()` remplace une carte de même nom et liste par nom — `vtt-server/src/main/java/solovtt/server/MapStore.java:34`, `MapStoreTest.java:18`
- [x] P1·2 `JsonFileMapStoreTest` passe — `run.md`, `mvn -q test` sur `e49b6b1`, code identique à `fe1c500`
- [x] P1·3 `testing.md` donne la commande avec les trois chemins du scratchpad — `aidd_docs/memory/testing.md:27`
- [x] P2·1 Carte chargée neuve, aux positions, sans combat ni journal, nom rejoué ; entrée manquante sans changement ; partie sans nom rejouée — `SceneSession.java:399-410`, `:463`, `JsonFileGameStoreTest.java:91` ; `run.md` (chargement après combat, refus « Ruine »)
- [x] P2·2 Codes 201, 200, 400, 404, 409 rendus — `MapResource.java:38-78`, `MapApiTest`, `SceneWithoutMapTest`
- [x] P2·3 `mvn -q test` passe ; carte chargée reprise après relance — `run.md` (110 tests serveur, redémarrage de 8081)
- [x] P3·1 `npm run build` passe — `run.md`
- [x] P3·2 Champ, liste, avertissement de nom pris et confirmation affichés — `run.md`, parcours navigateur
- [x] P3·3 Refus affiché avec son propre message ; parcours enregistrer, charger après combat, poser et lancer le combat, retrouver après redémarrage — `run.md`
- [x] P3·4 Docs à jour ; story et plan clos — `codebase-map.md:42` cite `SavedMapsPanel`, story `status: done`, plan `status: implemented`
