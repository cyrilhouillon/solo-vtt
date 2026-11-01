# Review: enchaîner les tours selon l'initiative

- Rounds: 1
- Started: 2026-11-02

## Round 1 · r-7c4e

- Date: 2026-11-02
- By: Cyril Houillon
- Diff: `main...4ea3e8f`
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 10/18 met, 0 unmet, 8 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1·1 `TurnOrder` sans import `dnd` ; candidat unique actif d'office — `vtt-engine/src/main/java/solovtt/engine/TurnOrder.java:34`, `TurnOrderTest.java:33`
- [x] P1·2 politique jouet « joueurs, puis MJ » hors `vtt-engine` — `vtt-engine/src/test/java/solovtt/engine/TurnOrderTest.java:70`
- Unverified at runtime: P1·3 `mvn -q test -pl vtt-engine` passe — aucun run enregistré sur ce head
- [x] P2·1 modificateur SRD par `initiative()` — `dnd-engine/src/main/java/dnd/engine/catalog/SrdCatalog.java:31`, `SrdCatalogTest.java:44`
- [x] P2·2 copie en combat de même `signature()`, aucun combat hors combat — `dnd-engine/src/main/java/dnd/engine/state/GameState.java:55`, `CombatTest.java:218`
- [x] P2·3 journal du lancement lisible — `dnd-engine/src/test/java/dnd/engine/CombatTest.java:58`
- [x] P2·4 ex æquo → « Qui joue ? », une fois par round, puis « Round 2 » — `CombatTest.java:105`, `CombatTest.java:76`
- Unverified at runtime: P2·5 tests d'attaque existants passent inchangés — inchangés dans le diff, le passage demande un run
- Unverified at runtime: P2·6 `mvn -q test` passe — aucun run enregistré
- [x] P3·1 pose entre dans l'ordre, retrait en sort, aucun refus en 500 — `vtt-server/src/main/java/solovtt/server/SceneSession.java:190`, `SceneSession.java:202`, `refusable` `SceneSession.java:133`, `CombatApiTest.java:132`
- [x] P3·2 trois opérations décrites avec leurs codes ; 409 en `ErrorDto` — `vtt-server/src/main/java/solovtt/server/CombatResource.java:22`, `ErrorMapper.java:25`
- Unverified at runtime: P3·3 `mvn -q test` passe, API comprise — aucun run enregistré
- Unverified at runtime: P4·1 `npm run build` passe — aucun run enregistré
- Unverified at runtime: P4·2 « Round 1 · Tour de … » et initiatives au clic — affichage
- Unverified at runtime: P4·3 créature active marquée dans le tableau et sur la carte, suit « Fin du tour » — affichage
- Unverified at runtime: P4·4 « Round 2 » après la dernière, état gardé au rechargement — affichage (`App.vue` relit `api.combat()` au montage et dans `resync`)
- [x] P5·1 mémoire non contredite par le code — `aidd_docs/memory/architecture.md`, `codebase-map.md` relus contre le diff
- [x] P5·2 story en `status: done` — `aidd_docs/backlog/stories/enchainer-les-tours-selon-l-initiative.md:3`

### Findings

- `vtt-web/src/App.vue:277` : retrait multiple en combat ; si le premier retrait est la créature active et ouvre « Qui joue ? », le `removeToken` suivant prend un 409, `resync` récupère mais les jetons déjà retirés ne vont pas dans `history` (Ctrl+Z perdu) et le reste n'est pas retiré → arrêter la boucle dès qu'un retrait ouvre une décision (relire `pendingDecision`) et pousser ce qui a été retiré — 🟢 minor, error-handling
- `dnd-engine/src/main/java/dnd/engine/combat/Turns.java:41` : `engine.ask("", …)` sert de sentinelle « pas de créature qui choisit », que `DecisionDialog.vue:54` lit implicitement par `chooserName` vide → rendre l'absence explicite (`chooserId` null documenté dans `DecisionDto`) — 🟢 minor, code-health
- `dnd-engine/src/main/java/dnd/engine/state/Combat.java:88` : `startTurn` accepte un non-candidat en le faisant seul candidat ; `Turns.start` en dépend pour le candidat unique (pas de `TurnOffered`), ce qui masque un appel incohérent → refuser un non-candidat quand un tour est proposé, et documenter le cas « candidat unique sans offre » — 🟢 minor, code-health
- `dnd-engine/src/main/java/dnd/engine/combat/JoinCombat.java:51` : l'aide privée `ranksBefore(total, modifier, current)` (la nouvelle créature passe avant) porte le nom de `InitiativeGroup.ranksBefore` au sens inverse (le groupe passe avant) → renommer, par ex. `ranksAhead` — 🟢 minor, standards
- `vtt-web/src/components/CombatControl.vue:14` : branche `creatureCount === null` (« Créer d'abord une carte. ») morte, le composant n'est rendu que `v-if="scene"` → typer `creatureCount: number` et retirer la branche — 🟢 minor, rot
- `vtt-web/src/components/JournalPanel.vue:17` : union de groupes réécrite à la main à côté du type `Item` → dériver `Item` ou nommer le type intermédiaire — 🟢 minor, rot
