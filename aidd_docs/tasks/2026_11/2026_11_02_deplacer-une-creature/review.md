# Review: déplacer une créature dans la limite de sa vitesse

- Rounds: 1
- Started: 2026-11-02

## Round 1 · r-7c3a

- Date: 2026-11-02
- By: Cyril Houillon
- Diff: `main...feat/deplacer-une-creature`
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 13/20 met, 0 unmet, 7 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1·1 Sans Maj, pose sous le pointeur ; avec Maj, au centre de la case, aperçu compris — `vtt-web/src/map/SceneCanvas.ts:211` (`free = !shift`), `onShift` l. 85
- [x] P1·2 Pose à moins d'une case refusée, calée ou non ; case voisine acceptée — `vtt-web/src/map/grid.ts:27` (`< cell - EPSILON`), `vtt-web/src/App.vue:341`
- [x] P1·3 Barre d'état et refus décrivent Maj comme la touche qui cale — `vtt-web/src/App.vue:343`, `vtt-web/src/App.vue:851`
- Unverified at runtime: P1·4 `npm run build` passe — aucun run enregistré sur ce head
- [x] P2·1 Jeton déplacé dans la carte ; hors carte ou inconnu, refus sans changement — `vtt-engine/src/main/java/solovtt/scene/Scene.java:60`, `SceneTest`
- [x] P2·2 Déplacement dépensé dans la signature, remis à 0 au début du tour — `dnd-engine/src/main/java/dnd/engine/state/Combat.java:82`, `:106`
- [x] P2·3 Refus au-delà de la vitesse, à 0 PV ou entravée, sans événement ; sinon `CreatureMoved` — `dnd-engine/src/main/java/dnd/engine/combat/Move.java:57`
- Unverified at runtime: P2·4 `mvn -q test` passe, rejeu à signature égale — `MoveTest`, `CombatTest` présents, aucun run enregistré
- [x] P3·1 Libre hors journal ; active au journal — `vtt-server/src/main/java/solovtt/server/SceneSession.java:144`
- [x] P3·2 Codes 200/400/404/409, refus sans bouger — `vtt-server/src/main/java/solovtt/server/SceneResource.java:76`, `SceneSession.java:154` (`Move.of` avant `scene.move`)
- [x] P3·3 `movementLeft` baisse puis revient plein, dans l'unité de la carte — `vtt-server/src/main/java/solovtt/server/SceneSession.java:160`, `Dtos.java:109`
- Unverified at runtime: P3·4 `mvn -q test` passe — `MoveApiTest` présent, aucun run enregistré
- Unverified at runtime: P4·1 `npm run build` passe avec les nouveaux types — aucun run enregistré
- Unverified at runtime: P4·2 Distance parcourue et restante pendant le glisser, trop loin ou trop près visible avant le relâché — `drawMovePreview` présent ; l'affichage demande un parcours navigateur
- [x] P4·3 Au-delà de la vitesse restante refusé avec la raison ; dans la limite, journal et reste suivent — `vtt-web/src/App.vue:537`, `settleAction`
- Unverified at runtime: P4·4 Au clavier seul, l'active se déplace et le lecteur d'écran annonce distance et reste — `MapView.vue` `announce` ; l'annonce demande un lecteur d'écran
- Unverified at runtime: P4·5 Feuille de l'active avec la vitesse restante, pleine au début du tour — prop `movementLeft` câblée (`App.vue:903`) ; l'affichage demande un parcours
- [x] P5·1 Mémoire décrit le déplacement et Maj comme le code — `aidd_docs/memory/architecture.md:44`, `codebase-map.md`, `project-brief.md`
- [x] P5·2 `DESIGN.md` décrit chaque élément visuel, dans les deux thèmes (jetons laiton / `--danger`) — `DESIGN.md:290`
- [x] P5·3 Story `done`, plan `implemented` — frontmatter des deux fichiers

### Findings

- `vtt-web/src/map/SceneCanvas.ts:474` : le filtre `accept` de l'aimant recode la règle « trop loin » de `checkMove` (hypot + littéral `1e-6` au lieu de `EPSILON`) → passer `(p) => !checkMove(scene, token, p, limit).tooFar` — 🟢 minor, rot
- `dnd-engine/src/main/java/dnd/engine/events/GameEvent.java:222` : `describe` recalcule la vitesse restante avec `speed()` brute, en double de `Move.remaining` (vitesse effective) → exposer le calcul en un seul endroit (ex. `Combat`/`Creature`) et l'appeler des deux côtés — 🟢 minor, rot
- `dnd-engine/src/main/java/dnd/engine/state/Attack.java:44` : la tolérance d'allonge (61bd305) et l'aimant (8259c4d) sont livrés sur la branche sans ligne dans les Decisions du plan ; la mémoire les décrit, le plan non → ajouter deux lignes aux Decisions de `plan.md`, ou les citer dans la PR — 🟢 minor, fit
