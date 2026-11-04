# Review: créer et ouvrir une campagne depuis l'accueil

- Rounds: 1
- Started: 2026-11-04

## Round 1 · r-7c3e

- Date: 2026-11-04
- By: Cyril Houillon
- Diff: `main...4da41b5`
- Axes: code, functional, relevancy
- Verdict: changes-requested
- Score: 4/11 met, 1 unmet, 6 unverified at runtime, 0 out of the diff

### Criteria

- [x] Phase 1 · T1 — `CampaignStoreTest` : créer, lister par nom, trouver, partie et cartes séparées par campagne — `vtt-server/src/test/java/solovtt/server/CampaignStoreTest.java:20`, `:33`
- Unverified at runtime: Phase 1 · T2 — `FileCampaignStoreTest` (relecture, isolation, dossier illisible ignoré, id inconnu refusé) et `mvn -q test` passe — tests présents (`FileCampaignStoreTest.java:69`), aucun run enregistré sur ce head
- Unverified at runtime: Phase 2 · T1 — aller-retour et session neuve identiques, `ResumeGameTest` / `CombatDeBoutEnBoutTest` passent sans changement, session CDI sans campagne refuse `create` — tests présents (`CampaignSessionTest.java:28`, `:40`), les deux classes non modifiées ; seul un run montre qu'ils passent
- [ ] Phase 2 · T2 — `CampaignApiTest` : 201, 400, 404, 204 et liste triée — le 204 de `GET /api/campaigns/current` n'est testé nulle part (seul `session.campaign()` vide l'est, `CampaignSessionTest.java:31`)
- [x] Phase 2 · T3 — carte enregistrée de A absente de B ; sans campagne, `/api/maps` rend 409 — `CampaignApiTest.java:103`, `MapResource.java:32` via `SceneSession.maps()` → `NoCampaignException extends ActionRefusedException` (409)
- Unverified at runtime: Phase 2 · T4 — `mvn -q test` passe en entier — aucun run enregistré
- [x] Phase 2 · T5 — mémoire à jour, sans référence aux producteurs supprimés — `architecture.md`, `codebase-map.md`, `testing.md` ; aucune occurrence de `GameStoreProducer`, `MapStoreProducer`, `solovtt.save.path`, `solovtt.maps.path` hors `aidd_docs/tasks`
- Unverified at runtime: Phase 3 · T1 — `npm run build` passe — aucun run enregistré
- Unverified at runtime: Phase 3 · T2 — écran de jeu inchangé hors barre, nom affiché, Accueil, retour forcé après redémarrage — code présent (`GameScreen.vue:324`, `:1092`), l'affichage demande un run
- Unverified at runtime: Phase 3 · T3 — parcours navigateur sur 5174 (deux campagnes, aller-retour avec décision, cartes séparées, redémarrage de 8081) — aucun run enregistré
- [x] Phase 3 · T4 — docs à jour, story et plan clos — `DESIGN.md`, `project-brief.md`, `codebase-map.md` ; story `status: done`, plan `status: implemented`

### Findings

- `vtt-server/src/main/java/solovtt/server/CampaignResource.java:55` : la branche 204 de `current()` n'a aucun test, alors que le client en dépend (`api.currentCampaign` → `null` → retour à l'accueil) → test unitaire `new CampaignResource(new SceneSession(Dice.scripted(), CampaignStore.inMemory()), …).current().getStatus() == 204` (un `@QuarkusTest` ne peut garantir l'absence de campagne, la session étant partagée) — 🟡 major, test-coverage
- `vtt-web/src/types.ts:46` : la doc « Une carte enregistrée, prête à charger. » reste au-dessus de celle de `CampaignView` ; `SavedMapView` perd la sienne → remonter `CampaignView` au-dessus de ce commentaire — 🟢 minor, standards
- `vtt-server/src/main/java/solovtt/server/CampaignResource.java:44` : nettoyage et bornes du nom recopiés de `MapResource.nameOf` / `save` (même `MAX_NAME_LENGTH = 80`, mêmes messages) → une aide commune `Names.required(request, label)` — 🟢 minor, rot
- `vtt-web/src/components/HomeScreen.vue:122` : `.bar`, `.app-name`, `.icon-button`, `.error` recopiés des styles scoped de `GameScreen.vue` ; une retouche de la barre devra se faire deux fois → déplacer ces règles dans `style.css` — 🟢 minor, rot
- `vtt-server/src/main/java/solovtt/server/MapResource.java:69` : `session.maps().find(name)` puis `session.load(map)` sont deux sections synchronisées ; une ouverture de campagne entre les deux charge une carte de A dans B → acceptable en solo, sinon une méthode `SceneSession.load(String name)` — 🟢 minor, backend
- `vtt-server/src/test/java/solovtt/server/CampaignSessionTest.java:40` : le cas « partie illisible à l'ouverture d'une campagne » de la Test Scope (phase 2) n'a pas de test ; le comportement vient de `restore()`, déjà couvert par `ResumeGameTest` hors campagne → ajouter un `open` sur un `GameStore` illisible — 🟢 minor, test-coverage
