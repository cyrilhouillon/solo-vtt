# Review: zoomer et déplacer la vue de la carte

- Rounds: 2
- Started: 2026-11-03

## Round 1 · r-306f

- Date: 2026-11-03
- By: Cyril Houillon
- Diff: `main...5e0bdf7`
- Axes: code, functional, relevancy
- Verdict: changes-requested
- Score: 3/7 met, 0 unmet, 4 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1.1 `npm run build` passe ; à `FIT_VIEW`, la carte s'affiche comme avant — build passé sur `5e0bdf7` (revue) ; `view.ts:35-58` : à zoom 1, même échelle d'ajustement et même origine centrée qu'avant (`Math.round((stage - length) / 2)`)
- Unverified at runtime: P1.2 molette et pincement zooment autour du pointeur, bornés à [×1, ×8], sans défiler ni zoomer la page ; poser, glisser, mesurer, cibler, catalogue visent sous le pointeur — un parcours zoomé sur 8081/5174
- Unverified at runtime: P1.3 boutons et + − 0 zooment, « Ajuster » et 0 rendent la carte entière, curseur clavier en vue, nouvelle carte entière (`App.vue:396` dans `startFresh`, appelé par création et chargement) — un parcours
- [x] P1.4 `DESIGN.md`, `architecture.md`, `codebase-map.md` décrivent le zoom — diff de ces trois fichiers
- Unverified at runtime: P2.1 glisser le fond déplace la vue (souris, deux doigts), un doigt fait défiler la page, un clic sans glisser garde son effet, catalogue et jetons gardent leur glisser — un parcours souris et toucher
- Unverified at runtime: P2.2 au calage, bouton du milieu décale la grille au pixel entier, bouton gauche déplace la vue ; consignes `App.vue:1204`, `GridAlignPanel.vue:83` disent le bouton du milieu — un parcours de calage
- [x] P2.3 `DESIGN.md`, `architecture.md`, `project-brief.md` et la story décrivent le geste — diff, story `status: done`

### Findings

- `vtt-web/src/map/SceneCanvas.ts:200` : le pincement n'est pas gardé par `Konva.isDragging()` ; un second doigt pendant le glisser tactile d'un jeton change la vue, le redessin (`redraw` → `layer.destroyChildren` → `Node.remove` → `stopDrag`) émet `dragend`, et `onTokenMove` envoie une position intermédiaire (en combat : vitesse dépensée et `CreatureMoved` au journal) → ignorer le geste à deux doigts si `Konva.isDragging()`, comme la molette (`:195`) — 🟡 major, frontend
- `vtt-web/src/App.vue:1204` : la consigne de calage de la barre d'état (et celle du tiroir, `GridAlignPanel.vue:83`) ne dit pas que glisser au bouton gauche déplace la vue, alors que la tâche 2.2 de la phase 2 le demande → ajouter « glisser pour déplacer la vue » — 🟢 minor, fit

## Round 2 · r-142d

- Date: 2026-11-03
- By: Cyril Houillon
- Diff: `main...09a29cd`
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 3/7 met, 0 unmet, 4 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1.1 `npm run build` passe ; à `FIT_VIEW`, la carte s'affiche comme avant — build passé sur `09a29cd` (revue) ; `view.ts:35-58` : à zoom 1, même échelle et même origine centrée qu'avant
- Unverified at runtime: P1.2 molette et pincement zooment autour du pointeur, bornés à [×1, ×8], sans défiler ni zoomer la page ; poser, glisser, mesurer, cibler, catalogue visent sous le pointeur — un parcours zoomé sur 8081/5174
- Unverified at runtime: P1.3 boutons et + − 0 zooment, « Ajuster » et 0 rendent la carte entière, curseur clavier en vue, nouvelle carte entière (`App.vue:396`, `startFresh`) — un parcours
- [x] P1.4 `DESIGN.md`, `architecture.md`, `codebase-map.md` décrivent le zoom — diff de ces trois fichiers
- Unverified at runtime: P2.1 glisser le fond déplace la vue (souris, deux doigts, pas pendant le glisser d'un jeton : `SceneCanvas.ts:203`), un doigt fait défiler la page, un clic sans glisser garde son effet — un parcours souris et toucher
- Unverified at runtime: P2.2 au calage, bouton du milieu décale la grille, bouton gauche déplace la vue ; les deux consignes le disent (`App.vue:1204`, `GridAlignPanel.vue:83`) — un parcours de calage
- [x] P2.3 `DESIGN.md`, `architecture.md`, `project-brief.md` et la story décrivent le geste — diff, story `status: done`

### Findings

- `vtt-web/src/map/SceneCanvas.ts:268` : `fit()` n'est pas gardé par `Konva.isDragging()`, contrairement à `zoomIn`/`zoomOut` ; la touche 0 pressée pendant le glisser d'un jeton à la souris redessine la carte, ce qui émet `dragend` et valide un déplacement intermédiaire → garder `fit()` comme `zoomAroundCenter` — 🟢 minor, frontend
