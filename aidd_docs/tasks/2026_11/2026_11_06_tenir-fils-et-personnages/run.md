# Run

## Phase 1 · Le cœur range des pages dans la section d'un module

- Date: 2026-11-06
- Validated on: dcd4342

- [pass] Task 1 — Une racine de section garde sa section après relecture ; une fiche d'avant se lit sans section — `JsonFileNotebookStoreTest.laSectionDUnModule_suitLesReglesDuPort_etSeRelit` (relue par un nouvel adaptateur, section `essai` ; une page ordinaire n'écrit pas `section`) et `uneFicheEcriteAvantLArbre_seLitALaRacine` (fiche sans `section` égale à `new Page(id, "Session 1", null, null)`) verts.
- [pass] Task 2 — Déplacer ou supprimer une racine de section rend 400 ; ses sous-pages vont et viennent librement — `NotebookStoreTest.sections` (mémoire et JSON) : refus « Une page de section ne se déplace pas » / « ne se supprime pas », liste inchangée, corbeille vide ; sous-page déplacée, supprimée, restaurée. `NotebookApiTest.uneRacineDeSection_…` : `PUT …/parent` et `DELETE` sur la racine → 400 avec ce message ; Aldric déplacé sous elle → 204, supprimé → 204.
- [pass] Task 3 — `vtt-module-api` compile sans dépendance ; le port injecté crée une racine visible dans `GET …/notebook` — `mvn -q test -pl vtt-module-api` exit 0 ; `NotebookApiTest` : `pages.section` = `[null, "essai"]`, `GET …/pages/{id}` `section` = `essai` ; campagne inconnue → `IllegalArgumentException`.
- [pass] Task 4 — `mvn -q test -pl vtt-server -am` vert — `mvn test -pl vtt-server` : `Tests run: 231, Failures: 0, Errors: 0, Skipped: 0`, BUILD SUCCESS.
- [pass] Task 5 — La mémoire décrit le marqueur, ses règles et le port — `architecture.md` (paragraphe des modules : `NotebookPages`, `section`, refus, DTO) et `codebase-map.md` (`NotebookPages`, `NotebookPagesAdapter`) ; `grep -ril mythic vtt-server/src` vide.
