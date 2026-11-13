# Run

## Phase 1 · PV perdus dans l'histoire de la fiche et port `sustained`

- Date: 2026-11-13
- Validated on: dc8a097

- [pass] Task 1 — Une histoire avec `HitPointsLost` se plie en PV baissés, jamais sous 0, construction et jets intacts — `SheetHistoryTest.desPvPerdus_baissentLesPvActuels_etGardentConstructionEtJets` (seuls les PV changent, construction et jets égaux), `desPvPerdus_seComposentAvecUneRetouche_etNeDescendentPasSous0` (retouche 10 puis −3 → 7 ; 7 − 20 → 0), `desPvPerdus_sontPositifs` (0 et −2 refusés) ; `mvn test` dnd-engine `Tests run: 138, Failures: 0`
- [pass] Task 2 — Un coup envoyé à `SheetCharacters` baisse la fiche lue par `GET` et par `take`, relue par une autre instance ; une page qui n'est pas une fiche rend `false` sans écrire ; un document abîmé lève 409 sans être écrasé — `SheetCharactersTest.unCoupRecu_…` (PV − 5, `dnd.json` contient `{"type":"lost","amount":5}`, nouvelles `SheetResource` et `SheetCharacters` : `read` et `take` à PV − 5), `unCoup_seComposeAvecUneRetouche_…` (10 → 7 → 0), `uneAutrePage_…` (fiche à la corbeille, id inconnu, page ordinaire : `false`, document inchangé), `sansDegats_…` (liste vide, autre événement, dégâts 0 : `true`, rien écrit), `unDocumentAbime_refuseLeCoupEn409_sansEtreEcrase`, `desPvPerdusSansMontant_…` (409) ; dnd-server `Tests run: 39, Failures: 0`. Injection de `SheetResource` dans `SheetCharacters` : le démarrage Quarkus des tests de vtt-server passe, `Tests run: 282, Failures: 0`, `BUILD SUCCESS` à la racine
