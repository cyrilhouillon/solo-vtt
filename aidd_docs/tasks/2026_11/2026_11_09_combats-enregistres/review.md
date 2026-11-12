# Review: combats enregistrés

- Rounds: 1
- Started: 2026-11-12

## Round 1 · r-7c2e

- Date: 2026-11-12
- By: Cyril Houillon
- Diff: `main...3f957c6` plus the working tree
- Axes: code, functional, relevancy
- Verdict: changes-requested
- Score: 10/11 met, 0 unmet, 1 unverified at runtime, 0 out of the diff

### Criteria

- [x] Le fichier du combat rejoué redonne scène et PV finals ; une partie d'avant sans `combatId` se reprend — `SavedGame.java:44-49`, `CombatsEnregistresTest.java:65`, `:188`, `JsonFileCombatStoreTest.java:49`
- [x] Le lancement crée « Combat N » (`combatId`) sous « Combats » ; contenu 400 ; renommer, déplacer, supprimer, restaurer comme une page — `CampaignSessions.java:35-36`, `NotebookStore.java` (`createCombatEntry`), `NotebookStoreTest.java:227`, `CombatsApiTest.java:63`, `:85`
- [x] `GET …/combats/{id}` rend état et scène finale ; `resume` reprend un combat en cours, 409 terminé, 404 inconnu — `CombatsResource.java:28-55`, `CombatsApiTest.java:118`, `:139`, `:158`
- [x] Le combat scripté donne les pas attendus ; le dernier pas égale `GET …/combats/{id}` — `CombatReplay.java:44-55`, `CombatReplayTest.java:48`
- [x] La route de rejeu rend un combat terminé, 409 en cours, 404 inconnu — `CombatsResource.java:60-72`, `CombatsApiTest.java:165`
- [x] Section « Combats » visible sans module ; aucune sous-page sous un combat ; build passe — `tree.ts:83`, `:123`, `PageBranch.vue:107`, run.md Final (b144269, seul du Markdown et `vite.config.ts` hors build changés depuis)
- Unverified at runtime: ouvrir une entrée combat (arbre, `[[`, onglet, recherche) montre sa miniature et le clic mène à l'écran du combat — un run sur la tête montrerait la miniature par les quatre chemins (run.md Phase 3 validé sur 79f4e3a, autre tête)
- [x] Combat terminé ouvert en rejeu, combat en cours en direct, repris s'il n'était pas courant — `CombatEntryView.vue:38-39`, `router.ts:34-39`, run.md Phase 4 Task 1
- [x] En rejeu, aucun geste ne modifie la partie — `ReplayScreen.vue:196` (deux GET), run.md Phase 4 Task 2 (`calls: []`)
- [x] Les cinq commandes ; 2 s après la fin de l'animation ; build passe — `ReplayScreen.vue:27`, `:87`, `:133`, run.md Phase 4 Task 3 (+2,00 s, +2,05 s)
- [x] La mémoire décrit combats, rejeu, reprise et section « Combats » — `architecture.md:46-47`, `codebase-map.md:45-46`, `project-brief.md:52`

### Findings

- `vtt-web/src/components/ReplayScreen.vue:133` : Espace, Espace pendant que les dés roulent, Espace : la boucle 1, bloquée dans `forward()`, relit `playing` remis à vrai et repart ; deux boucles avancent, `wait()` écrase `pause` et `stop()` ne coupe plus qu'un timer → numéro de lecture (`const run = ++playRun` ; `stop` incrémente), boucle sur `run === playRun` — 🟡 major, frontend
- `vtt-server/src/main/java/solovtt/server/CombatsResource.java:83` : un `combats/<id>.json` corrompu lève `UncheckedIOException` (`JsonFileGameStore.java:47`), non attrapée ni mappée : 500 au lieu du 409 « illisible » que le Carnet sait dire → l'attraper dans `game()` en `ActionRefusedException` — 🟡 major, error-handling
- `vtt-server/src/main/java/solovtt/server/SceneSession.java:277` : si `combatStarted` échoue (E/S du Carnet), le combat est lancé et sauvé mais la requête rend 500 et le combat n'a jamais d'entrée, inaccessible dès qu'il n'est plus courant → attraper et journaliser, ou créer l'entrée manquante à la lecture du Carnet — 🟡 major, error-handling
- `vtt-web/src/components/ReplayScreen.vue:77` : reculer, Début ou Fin pendant un lancer laisse rouler les dés du pas abandonné sur la carte → exposer l'annulation de `DiceRoller.cancel()` dans `MapView` et l'appeler dans `show()` — 🟢 minor, frontend
- `vtt-web/src/components/ReplayScreen.vue:196` : `Promise.all` : un échec de `notebook()`, qui ne sert qu'au titre, empêche tout le rejeu → attraper son erreur et garder « Combat » — 🟢 minor, error-handling
- `vtt-server/src/main/java/solovtt/server/CombatsResource.java:54` : `resume` rejoue la partie trois fois, `replay` deux fois (`:70`) → vérifier « terminé » une seule fois sur la session déjà rejouée — 🟢 minor, performance
- `vtt-web/src/components/ReplayScreen.vue:328` : ~150 lignes de CSS de mise en page, `TABS`, `measure` et l'étiquette d'impact copiées de `GameScreen.vue` ; la copie a déjà dérivé (`.tab-panel:focus-visible` manque) → mise en page commune, `impactLabel` dans `dice/tray.ts` — 🟢 minor, rot
- `vtt-web/src/components/GameScreen.vue:134` : enveloppe locale `trayFromJournal` qui ne fait que passer `journal.value` → appeler celle de `dice/tray.ts` — 🟢 minor, rot
- `vtt-web/src/dice/tray.ts:1` : importe `TrayResult` de `DiceTray.vue`, alors que `src/dice/` est « sans dépendance à Vue » (architecture.md) → déplacer le type dans `types.ts` — 🟢 minor, conform
- `vtt-server/src/main/java/solovtt/server/persistence/JsonFileCombatStore.java:40` : try/catch sur `checkId` en guise de filtre → prédicat `CombatStore.isId` — 🟢 minor, code-health
- `vtt-server/src/main/java/solovtt/server/Page.java:6` : `section` dit encore « l'id du module, que seul un module crée » alors que le cœur crée `combats` ; `checkModuleId` ne réserve pas `combats` → corriger la doc (et architecture.md) et réserver l'id — 🟢 minor, rot
- `aidd_docs/memory/architecture.md:40` : les routes de partie omettent `combats`, et les adresses (`:49`) omettent `/campagnes/<id>/combats/<combatId>/rejeu` → les ajouter — 🟢 minor, rot
- `vtt-server/src/main/java/solovtt/server/Dtos.java:131` : le pas 0 est l'état avant `StartCombat` (`CombatReplay.java:139`), mais doc et `ReplayScreen` disent « Lancement » → « Départ », « l'état avant le lancement » — 🟢 minor, rot
- `vtt-web/vite.config.ts:8` (arbre de travail) : `host` passe à `0.0.0.0`, exposant Vite et le proxy `/api` sur le réseau local, sans rapport avec la branche ; `.mcp.json` non suivi est local au poste → ne pas les committer ici (`.mcp.json` dans `.gitignore`) — 🟢 minor, fit
