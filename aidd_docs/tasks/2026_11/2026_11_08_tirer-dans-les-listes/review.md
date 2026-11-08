# Review: laisser un événement aléatoire tirer dans ses listes

- Rounds: 1
- Started: 2026-11-08

## Round 1 · r-7c3a

- Date: 2026-11-08
- By: Cyril Houillon
- Diff: `main...65d3449`
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 13/13 met, 0 unmet, 0 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1·1 `git status` ne montre plus `documents/` — `.gitignore:15` ; `git check-ignore -v documents/x` → `.gitignore:15:documents/`
- [x] P1·2 Les six focus de liste et les six autres rendent la bonne liste — `EventFocus.java:37-43` ; run.md Final (d83226f, seul du Markdown depuis) : `mythic-engine` 51 tests, 0 échec
- [x] P1·3 Sections et lignes suivent la table du livre, lignes vides finales exclues — `AdventureList.java:52-63` (`lastIndexOf(true)`, dé de section `2 × sections`, d10 par paires) ; run.md Final
- [x] P1·4 Anciens tests gardent leurs dés ; focus sans liste ne lance rien de plus — `FateQuestion.java:27-30` (`focus.list().map(…)`, dés de liste après le sens) ; run.md Final
- [x] P1·5 `mvn -q test -pl mythic-engine -am` vert — run.md Final : `mvn verify` exit 0
- [x] P2·1 Seule une racine vivante de la section rend son contenu ; `pageTitle` d'une page vivante seulement — `NotebookPagesAdapter.java:34-45` (`find` ne rend que les vivantes, filtre `section`) ; run.md Final : `vtt-server` 232 tests, 0 échec
- [x] P2·2 Liste à 3 éléments (vide, lien) → 3 lignes justes ; texte et sous-listes hors lignes — `ListLines.java:42-79` ; run.md Final : `mythic-server` 33 tests, 0 échec
- [x] P2·3 La réponse porte l'entrée, « Choisir » ou la liste vide, avec ses dés — `MythicDtos.java:64-77`, `MythicResource.java:152-158` ; run.md Final
- [x] P2·4 Tests verts ; la mémoire décrit le port et le tirage — run.md Final ; `architecture.md` (`sectionRootContent`, `pageTitle`, `AdventureList.draw`, `ListLines`), `codebase-map.md` (`AdventureList`, `ListDraw`, `ListLines`, port)
- [x] P3·1 `npm run build` vert — run.md Phase 3 (ef29904, seul du Markdown depuis) et Final : `✓ built in 929ms`
- [x] P3·2 Entrée, « Choisir » ou Contexte actuel écrits dans la page, lien compris, trouvés par la recherche — run.md Phase 3, Task 2 (citations observées, lien `<a href=…/carnet/352a8963…>`, recherche « ylo » → Session 1)
- [x] P3·3 Le panneau montre la même entrée que la page — run.md Phase 3, Task 3 (« Personnages : Vessa » dans le panneau et la dernière citation)
- [x] P3·4 Mémoire et story disent le tirage ; `grep -ri mythic vtt-server/src vtt-web/src` vide — story `status: done`, `project-brief.md` ; `grep -ril mythic vtt-server/src vtt-web/src | wc -l` → 0

### Findings

- `mythic-server/src/main/java/mythic/server/MythicResource.java:152` : chaque question relit `mythic.json`, les deux fiches de liste et le titre de chaque lien, alors que seule une minorité déclenche un événement visant une liste → passer au moteur un fournisseur paresseux (`Function<Kind, AdventureList>`) pour ne lire la liste que si le focus la vise — 🟢 minor, performance — corrigé : `FateQuestion.ask` reçoit `Function<Kind, AdventureList>`, la ressource lit à la demande (`EnumMap.computeIfAbsent`)
- `mythic-server/src/main/java/mythic/server/MythicResource.java:152` : l'état est lu deux fois par question (`chaosOf` puis `state`) → lire `State` une fois et en tirer le chaos — 🟢 minor, code-health — corrigé : `State` lu une fois, `chaos(State)`
- `mythic-engine/src/main/java/mythic/engine/FateQuestion.java:20` : les surcharges `ask(odds, chaos, tables, dice)` et `RandomEvent(focus, focusRoll, meaning)` (`RandomEvent.java:12`) ne servent qu'aux tests, et la première change désormais le sens d'un focus PNJ ou fil en « Contexte actuel » sans le dire → les retirer, les tests passant `Map.of()` et `Optional.empty()` — 🟢 minor, rot — corrigé : surcharges retirées, tests sur `SANS_LISTES` ; `mvn -q verify` exit 0, 411 tests, 0 échec
