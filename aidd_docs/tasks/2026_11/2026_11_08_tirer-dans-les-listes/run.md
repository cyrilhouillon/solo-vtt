# Run

## Phase 1 · Le moteur Mythic tire une ligne dans une liste

- Date: 2026-11-08
- Validated on: ba114d9

- [pass] Task 1 — `git status` ne montre plus `documents/` — `git status --short` : seul `?? .mcp.json` ; `git status --short --ignored documents` : `!! documents/`.
- [pass] Task 2 — Les six focus de liste et les six autres rendent la bonne liste — `EventFocusTest.lesFocusDePnj_tirentDansPersonnages_ceuxDeFil_dansFils_lesAutres_nullePart` vert.
- [pass] Task 3 — Les sections et les lignes suivent la table du livre, lignes vides finales exclues — `AdventureListTest` (10 tests) : 3 lignes, d10 6 → ligne 3 sans dé de section ; 7 lignes, d4 3 puis d10 1 → ligne 6 (exemple p. 44) ; d4/d6/d8/d10 à 10/11/16/21 lignes ; 25 lignes, d10 10 puis 10 → ligne 25 ; ligne vide → `blank` (Choisir) ; 5 écrites + 3 vides → pas de d4 ; « A », « A », « B » → deux paires sur trois pour « A » ; vide → `Empty` sans dé ; 26e ligne écrite → `Full` sans dé ; lignes vides au-delà de 25 ignorées.
- [pass] Task 4 — Les anciens tests gardent leurs dés ; un focus sans liste ne lance rien de plus — `FateQuestionTest` (14 tests) : les 12 appels existants gardent leurs dés scriptés (un focus PNJ sans listes rend `Empty(CHARACTERS)`, conforme à « Empty List ») ; dés de liste après ceux du sens (33, 25, 4, 90, 3 → Personnages ligne 2) ; focus 60 → Fils ; liste absente → `Contexte actuel` effectif, focus tiré gardé ; focus 1, 6, 11, 71, 81, 86 : aucun tirage, aucun dé de plus.
- [pass] Task 5 — `mvn -q test -pl mythic-engine -am` vert — `mvn -q test -pl mythic-engine,mythic-server -am` exit 0 : 74 tests, 0 échec (`mythic-engine` 51, `mythic-server` 23).
