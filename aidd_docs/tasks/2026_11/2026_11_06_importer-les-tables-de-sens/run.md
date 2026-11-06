# Run

## Phase 1 · Le cœur garde les données communes d'un module

- Date: 2026-11-06
- Validated on: b2e902e

- [pass] Task 1 — `vtt-module-api` compile sans dépendance ; `ModuleData` inchangé — `vtt-module-api/pom.xml` : 0 `<dependency>` ; `git diff 60696ee~1 b2e902e -- …/ModuleData.java` vide ; `mvn -q test -pl vtt-server -am` exit 0.
- [pass] Task 2 — `mvn -q test -pl vtt-server -am` vert ; `FileCommonModuleDataTest` couvre écriture, relecture après redémarrage, absence et id invalide — surefire : 227 tests, 0 échec, 0 erreur ; `FileCommonModuleDataTest` : `Tests run: 4, Failures: 0, Errors: 0`.
- [pass] Task 3 — La mémoire décrit le port et la commande de test complète — `architecture.md` (port `CommonModuleData`, `solovtt.modules.path`), `codebase-map.md` (`vtt-module-api`), `testing.md` (`-Dsolovtt.modules.path=<scratchpad>/modules`).

## Phase 2 · Le moteur Mythic lit les tables et tire un sens

- Date: 2026-11-06
- Validated on: f7e144b

- [pass] Task 1 — Tirages Actions, Descriptions et élément rendent les mots des jets scriptés — `MeaningTablesTest` : `Tests run: 5, Failures: 0` (Actions 1/57, Descriptions 100/42, élément 3/3, élément inconnu refusé, entrée vide refusée).
- [pass] Task 2 — Chaque cas de refus rend sa raison et sa ligne ; BOM, CRLF, lignes vides tolérés — `MeaningTablesTextTest` : `Tests run: 10, Failures: 0` (99 entrées « ligne 102 », section absente, titre inconnu, texte avant titre, élément en double, nom vide, colonne en double, BOM/CRLF/espaces).
- [pass] Task 3 — `mvn -q test -pl mythic-engine -am` vert ; sans tables, ordre des dés et réponses inchangés — exit 0 ; `FateQuestionTest` : `Tests run: 10, Failures: 0` (7 cas d'avant avec `Optional.empty()`, sens 4/90 avec tables, deux dés seulement sans tables) ; `mvn -q verify -pl mythic-engine,mythic-server -am` exit 0, `MythicResourceTest` 9/9.
