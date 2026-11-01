# Review: préparer une scène de combat

- Rounds: 1
- Started: 2026-11-01

## Round 1 · r-4e99

- Date: 2026-11-01
- By: Cyril Houillon
- Diff: `main...e7b209d`
- Axes: code, functional, relevancy
- Verdict: changes-requested
- Score: 1/10 met, 0 unmet, 9 unverified at runtime, 0 out of the diff

### Criteria

- Unverified at runtime: `SceneTest` passe ; `vtt-engine` sans import `dnd` ni dépendance runtime — aucun import `dnd` dans `vtt-engine/src/main/java/solovtt/scene/` ; seul un run montrerait les tests verts et l'arbre de dépendances
- Unverified at runtime: `mvn -q test` passe ; `speed()` vaut 9 par défaut — `Creature.java:149` fixe 9 ; un run montrerait les tests verts
- Unverified at runtime: `SrdCatalogTest` passe ; chaque entrée a une arme et une attaque se résout — `SrdCatalog.java:28,35,41,46,52` donnent une arme à chaque entrée ; la résolution n'est visible qu'à l'exécution
- Unverified at runtime: `mvn -q verify` passe ; `quarkus:dev` répond sur `/api/catalog` — un run montrerait le build et la réponse HTTP
- [x] Deux placements d'une même entrée donnent deux créatures distinctes dans le `GameState` — `vtt-server/src/main/java/solovtt/server/SceneSession.java:50-56`
- Unverified at runtime: `SceneApiTest` passe ; chaque erreur rend un code HTTP et un message lisible — `ErrorMapper.java:12-20` mappe les erreurs ; un run montrerait les tests verts
- Unverified at runtime: `npm run build` passe dans `vtt-web` — un run montrerait le typage et le build
- Unverified at runtime: `SceneCanvas.ts` n'importe pas `vue` ; les jetons apparaissent aux coordonnées cliquées — aucun import `vue` dans `vtt-web/src/map/SceneCanvas.ts` ; la position à l'écran ne se voit qu'à l'exécution
- Unverified at runtime: le parcours du Test Scope se fait dans le navigateur — seul un passage dans le navigateur le montre
- Unverified at runtime: les commandes de `codebase-map.md` et `coding-assertions.md` s'exécutent ; plus de transport « indécis » — `PRODUCT.md` ne mentionne plus le transport indécis ; il faudrait lancer les commandes

### Findings

- `vtt-server/src/main/java/solovtt/server/Dtos.java:55` : la règle D&D « 1,5 m = 5 ft » est codée dans le serveur, hors du module du système, alors que la story « déplacer » en aura besoin côté règles → exposer `Creature.speedIn(DistanceUnit)` dans `dnd-engine` et la faire lire par le serveur — 🟡 major, rot
- `vtt-web/src/map/SceneCanvas.ts:16` : la case de 1,5 m / 5 ft (emprise D&D d'une créature moyenne) est fixée dans la carte générique, contre le principe 3 de `PRODUCT.md` → faire fournir le pas de grille par le serveur, ou documenter l'exception — 🟢 minor, conform
- `vtt-server/src/main/java/solovtt/server/SceneSession.java:69` : `catalogEntry` refait la recherche de `SrdCatalog.entry` (`SrdCatalog.java:63`), dont l'appel ne sert plus qu'aux tests → `SrdCatalog.find(id): Optional<CatalogEntry>` et supprimer la copie — 🟢 minor, rot
- `dnd-engine/src/main/java/dnd/engine/catalog/CatalogEntry.java:20` : `Kind.label()` n'est appelé nulle part, et `vtt-web/src/types.ts` reprend les libellés dans `KIND_LABELS` → sérialiser le libellé dans les DTO, ou supprimer `label` — 🟢 minor, rot
- `vtt-server/src/main/java/solovtt/server/Dtos.java:43` : deux formes JSON pour une longueur (`DistanceDto` avec `unit` et `unitSymbol`, `Measure` avec `unitSymbol` seulement) → un seul record de mesure — 🟢 minor, rot
- `vtt-server/src/main/java/solovtt/server/SceneSession.java:40` : `scene()` expose la `Scene` mutable hors verrou ; `SceneDto.of` la parcourt pendant qu'un `place()` peut l'écrire, et `CreatureResource.java:22` enchaîne trois appels verrouillés séparément → construire les vues dans des méthodes `synchronized` de `SceneSession` — 🟢 minor, architecture
- `vtt-server/src/main/java/solovtt/server/ErrorMapper.java:13` : toute `IllegalArgumentException` devient un 400 ; un bug interne passerait pour une saisie invalide → exception de validation dédiée, les autres en 500 — 🟢 minor, error-handling
- `vtt-server/src/main/java/solovtt/server/ErrorMapper.java:19` : `Response.Status.fromStatusCode` rend `null` pour un code hors enum, d'où une NPE dans le mapper → `RestResponse.ResponseBuilder.create(int, …)` — 🟢 minor, error-handling
- `vtt-web/src/App.vue:69` : les réponses `distance` et `sheet` ne sont pas ordonnées ; des clics rapides peuvent afficher une feuille périmée → ignorer les réponses d'une sélection dépassée — 🟢 minor, frontend
- `vtt-web/src/App.vue:56` : le jeton est ajouté à la scène affichée à l'arrivée de la réponse, même si une nouvelle carte a été créée entre-temps → ignorer la réponse si la scène a changé — 🟢 minor, frontend
- `vtt-web/src/map/SceneCanvas.ts:72` : `destroyChildren` détruit le jeton survolé sans `mouseleave`, le curseur `pointer` reste et masque le `crosshair` de pose → réinitialiser le curseur au début de `redraw()` — 🟢 minor, frontend
- `vtt-web/src/map/SceneCanvas.ts:105` : une `Konva.Line` par case, sans borne sur la taille de carte ; une très grande carte fige l'onglet → borner les dimensions, ou dessiner la grille en un seul `Konva.Shape` — 🟢 minor, performance
- `vtt-server/src/test/java/solovtt/server/SceneWithoutMapTest.java:10` : le 409 est testé en Java pur, pas en HTTP comme le prévoit `testing.md` → test `@QuarkusTest` avec une session vierge — 🟢 minor, conform
- `aidd_docs/tasks/2026_11/2026_11_01_preparer-scene-de-combat/phase-2.md` : le CORS prévu a été remplacé par le proxy Vite sans que la phase le note → consigner l'écart — 🟢 minor, fit
