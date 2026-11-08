# Run

## Phase 1 · Le moteur Mythic teste une scène attendue et ajuste le chaos en fin de scène

- Date: 2026-11-08
- Validated on: a439a82

- [pass] Task 1 — `FateQuestionTest` vert sans changer un dé — `git diff 4e80404 -- FateQuestionTest.java` vide ; `mvn test -pl mythic-engine` : `Tests run: 14, Failures: 0 -- in mythic.engine.FateQuestionTest`
- [pass] Task 2 — Relances 7-10, doublon et contraire suivent la règle du livre (p. 72) — `SceneAdjustmentTest` : `Tests run: 6, Failures: 0` (9, 2, 10, 2 → Ajouter un personnage ×2 ; 7, 2, 1 → Ajouter un personnage seul ; 8, 1, 6 → deux ajustements)
- [pass] Task 3 — Seuil et parité suivent la table du livre (p. 67) ; l'interruption porte un événement complet — `SceneCheckTest` : `Tests run: 7, Failures: 0` (6 au chaos 5 attendue, 10 au chaos 9 attendue, 1 au chaos 1 modifiée, 4 au chaos 4 interrompue, focus + sens + ligne de Personnages)
- [pass] Task 4 — −1 / +1, bornes 1 et 9 tenues — `ChaosFactorTest.enFinDeScene_leControleGardeBaisse_etPerduMonte_entre1Et9` vert
- [pass] Task 5 — `mvn -q test -pl mythic-engine -am` vert — exit 0 ; `Tests run: 65, Failures: 0, Errors: 0`

## Phase 2 · Le serveur Mythic teste et clôt une scène

- Date: 2026-11-08
- Validated on: 241d688

- [pass] Task 1 — Les DTO se lisent en JSON sans champ inattendu — serveur 8081 (chemins du scratchpad), campagne neuve : `POST …/mythic/scenes {"expected":"  On entre au donjon "}` → `{"expected":"On entre au donjon","chaos":5,"roll":7,"outcome":"EXPECTED","outcomeLabel":"Scène attendue","adjustments":[],"adjustmentRolls":[],"randomEvent":null}` ; `POST …/scenes/end {"inControl":true}` → `{"inControl":true,"before":5,"after":4}` 200
- [pass] Task 2 — Les trois issues reviennent avec leurs dés ; les tests de question restent verts — 8081 : `ALTERED` avec `[{"code":"INCREASE_AN_ACTIVITY",…}]`, `adjustmentRolls [4]`, et deux ajustements `[7,8,2,2]` ; chaos 9 : `INTERRUPTED`, `randomEvent` Loin d'un fil → Contexte actuel, liste Fils vide ; scène de 501 caractères → 400 « Scène attendue trop longue » ; campagne inconnue → 404 ; `MythicResourceTest` 32 tests verts, questions comprises
- [pass] Task 3 — Le chaos gardé suit −1 / +1 aux bornes, ids de listes intacts — 8081 : `GET chaos` → 4 après contrôle gardé ; `mythic.json` = `{"chaos":4,"threadsPageId":"4225…","charactersPageId":"3510…"}` ; contrôle perdu à 9 → `{"before":9,"after":9}` ; corps `{}` → 400 « Contrôle de la scène absent »
- [pass] Task 4 — `mvn -q test -pl mythic-server -am` vert — `mvn test -pl mythic-server` : `Tests run: 43, Failures: 0, Errors: 0`, BUILD SUCCESS
