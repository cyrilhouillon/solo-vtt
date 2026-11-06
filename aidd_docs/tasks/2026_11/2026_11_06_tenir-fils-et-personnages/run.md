# Run

## Phase 1 · Le cœur range des pages dans la section d'un module

- Date: 2026-11-06
- Validated on: dcd4342

- [pass] Task 1 — Une racine de section garde sa section après relecture ; une fiche d'avant se lit sans section — `JsonFileNotebookStoreTest.laSectionDUnModule_suitLesReglesDuPort_etSeRelit` (relue par un nouvel adaptateur, section `essai` ; une page ordinaire n'écrit pas `section`) et `uneFicheEcriteAvantLArbre_seLitALaRacine` (fiche sans `section` égale à `new Page(id, "Session 1", null, null)`) verts.
- [pass] Task 2 — Déplacer ou supprimer une racine de section rend 400 ; ses sous-pages vont et viennent librement — `NotebookStoreTest.sections` (mémoire et JSON) : refus « Une page de section ne se déplace pas » / « ne se supprime pas », liste inchangée, corbeille vide ; sous-page déplacée, supprimée, restaurée. `NotebookApiTest.uneRacineDeSection_…` : `PUT …/parent` et `DELETE` sur la racine → 400 avec ce message ; Aldric déplacé sous elle → 204, supprimé → 204.
- [pass] Task 3 — `vtt-module-api` compile sans dépendance ; le port injecté crée une racine visible dans `GET …/notebook` — `mvn -q test -pl vtt-module-api` exit 0 ; `NotebookApiTest` : `pages.section` = `[null, "essai"]`, `GET …/pages/{id}` `section` = `essai` ; campagne inconnue → `IllegalArgumentException`.
- [pass] Task 4 — `mvn -q test -pl vtt-server -am` vert — `mvn test -pl vtt-server` : `Tests run: 231, Failures: 0, Errors: 0, Skipped: 0`, BUILD SUCCESS.
- [pass] Task 5 — La mémoire décrit le marqueur, ses règles et le port — `architecture.md` (paragraphe des modules : `NotebookPages`, `section`, refus, DTO) et `codebase-map.md` (`NotebookPages`, `NotebookPagesAdapter`) ; `grep -ril mythic vtt-server/src` vide.

## Phase 2 · Mythic crée ses listes Fils et Personnages

- Date: 2026-11-06
- Validated on: aeef9e3

- [pass] Task 1 — Régler le chaos ne perd pas les ids, et l'inverse — `MythicResourceTest.leChaosEtLesListes_surviventLUnALAutre` (chaos 7 puis listes : chaos 7 ; chaos 3 : mêmes ids, 2 pages) ; `leChaosSeGarde_parCampagne` garde `{"chaos":7}` à l'octet près (`NON_NULL`).
- [pass] Task 2 — Deux appels créent exactement deux pages, jamais de doublon, même renommées ou sans `mythic.json` — `lesListes_sontCreeesUneFois_videsDansLaSectionMythic` (Fils, Personnages, module `mythic`, contenu `EMPTY_LIST`, second appel : mêmes ids, 2 racines ; autre campagne : 4), `uneListeRenommee_estGardee`, `desIdsPerdus_reprennentLesRacinesParLeurTitre_sansDoublon` (fichier abîmé puis absent : mêmes ids, chaos 5), `unIdGardeDUnePageDisparue_creeUneNouvellePage`, `lesListesDUneCampagneInconnue_sont404`.
- [pass] Task 3 — `mvn -q test -pl mythic-server -am` vert — `mvn test -pl mythic-server` : `Tests run: 23, Failures: 0, Errors: 0, Skipped: 0` ; assemblage : `mvn test -pl vtt-server` (MythicResource injecté avec `NotebookPagesAdapter`) `Tests run: 231, Failures: 0`, BUILD SUCCESS.
- [pass] Task 4 — La mémoire décrit la route et l'état gardé — `architecture.md` (contenu de `mythic.json`, `POST …/mythic/lists`, reprise par id puis titre) et `codebase-map.md` (listes de `MythicResource`).
