# Review: voir les dés rouler et le résultat sur la carte

- Rounds: 1
- Started: 2026-11-03

## Round 1 · r-7c3a

- Date: 2026-11-03
- By: Cyril Houillon
- Diff: `main...60601d6`
- Axes: code, functional, relevancy
- Verdict: changes-requested
- Score: 7/11 met, 0 unmet, 4 unverified at runtime, 0 out of the diff

### Criteria

- Unverified at runtime: P1.1 `mvn -q test` vert dans `dnd-engine`, texte du journal inchangé — le texte l'est statiquement (`GameEvent.java:74`, `notation()` = `values.size() + "d" + sides`) ; seul un run montre le vert
- [x] P1.2 `dice` sur chaque entrée de dés, `null` ailleurs ; champ dans l'OpenAPI — `SceneSession.java:101-107`, `Dtos.java:75`
- Unverified at runtime: P1.3 `mvn -q test` vert, nouveaux tests compris — tests présents (`AttackApiTest.java:77-79`, `:97-118`, `:150`), aucun run consigné
- Unverified at runtime: P2.1 `npm run build` sans erreur de type — types cohérents à la lecture (`types.ts:69-80`), aucun run consigné
- [x] P2.2 la piste reçoit les dés de l'action ; `live` faux au rechargement — `App.vue:91-103`, `:239`, `:272`
- Unverified at runtime: P2.3 valeurs à l'arrêt = entrées `dice`, hauteur de piste stable, aucun roulement sous `prefers-reduced-motion` ni au rechargement — logique présente (`DiceTray.vue:93-118`, `:233-236`), rendu à voir dans le navigateur
- [x] P3.1 jauge proportionnelle sur chaque jeton vivant, mise à jour après l'attaque — `SceneCanvas.ts:571-577`, `:416`, `App.vue:513`
- [x] P3.2 bulle à l'arrêt des dés, effacée seule, sans capter de clic, absente au rechargement — `SceneCanvas.ts:81`, `:583-607`, `DiceTray.vue:104-115`
- [x] P4.1 `DESIGN.md` ne dit plus « seul mouvement : le verdict » ; jauge et bulle décrites — `DESIGN.md:169`, `:284`, `:293`
- [x] P4.2 `architecture.md` et `project-brief.md` nomment `dice` et les fonctionnalités — `architecture.md:42`, `project-brief.md:40`
- [x] P4.3 story `done` aux critères amendés, plan `implemented` — story frontmatter et `## Acceptance`, `plan.md:3`

### Findings

- `vtt-web/src/App.vue:513` : les PV de la cible sont posés dès la réponse du serveur, avant l'arrêt des dés ; la carte (redessin `deep`, `MapView.vue:80-84`) baisse la jauge ou pose la croix « à terre », et la piste montre la jauge et « x / y PV » (`DiceTray.vue:140-144`, sans `waiting`) pendant les 600 ms du roulement : l'issue est révélée avant le verdict, contre le critère amendé de la story (« PV restants se posent sur la piste après l'arrêt des dés ») et le scénario de la phase 3 (« à l'arrêt des dés… sa jauge baisse ») → poser `target.hitPoints` dans `showImpact` (sur `landed`) pour un résultat `live`, et donner `waiting` à `.target` pendant `rolling` — 🟡 major, fit
- `vtt-web/src/components/DiceTray.vue:166` : `minmax(0, 1fr) auto auto` fait céder la colonne du verdict à 0 avant les dés et la cible quand la largeur manque : à largeur étroite, l'information principale se coupe la première, alors que `DESIGN.md` dit que ce sont les dés qui se coupent → réserver un minimum au verdict (`minmax(min(12rem, 100%), 1fr)`), à confirmer dans le navigateur — 🟢 minor, frontend
- `DESIGN.md:284` : double espace « atténuée  ; » → une seule espace — 🟢 minor, standards
