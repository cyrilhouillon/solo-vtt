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

## Phase 3 · Le panneau ouvre et clôt une scène dans la page

- Date: 2026-11-08
- Validated on: b43e752 (code du client identique à 8f4e81b, seul du Markdown diffère)

- [pass] Task 1 — `npm run build` vert dans `vtt-web/` — `✓ built in 852ms`, `vue-tsc` sans erreur
- [pass] Task 2 — Chaque issue et la clôture s'écrivent en citation dans la page, que la recherche trouve — Chrome, Vite 5174 → 8081 (chemins du scratchpad), campagne neuve, tables de test (mots inventés), Personnages « Garde Ylo », « Session 1 » verrouillée : citations « Scène attendue : On entre au donjon (chaos 5) / d10 : 10 → Scène attendue » ; « Ajustement : Retirer un objet (d10 : 5) » ; « Ajustements : Ajouter un objet · Intensifier une activité (d10 : 8, 10, 9, 6, 10, 7, 4) » ; « d10 : 8 → Scène interrompue / Événement aléatoire : PNJ négatif — action1-083 · action2-060 / Personnages : Choisir — ligne vide … (d10 : 3 → ligne 2) » ; « Fin de scène : contrôle perdu → chaos 9 (déjà au maximum) », « Fin de scène : contrôle gardé → chaos 9 → 8 » ; page restée verrouillée (`contenteditable=false`) ; recherche « donjon » → « 1 page, Session 1 » avec l'extrait
- [pass] Task 3 — Le parcours du Test Scope passe au navigateur sur 5174 ; le chaos affiché suit la clôture — chaos 5 → 9 par +, puis 9 après « perdu », 8 après « gardé » ; rechargement : chaos 8, 19 citations gardées ; page déverrouillée, Tester puis Ctrl+Z : 20 → 19 citations (un pas) ; sans page ouverte : « Aucune page ouverte : ouvrir une page pour y écrire la scène. » et « … la fin de scène. », aucun appel `/scenes`, chaos 8 inchangé ; arrêt de 8081 et 5174, 8080 et 5173 toujours à l'écoute
- [pass] Task 4 — Mémoire et story à jour ; `grep -ri mythic vtt-server/src vtt-web/src` vide ; `mvn -q verify` vert — `DESIGN.md`, `architecture.md`, `codebase-map.md`, `project-brief.md` et story `status: done` dans b43e752 ; `grep -ril mythic vtt-server/src vtt-web/src | wc -l` → 0 ; `mvn -q verify` exit 0, rapports surefire : 435 tests, 0 échec, 0 erreur
