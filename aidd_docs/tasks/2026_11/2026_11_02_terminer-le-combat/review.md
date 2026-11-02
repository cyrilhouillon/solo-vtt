# Review: terminer le combat à tout moment

- Rounds: 1
- Started: 2026-11-02

## Round 1 · r-7c3e

- Date: 2026-11-02
- By: Cyril Houillon
- Diff: `main...303b029`
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 6/11 met, 0 unmet, 5 unverified at runtime, 0 out of the diff

### Criteria

- [x] `CombatEnded` vide `state.combat()` et se décrit « Le combat se termine ». — `dnd-engine/src/main/java/dnd/engine/events/GameEvent.java:144`
- [x] `EndCombat.of` refuse hors combat sans émettre ; en combat, la résolution n'émet que `CombatEnded`. — `dnd-engine/src/main/java/dnd/engine/combat/EndCombat.java:18`
- Unverified at runtime: `mvn -q test` vert, nouveaux tests compris ; le rejeu d'un combat terminé redonne la même signature. — le test existe (`CombatTest.java:285`), aucun run consigné à côté du plan
- [x] En combat, `endCombat()` rend l'entrée « Le combat se termine » ; ensuite `combat()` est vide. — `vtt-server/src/main/java/solovtt/server/SceneSession.java:153`
- Unverified at runtime: `POST /api/combat/end` rend 200 avec `combat: null`, ou 409 avec la raison ; la route figure dans `/q/openapi`. — route et `@APIResponse` présentes (`CombatResource.java:51`) ; la présence dans `/q/openapi` demande un run
- Unverified at runtime: `mvn -q test` vert, nouveaux tests compris. — tests présents (`CombatApiTest.java:166`), aucun run consigné
- Unverified at runtime: `npm run build` passe ; « Confirmer » envoie `POST /api/combat/end`. — `api.ts:75` et `App.vue:592` câblent l'appel ; le build et l'onglet réseau demandent un run
- [x] Un seul clic ne termine pas le combat ; Annuler et Échap le laissent intact, focus sur le bouton. — `CombatControl.vue:43` (`requestEnd` n'émet rien), `CombatControl.vue:68`, `App.vue:720`
- Unverified at runtime: Après « Confirmer » : « Lancer le combat », journal « Le combat se termine », créatures et PV en place, feuille sans refus de tour. — affichage à l'écran ; `App.vue:595` recharge la feuille
- [x] `architecture.md`, `codebase-map.md` et `project-brief.md` nomment `EndCombat`, la route et la fonctionnalité, sans contradiction. — `aidd_docs/memory/architecture.md:36`, `aidd_docs/memory/codebase-map.md:35`, `aidd_docs/memory/project-brief.md:42`
- [x] `DESIGN.md` décrit le bouton et la confirmation ; la story est `done`. — `DESIGN.md:266`, `aidd_docs/backlog/stories/terminer-le-combat-a-tout-moment.md:3`

### Findings

- `vtt-web/src/components/CombatControl.vue:98` : « le relancer retire l'initiative » se lit aussi « enlève l'initiative » pour un lecteur d'écran → « le relancer fait tirer une nouvelle initiative » — 🟢 minor, standards
- `vtt-web/src/components/CombatControl.vue:144` : `.ghost`, `.confirm`, `.ask` et `.confirm .danger` recopient les styles de `TokenBoard.vue:511-538` → les remonter dans `src/style.css` (classes partagées) si une troisième confirmation apparaît — 🟢 minor, rot
