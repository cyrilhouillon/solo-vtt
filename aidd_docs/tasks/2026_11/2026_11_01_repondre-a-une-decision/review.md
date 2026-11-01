# Review: répondre à une décision du moteur

- Rounds: 1
- Started: 2026-11-01

## Round 1 · r-b41e

- Date: 2026-11-01
- By: Cyril Houillon
- Diff: `main...1c601c4` plus the working tree (`.impeccable/critique/`, `.impeccable/review/`, non suivis, hors périmètre)
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 2/6 met, 0 unmet, 4 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1.1 attaque suspendue sans dégâts ; Bouclier lancé fait rater, refusé fait perdre 5 PV ; pose, retrait et attaque refusés pendant l'attente ; nouvelle carte efface la décision — `SceneSession.java:70` (`StandardRules.create()`), `SceneSession.java:88,138,152` (`requireNoDecision`), `ShieldReaction.java:28-41`, scénarios dans `DecisionApiTest`
- Unverified at runtime: P1.2 `DecisionApiTest`, `AttackApiTest`, `SceneApiTest`, `SceneWithoutMapTest` et `mvn -q test` passent — aucun run enregistré dans le dossier ; un `mvn -q test` sur `1c601c4` le montrerait
- Unverified at runtime: P2.1 `npm run build` passe — un build le montrerait
- Unverified at runtime: P2.2 dialogue ouvert sur la question et ses options, Échap sans effet, focus piégé, lisible en clair et sombre — `DecisionDialog.vue` (`@keydown.esc.prevent`, `@cancel.prevent`, réouverture sur `close`) ; l'écran demande un run
- Unverified at runtime: P2.3 parcours 5174 (Bouclier lancé ou refusé, Suppr / Ctrl+Z / clics sans requête, rechargement qui rouvre la question) — `App.vue` (`onKey` coupé, `pendingDecision` au montage et à la resynchronisation) ; aucun parcours enregistré
- [x] P3.1 noms cités présents, story `done`, plus de serveur sans Bouclier dans la mémoire — `DecisionPendingException`, `DecisionResource`, `DecisionDialog` suivis par git ; `repondre-a-une-decision-du-moteur.md:3` ; `architecture.md` sans `SceneSession.rules`

### Findings

- `vtt-web/src/components/DecisionDialog.vue:18` : `sync` ne déplace le focus que si le dialogue était fermé ; quand `ReactionWindow` enchaîne une seconde question (une autre créature) sur le dialogue déjà ouvert, le focus reste sur l'ancien bouton et la nouvelle question n'est pas annoncée → dans `sync`, focaliser la première option à chaque changement de `decision.id`, dialogue ouvert ou non — 🟢 minor, frontend
- `vtt-web/src/components/DecisionDialog.vue:13` : `firstOption` désigne la liste de tous les boutons d'option → renommer `optionButtons` — 🟢 minor, standards
- `dnd-engine/src/main/java/dnd/engine/rules/AttackHitReactions.java:36` : « %s est touchée » accorde au féminin toute créature (« Garde est touchée ») → une tournure neutre (« Coup au but sur %s (%d contre CA %d). ») — 🟢 minor, standards
- `vtt-server/src/main/java/solovtt/server/SceneResource.java:31` : le contenu d'erreur `@Content(mediaType = "application/json", schema = @Schema(implementation = Dtos.ErrorDto.class))` est recopié 13 fois dans quatre ressources → déclarer une réponse d'erreur nommée une fois (`@Components(responses = …)` sur une classe `@OpenAPIDefinition`) et la référencer par `ref` — 🟢 minor, rot
- `aidd_docs/tasks/2026_11/2026_11_01_repondre-a-une-decision/plan.md:29` : la fenêtre de réaction (`Reaction`, `ReactionWindow`, `AttackHitReactions`) et l'exposition OpenAPI sont sur la branche sans figurer au plan ni au backlog ; seule la mémoire les trace → les ajouter aux Decisions du plan, ou les décrire dans la PR — 🟢 minor, fit
