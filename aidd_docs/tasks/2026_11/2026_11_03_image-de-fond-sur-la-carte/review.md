# Review: image de fond sur la carte

- Rounds: 1
- Started: 2026-11-03

## Round 1 · r-7c2a

- Date: 2026-11-03
- By: Cyril Houillon
- Diff: `main...e89c32b`
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 5/10 met, 0 unmet, 5 unverified at runtime, 0 out of the diff

### Criteria

- [x] Phase 1.1 — image relue après une nouvelle instance, dédupliquée, id forgé refusé — `FileImageStore.java:33-60` (motif `[0-9a-f]{64}`), `FileImageStoreTest`
- [x] Phase 1.2 — 201 + empreinte, mêmes octets et type au `GET`, 415 / 400 / 404 — `ImageResource.java:35-74`, `ImageApiTest`
- Unverified at runtime: Phase 1.3 — `mvn -q test` passe (mémoire et `testing.md` à jour) — aucun run enregistré sur e89c32b
- [x] Phase 2.1 — session reprise redonne image et grille ; ancienne partie sans `imageId` se rejoue — `SceneSession.java:421-422`, `ResumeGameTest`, `JsonFileGameStoreTest.unePartieGardeeAvantLesImages_seLitSansImage`
- [x] Phase 2.2 — image connue → `imageId` + `gridVisible: true`, inconnue → 400, `PUT /grid` permis pendant une décision, 409 sans carte — `SceneResource.java:39-59`, `SceneApiTest`, `DecisionApiTest.pendantUneDecision_laGrilleSeBasculeQuandMeme`
- Unverified at runtime: Phase 2.3 — `mvn -q test` passe — aucun run enregistré
- Unverified at runtime: Phase 3.1 — `npm run build` passe ; `/api/images` puis `/api/scene` avec `imageId` dans le réseau — `App.vue:291-292` et `api.ts:48-53` en place, aucun run
- Unverified at runtime: Phase 3.2 — 975 × 975 px : 30 cases → 32,5 px, 45 × 45 m / 150 × 150 ft — `backdrop.ts`, `NewMapForm.vue` en place, affichage non observé
- Unverified at runtime: Phase 3.3 — image remplit la carte, grille alignée, bascule, reprise après redémarrage — `SceneCanvas.ts:260-271`, rendu non observé
- [x] Phase 3.4 — `DESIGN.md`, `project-brief.md`, story `done`, plan `implemented`

### Findings

- `vtt-server/src/main/java/solovtt/server/SceneSession.java:152` : surcharge `create(width, height, unit)` gardée pour les seuls tests → appeler `create(…, null)` dans les tests et la supprimer — 🟢 minor, rot
- `vtt-server/src/test/java/solovtt/server/SceneApiTest.java` : le Test Scope de la phase 2 demande la bascule de grille en combat (200), seule la décision est testée → ajouter le cas combat lancé — 🟢 minor, test-coverage
- `vtt-web/src/map/SceneCanvas.ts:260` : l'image, posée sur le `Rect` du sol, recouvre la moitié intérieure de son trait `map-border` → dessiner le bord après l'image (ou l'image en retrait d'un demi-pixel) — 🟢 minor, frontend
- `vtt-web/src/components/NewMapForm.vue:96` : ternaire utilisé comme instruction (`typedField.value === 'cells' ? typeCells() : typeCellPixels()`) → `if / else` — 🟢 minor, standards
