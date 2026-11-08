# Run

## Phase 1 · Le moteur Mythic teste une scène attendue et ajuste le chaos en fin de scène

- Date: 2026-11-08
- Validated on: a439a82

- [pass] Task 1 — `FateQuestionTest` vert sans changer un dé — `git diff 4e80404 -- FateQuestionTest.java` vide ; `mvn test -pl mythic-engine` : `Tests run: 14, Failures: 0 -- in mythic.engine.FateQuestionTest`
- [pass] Task 2 — Relances 7-10, doublon et contraire suivent la règle du livre (p. 72) — `SceneAdjustmentTest` : `Tests run: 6, Failures: 0` (9, 2, 10, 2 → Ajouter un personnage ×2 ; 7, 2, 1 → Ajouter un personnage seul ; 8, 1, 6 → deux ajustements)
- [pass] Task 3 — Seuil et parité suivent la table du livre (p. 67) ; l'interruption porte un événement complet — `SceneCheckTest` : `Tests run: 7, Failures: 0` (6 au chaos 5 attendue, 10 au chaos 9 attendue, 1 au chaos 1 modifiée, 4 au chaos 4 interrompue, focus + sens + ligne de Personnages)
- [pass] Task 4 — −1 / +1, bornes 1 et 9 tenues — `ChaosFactorTest.enFinDeScene_leControleGardeBaisse_etPerduMonte_entre1Et9` vert
- [pass] Task 5 — `mvn -q test -pl mythic-engine -am` vert — exit 0 ; `Tests run: 65, Failures: 0, Errors: 0`
