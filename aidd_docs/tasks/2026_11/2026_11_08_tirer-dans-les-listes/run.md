# Run

## Phase 1 · Le moteur Mythic tire une ligne dans une liste

- Date: 2026-11-08
- Validated on: ba114d9

- [pass] Task 1 — `git status` ne montre plus `documents/` — `git status --short` : seul `?? .mcp.json` ; `git status --short --ignored documents` : `!! documents/`.
- [pass] Task 2 — Les six focus de liste et les six autres rendent la bonne liste — `EventFocusTest.lesFocusDePnj_tirentDansPersonnages_ceuxDeFil_dansFils_lesAutres_nullePart` vert.
- [pass] Task 3 — Les sections et les lignes suivent la table du livre, lignes vides finales exclues — `AdventureListTest` (10 tests) : 3 lignes, d10 6 → ligne 3 sans dé de section ; 7 lignes, d4 3 puis d10 1 → ligne 6 (exemple p. 44) ; d4/d6/d8/d10 à 10/11/16/21 lignes ; 25 lignes, d10 10 puis 10 → ligne 25 ; ligne vide → `blank` (Choisir) ; 5 écrites + 3 vides → pas de d4 ; « A », « A », « B » → deux paires sur trois pour « A » ; vide → `Empty` sans dé ; 26e ligne écrite → `Full` sans dé ; lignes vides au-delà de 25 ignorées.
- [pass] Task 4 — Les anciens tests gardent leurs dés ; un focus sans liste ne lance rien de plus — `FateQuestionTest` (14 tests) : les 12 appels existants gardent leurs dés scriptés (un focus PNJ sans listes rend `Empty(CHARACTERS)`, conforme à « Empty List ») ; dés de liste après ceux du sens (33, 25, 4, 90, 3 → Personnages ligne 2) ; focus 60 → Fils ; liste absente → `Contexte actuel` effectif, focus tiré gardé ; focus 1, 6, 11, 71, 81, 86 : aucun tirage, aucun dé de plus.
- [pass] Task 5 — `mvn -q test -pl mythic-engine -am` vert — `mvn -q test -pl mythic-engine,mythic-server -am` exit 0 : 74 tests, 0 échec (`mythic-engine` 51, `mythic-server` 23).

## Phase 2 · Le serveur Mythic lit les listes du Carnet et rend l'entrée tirée

- Date: 2026-11-08
- Validated on: ffe3ed1

- [pass] Task 1 — Seule une racine vivante de la section du module rend son contenu ; `pageTitle` rend le titre d'une page vivante seulement — `NotebookApiTest.lePortDesSections_neRendLeContenuQueDesRacinesVivantesDuModule_etLeTitreDesPagesVivantes` : contenu de Fils (`essai`) rendu ; racine d'`autre`, page ordinaire, id inconnu → vide ; `pageTitle` Aldric et Fils rendus, Vessa à la corbeille → vide ; id de module invalide et campagne inconnue → `IllegalArgumentException`. `NotebookApiTest` : 29 tests, 0 échec.
- [pass] Task 2 — Une liste à 3 éléments, dont un vide et un lien, rend 3 lignes justes ; texte et sous-listes hors lignes — `ListLinesTest` (4 tests) : « Garde Ylo » (marques gardées), ligne vide, lien lu « Vessa », lien à texte choisi, lien mort « page introuvable » ; paragraphe, citation et sous-liste ignorés, `orderedList` lue ; espaces seuls → ligne vide ; illisible, nul ou sans contenu → aucune ligne.
- [pass] Task 3 — La réponse porte l'entrée tirée, « Choisir » ou la liste vide, avec ses dés — `MythicResourceTest` (22 tests) : d10 5 → ligne 3 « Garde Ylo » ; d10 3 → lien Vessa d'origine, texte « Vessa » ; d10 8 → ligne 4 vide, sans entrée ; Fils vide → `CURRENT_CONTEXT`, `empty` ; contenu illisible ou id qui n'est pas une racine → vide. API réelle (Quarkus 8081, chemins du scratchpad, campagne « Essai listes », Personnages : Garde Ylo, [[Vessa]], Garde Ylo, ligne vide ; chaos 9 ; 2000 `POST …/mythic/questions` sur 127.0.0.1) : 200 et `randomEvent.list` = ligne 3 `entry` `[{"type":"text","text":"Garde Ylo"}]` ; ligne 2 `entry` `[{"type":"pageLink","attrs":{"pageId":…,"label":null}}]`, `entryText` « Vessa » ; ligne 5 `blank: true` ; « Clore un fil » → `empty: true`, `effectiveFocusLabel` « Contexte actuel » (le `JsonNode` se sérialise tel quel).
- [pass] Task 4 — `mvn -q test -pl vtt-server,mythic-server -am` vert ; la mémoire décrit le port et le tirage — `mvn -q test -pl vtt-module-api,vtt-server,mythic-engine,mythic-server` exit 0 : `vtt-server` 232 tests, `mythic-server` 33 tests, 0 échec ; `architecture.md` (port `sectionRootContent` / `pageTitle`, lignes d'une liste, sections, Choisir, liste vide, pleine, DTO) et `codebase-map.md` (`AdventureList`, `ListDraw`, `ListLines`, port).
