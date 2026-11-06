# Run record

## Phase 1 · le cœur garde les données d'un module

- Date: 2026-11-06
- Validated on: 9f24119

- [pass] Task 1 — `mvn -q compile` vert ; `vtt-module-api` sans dépendance — `mvn -q compile` exit 0 ; `mvn dependency:tree -pl vtt-module-api` : `solovtt:vtt-module-api:jar:0.1.0-SNAPSHOT` seul, `BUILD SUCCESS`
- [pass] Task 2 — `mvn -q test -pl vtt-server` vert sur les scénarios ; un `moduleId` malveillant n'écrit rien hors du dossier — `mvn test` racine : vtt-server `Tests run: 223, Failures: 0, Errors: 0` ; `CampaignStoreTest` 5/5 (données par campagne, campagne inconnue, id `../x` et vide refusés, adaptateur `exists`/`read`/`write`), `FileCampaignStoreTest` 8/8 (`<campagne>/modules/mythic.json` seul fichier, relu par un nouvel adaptateur, `../evade` refusé sans dossier `modules` ni `evade.json`)
- [pass] Task 3 — les trois fichiers de mémoire nomment `vtt-module-api` et `ModuleData` — `grep -c` : `architecture.md` 2 / 1, `codebase-map.md` 1 / 1, `package.md` 2 / 1
