# Review: choisir une espèce en création guidée

- Rounds: 1
- Started: 2026-11-12

## Round 1 · r-7c3e

- Date: 2026-11-12
- By: Cyril Houillon
- Diff: `main...cb284e7` plus the working tree (`vtt-web/vite.config.ts`, `.mcp.json`)
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 10/10 met, 0 unmet, 0 unverified at runtime, 0 out of the diff

### Criteria

- [x] Phase 1 · 1 — Chaque espèce a la taille, la vitesse, la vision et les traits de sa note SRD ; chaque option son texte et son type de dégâts — `Species.java:227-259`, `SpeciesOption.java:457-498` (relus contre le SRD 5.2.1 : tailles, vitesses 9 / 10,5, vision 18 / 36 m, 10 dragons et leurs types) ; `SpeciesChoiceTest`, passé dans `run.md` § Final (`mvn verify`, 116 tests dnd-engine, 0 échec, 4fa0005)
- [x] Phase 1 · 2 — Chaque choix impossible refusé avec sa raison ; une espèce seule valide — `SpeciesChoice.java:375-403`, `Build.java:72` ; `SpeciesChoiceTest.unSousChoixImpossible_estRefuseAvecLaRaison`, `uneCompetenceDejaMaitriseeParLaClasse_estRefusee`, `uneEspeceSeule_estValide`, passés dans `run.md` § Final
- [x] Phase 1 · 3 — Nain PV 12 ; Elfe sylvestre 10,5 et sa compétence ; changer d'espèce réécrit et garde une retouche inchangée ; sans espèce, la fiche garde ses saisies — `GuidedValues.java:62-82`, `:113-142` (`pick` garde la fiche pour un dérivé `null`) ; `GuidedValuesTest:127-193`, passés dans `run.md` § Final
- [x] Phase 2 · 1 — Construction avec espèce écrite et relue à l'identique ; `dnd.json` d'avant lu sans 409 — `DndDtos.java:121-166`, `SheetStore.java:116` ; `SheetResourceTest:620`, `:649`, passés dans `run.md` § Final (29 tests dnd-server)
- [x] Phase 2 · 2 — `GET …/dnd/species` rend les 9 espèces ; campagne inconnue 404 — `SheetResource.java:67-77` ; `SheetResourceTest:597`, passé dans `run.md` § Final
- [x] Phase 3 · 1 — `npm run build` passe ; types alignés sur les DTO — `sheet.ts:150-181` face à `SpeciesChoiceDto` / `SpeciesOptionsDto` ; `run.md` § Phase 3 (1d77211, seuls des `.md` changés depuis) : `✓ built`
- [x] Phase 3 · 2 — Chaque espèce du Test Scope remplit la fiche ; compétence déjà maîtrisée non proposable — `GuidedCreation.vue` (partie Espèce, `:disabled` croisés) ; `run.md` § Phase 3 : Nain PV 12 / 12, Elfe sylvestre 10.5, Perception éteinte, refus forcé affiché et liste remise
- [x] Phase 3 · 3 — Taille et traits retouchés survivent au rechargement — `CharacterSheetView.vue:233`, `:406` ; `run.md` § Phase 3 : Brin, `GET` puis rechargement identiques
- [x] Phase 4 · 1 — Chaque trait de niveau 1 des 9 espèces joué, porté, hors combat ou renvoyé à une story — `backlog/tasks/lister-les-capacites-…md` (26 lignes) et six stories créées
- [x] Phase 4 · 2 — La mémoire nomme chaque nouveau type, champ et route — `architecture.md` (paragraphe Espèce : `SpeciesChoice`, `Species`, `SpeciesOption`, `CreatureSize`, `size`, `speciesTraits`, `GET …/dnd/species`), `codebase-map.md` (`sheet/`), `project-brief.md`

### Findings

- `vtt-web/modules/dnd/GuidedCreation.vue:67` : un échec de `species()` écrase le message d'un échec de `classes()` (un seul `loadError`), et les deux appels partent l'un après l'autre → lancer les deux par `Promise.allSettled` et joindre les raisons — 🟢 minor, error-handling
- `vtt-web/modules/dnd/sheet.ts:167` : `SPELLCASTING_ABILITIES` recopie au client la règle de `SpeciesChoice.SPELLCASTING` (Int, Sag, Cha), alors que le serveur sert déjà les autres sous-choix → servir la liste dans `SpeciesOptionsDto` (au lieu de `spellcasting: boolean`) — 🟢 minor, rot
- `dnd-engine/src/main/java/dnd/engine/sheet/GuidedValues.java:205` : une construction qui retire l'espèce (`species: null` après Nain, possible par l'API seule) garde « Nain » et ses traits mais réécrit PV 12 → 11, la fiche devient incohérente → refuser le retrait d'une espèce déjà choisie (`PUT …/build`, 400), comme le client qui ne propose plus « — » — 🟢 minor, backend
- `vtt-web/vite.config.ts:8` : `host: '0.0.0.0'` (arbre de travail, hors branche) expose le Vite de développement et son proxy `/api` à tout le réseau local → le garder hors de cette PR, ou sur `127.0.0.1` — 🟢 minor, security
