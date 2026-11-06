# Review: importer ses tables de mots et tirer un sens

- Rounds: 1
- Started: 2026-11-06

## Round 1 · r-7c4e

- Date: 2026-11-06
- By: Cyril Houillon
- Diff: `main...3bdcdef`
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 11/11 met, 0 unmet, 0 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1·1 `vtt-module-api` compile sans dépendance ; `ModuleData` inchangé — `vtt-module-api/pom.xml` : 0 `<dependency>` ; `ModuleData.java` absent du diff
- [x] P1·2 `mvn -q test -pl vtt-server -am` vert ; `FileCommonModuleDataTest` couvre écriture, relecture après redémarrage, absence, id invalide — `FileCommonModuleDataTest.java:22-55` ; run.md Final (e7de073, seul du Markdown depuis) : 377 tests, 0 échec
- [x] P1·3 La mémoire décrit le port et la commande de test complète — `architecture.md` (`CommonModuleData`, `solovtt.modules.path`), `testing.md` (`-Dsolovtt.modules.path=<scratchpad>/modules`)
- [x] P2·1 Tirages Actions, Descriptions et élément rendent les mots des jets scriptés — `MeaningTablesTest.java:18-32`
- [x] P2·2 Chaque refus rend sa raison et sa ligne ; BOM, CRLF, lignes vides tolérés — `MeaningTablesTextTest.java:40-88` ; la section absente n'a pas de ligne (`MeaningTablesText.java:57`), aucune ne la désignerait
- [x] P2·3 `mythic-engine` vert ; sans tables, ordre des dés et réponses inchangés — `FateQuestionTest.java:16-85` (`sansTables_…neLancePasDeDeDePlus`) ; run.md Final
- [x] P3·1 Tables relues par un nouveau `MeaningStore` ; document abîmé = sans tables — `MeaningResourceTest.java` (`lImport_…etSeGarde`, `unDocumentAbime_…`)
- [x] P3·2 `mvn -q verify` vert ; import mal formé en 400 sans écraser ; tirage sans tables en 409 — `MeaningResourceTest.java` (`unFichierMalForme_…`, `sansTables_…409`) ; `ErrorMapper.http` rend le statut ; run.md Final
- [x] P3·3 Sur 8081, texte accentué intact au tirage ; un événement porte son sens — run.md Phase 4 (e7de073) : « Évêque-009 · forêt-015 », « Événement ambigu — Évêque-016 · forêt-028 »
- [x] P4·1 Import commun aux campagnes et gardé au redémarrage ; Tirer écrit deux mots ; un événement écrit ses mots ; sans tables, le panneau dit pourquoi et l'oracle répond — run.md Phase 4 (e7de073)
- [x] P4·2 Builds verts ; aucun mot du livre ; mémoire, `DESIGN.md`, story, epic et plan à jour — run.md Phase 4 et Final ; diff : `DESIGN.md`, story `done`, epic « tranchée », plan `implemented`

### Findings

- `mythic-server/src/main/java/mythic/server/MeaningStore.java:34` : `find()` n'attrape que les erreurs JSON ; un `mythic.json` commun illisible sur disque (`UncheckedIOException` de `FileCommonModuleData.read`) fait échouer en 500 chaque question oui/non, alors que l'oracle doit rester utilisable sans tables → attraper aussi `UncheckedIOException` et rendre `Optional.empty()` — 🟢 minor, error-handling
- `mythic-server/src/main/java/mythic/server/MythicDtos.java:29` : `MythicDtos.ACTIONS` (`"ACTIONS"`) et `MeaningTables.ACTIONS` (`"Actions"`) portent le même nom pour deux valeurs différentes (sorte et libellé) → nommer la sorte par une enum (`MeaningKind`) ou des constantes `KIND_…` — 🟢 minor, standards
