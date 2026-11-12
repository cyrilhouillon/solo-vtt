# Review: choisir un historique en création guidée

- Rounds: 1
- Started: 2026-11-13

## Round 1 · r-b909

- Date: 2026-11-13
- By: Cyril Houillon
- Diff: `main...e84f8af` plus the working tree (`vtt-web/vite.config.ts`, `.mcp.json`)
- Axes: code, functional, relevancy
- Verdict: changes-requested
- Score: 7/11 met, 0 unmet, 4 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1·1 Chaque historique a caractéristiques, don, compétences, outil et équipement du SRD ; chaque don son texte — `Background.java:8-23`, `BackgroundChoiceTest.java:52`, `OriginFeatChoiceTest.java:26-38`
- [x] P1·2 Chaque choix impossible refusé avec sa raison ; un historique complet est valide — `BackgroundChoiceTest.java:191`, `:208`, `:227`, `OriginFeatChoiceTest.java:40` (écarts documentés dans `run.md` : plafond 20 inatteignable, Doué accepte moins de 3)
- [x] P1·3 Soldat For 17, Athlétisme, Arc court, Sauvagerie martiale ; Criminel +2 ; changement d'historique recalculé, retouche gardée ; sans historique, saisie gardée — `BackgroundChoiceTest.java:68`, `:87`, `:140`, `:169`, `:181`
- [x] P2·1 Construction avec historique et Polyvalent écrite, relue à l'identique ; fiche d'avant relue (`background` null, `feats` "") ; historique tronqué 409 — `SheetResourceTest.java:179`, `:680-681`, `:223`
- [x] P2·2 `GET backgrounds` et `GET origin-feats` avec sous-choix ; campagne inconnue 404 — `SheetResourceTest.java:150`, `:700`, `:723`
- Unverified at runtime: P3·1 `npm run build` passe ; options chargées au montage — `run.md` validé sur `e8ee101`, l'arbre de travail modifie depuis `vtt-web/vite.config.ts` (autre tête)
- Unverified at runtime: P3·2 Soldat remplit la fiche, éteint Athlétisme et Intimidation ; augmentations, boîte, équipement ; refus affiché et choix remis — même raison
- Unverified at runtime: P3·3 Humain : Doué et trois compétences, ou Initié à la magie, liste et incantation — même raison ; de plus `run.md` dit Initié par Polyvalent non cliqué
- Unverified at runtime: P3·4 « Dons » s'affiche, se retouche, survit au rechargement — même raison
- [x] P4·1 Chaque don d'origines non joué renvoie à une story existante — `lister-les-capacites-de-niveau-1-que-le-moteur-ne-joue-pas.md:60-65`, `relancer-les-degats-avec-sauvagerie-martiale.md`, `echanger-son-initiative-avec-vigilant.md`, `creer-en-guide-les-autres-lanceurs-de-sorts.md:19`
- [x] P4·2 La mémoire nomme classes et routes ; les deux stories sont `done` — `architecture.md`, `codebase-map.md:41`, `project-brief.md:52`, statuts des deux stories

### Findings

- `dnd-engine/src/main/java/dnd/engine/sheet/Build.java:95` : le même don peut être pris deux fois (Criminel + Polyvalent Vigilant, Soldat + Polyvalent Sauvagerie martiale, Acolyte + Polyvalent Initié Clerc), interdit par le SRD 5.2.1 « Feats » pour un don non répétable ; « Dons » l'écrit deux fois ; `GuidedCreation.vue` le propose aussi → refuser en 400 un don de Polyvalent égal à celui de l'historique (sauf Doué, et Initié avec une autre liste), l'éteindre côté client, tester la situation — 🟡 major, backend
- `dnd-engine/src/main/java/dnd/engine/sheet/GuidedValues.java:155` : `pick` garde la fiche quand la valeur dérivée devient `null` ; un Humain sans historique qui perd son don de Polyvalent (autre espèce, ou « à choisir ») garde « Vigilant — … » dans Dons alors que l'initiative perd le bonus → écrire `""` quand `previous` dérivait une valeur et que la nouvelle est `null`, avec un test — 🟡 major, backend
- `vtt-web/vite.config.ts:8` (arbre de travail) : `host: '0.0.0.0'` expose le serveur Vite et l'API proxifiée au réseau local, sans rapport avec la story → ne pas committer (ou garder en local) — 🟢 minor, fit
- `dnd-engine/src/main/java/dnd/engine/sheet/GuidedValues.java:49` : `of(Build)` passe `build.scores()` sans les augmentations, incohérent avec `abilities` dérivé ; seuls les tests l'appellent → `of(build, build.abilities())` — 🟢 minor, code-health
- `dnd-server/src/main/java/dnd/server/SheetStore.java:121` : une augmentation gardée à `null` (fichier édité) passe le contrôle, puis `Build.abilities()` lève une NPE (500 au lieu de 409) → ajouter `!increases().containsValue(null)` — 🟢 minor, error-handling
- `vtt-web/modules/dnd/GuidedCreation.vue:415` : `species.id === 'HUMAN'` duplique `Species.choosesOriginFeat()` → servir `choosesOriginFeat` dans `SpeciesOptionsDto` — 🟢 minor, architecture
- `dnd-server/src/main/java/dnd/server/DndDtos.java:143` : `Set.copyOf(skills)` rejette un doublon avec un message anglais brut (400 « duplicate element ») → `EnumSet`/`HashSet` ou refus explicite en français — 🟢 minor, error-handling
