# Run record

## Phase 1 · le cœur garde les données d'un module

- Date: 2026-11-06
- Validated on: 9f24119

- [pass] Task 1 — `mvn -q compile` vert ; `vtt-module-api` sans dépendance — `mvn -q compile` exit 0 ; `mvn dependency:tree -pl vtt-module-api` : `solovtt:vtt-module-api:jar:0.1.0-SNAPSHOT` seul, `BUILD SUCCESS`
- [pass] Task 2 — `mvn -q test -pl vtt-server` vert sur les scénarios ; un `moduleId` malveillant n'écrit rien hors du dossier — `mvn test` racine : vtt-server `Tests run: 223, Failures: 0, Errors: 0` ; `CampaignStoreTest` 5/5 (données par campagne, campagne inconnue, id `../x` et vide refusés, adaptateur `exists`/`read`/`write`), `FileCampaignStoreTest` 8/8 (`<campagne>/modules/mythic.json` seul fichier, relu par un nouvel adaptateur, `../evade` refusé sans dossier `modules` ni `evade.json`)
- [pass] Task 3 — les trois fichiers de mémoire nomment `vtt-module-api` et `ModuleData` — `grep -c` : `architecture.md` 2 / 1, `codebase-map.md` 1 / 1, `package.md` 2 / 1

## Phase 2 · le moteur Mythic répond à une question

- Date: 2026-11-06
- Validated on: b94866f

- [pass] Task 1 — les 81 cases et les tranches de focus sont celles transcrites par l'utilisateur ; le test de cohérence passe sur chaque case — script Python comparant le collage de l'utilisateur aux lignes de `FateChart.java` : `IDENTIQUE` ; `FateChartTest` 6/6 (`chaqueCase_aDesSeuilsOrdonnes` sur 9 × 9, bornes de « Probable » chaos 5, « X » des deux côtés), `EventFocusTest` 2/2 (12 tranches, début et fin), `ChaosFactorTest` 3/3
- [pass] Task 2 — `mvn -q test -pl mythic-engine -am` vert sur les scénarios ; aucun test de destin à 2d10 — `mvn test -pl mythic-engine -am` : `Tests run: 18, Failures: 0, Errors: 0`, `BUILD SUCCESS` ; `FateQuestionTest` 7/7 (33 et 55 au chaos 5 déclenchent, 66 non, 100 déclenche au chaos 1, focus par second d100) ; `grep -rni 2d10 mythic-engine` : aucun
- [pass] Task 3 — la mémoire nomme `mythic-engine` et ses tests — `grep -c mythic-engine` : `codebase-map.md` 1, `package.md` 1, `testing.md` 1
