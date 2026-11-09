# Review: ouvrir et clore une scène

- Rounds: 1
- Started: 2026-11-08

## Round 1 · r-644e

- Date: 2026-11-08
- By: Cyril Houillon
- Diff: `main...c02c6a8` plus the working tree (`.mcp.json` non suivi, hors du changement)
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 13/13 met, 0 unmet, 0 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1·1 `FateQuestionTest` vert sans changer un dé — test absent du diff ; `FateQuestion.java:29` délègue à `RandomEvent.roll` (`RandomEvent.java:20`), même ordre focus, sens, liste ; run.md Final (b43e752) : `mvn -q test` 435 tests, 0 échec
- [x] P1·2 Relances 7-10, doublon et contraire suivent le livre — `SceneAdjustment.java:56-63`, `SceneAdjustmentTest` (9,2,10,2 ; 7,2,1 ; 7,4,3)
- [x] P1·3 Seuil et parité ; l'interruption porte un événement complet — `SceneCheck.java:29-38`, `SceneCheckTest` (6/5, 10/9, 1/1, 4/4, focus + sens + ligne)
- [x] P1·4 −1 / +1, bornes 1 et 9 — `ChaosFactor.java:30`, `ChaosFactorTest.enFinDeScene_…`
- [x] P1·5 `mvn -q test -pl mythic-engine -am` vert — run.md Final (b43e752, seul du Markdown depuis)
- [x] P2·1 DTO lus en JSON sans champ inattendu — run.md phase 3 (b43e752) : les citations du navigateur lisent chaque champ de `Scene` et `SceneEnd`
- [x] P2·2 Trois issues avec leurs dés ; tests de question verts — `MythicResource.testScene`, `MythicResourceTest` (EXPECTED, ALTERED, INTERRUPTED, liste perdue) ; run.md Final
- [x] P2·3 Chaos gardé −1 / +1 aux bornes, ids de listes intacts — `MythicResource.java:186-195`, `finDeScene_controleGarde_…_etGardeLesListes`, `…_sansDepasser9`
- [x] P2·4 `mvn -q test -pl mythic-server -am` vert — run.md Final (b43e752)
- [x] P3·1 `npm run build` vert — run.md phase 3 et Final (b43e752) : `✓ built`, `vue-tsc` sans erreur
- [x] P3·2 Chaque issue et la clôture en citation, trouvée par la recherche — run.md phase 3 : citations attendue, ajustement(s), interrompue, fin de scène ; recherche « donjon » → Session 1
- [x] P3·3 Parcours au navigateur ; chaos affiché suit la clôture — run.md phase 3 : 9 → 9 → 8, rechargement à 8, Ctrl+Z en un pas, refus sans page sans appel
- [x] P3·4 Mémoire et story à jour, cœur sans « mythic », `mvn -q verify` vert — story `status: done`, `architecture.md`, `codebase-map.md`, `project-brief.md`, `DESIGN.md` ; `grep -ril mythic vtt-server/src vtt-web/src` → 0 ; run.md phase 3 `mvn -q verify` exit 0

### Findings

- `vtt-web/modules/mythic/MythicPanel.vue:307` : le rappel de l'événement (focus, sens, ligne tirée) recopie celui de la réponse (l. 288-298), alors que `result.ts` vient justement de factoriser `eventParagraphs` → extraire un petit composant ou une fonction `eventText(event)` partagée par les deux branches — 🟢 minor, rot
- `mythic-server/src/main/java/mythic/server/MythicResource.java:202` : `MAX_QUESTION_LENGTH` borne désormais aussi la scène attendue ; le nom ment → renommer en `MAX_TEXT_LENGTH` — 🟢 minor, standards
- `vtt-web/modules/mythic/MythicPanel.vue:174` : si la page se ferme pendant l'appel, le chaos a changé au serveur mais `write` affiche seulement « Aucune page ouverte… », sans dire que la scène est close ; fenêtre étroite → dans ce cas, préfixer le refus de « Chaos a → b ; » — 🟢 minor, error-handling
