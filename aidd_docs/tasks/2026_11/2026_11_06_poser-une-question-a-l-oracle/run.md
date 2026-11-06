# Run record

## Phase 1 · le cœur garde les données d'un module

- Date: 2026-11-06
- Validated on: 9f24119

- [pass] Task 1 — `mvn -q compile` vert ; `vtt-module-api` sans dépendance — `mvn -q compile` exit 0 ; `mvn dependency:tree -pl vtt-module-api` : `solovtt:vtt-module-api:jar:0.1.0-SNAPSHOT` seul, `BUILD SUCCESS`
- [pass] Task 2 — `mvn -q test -pl vtt-server` vert sur les scénarios ; un `moduleId` malveillant n'écrit rien hors du dossier — `mvn test` racine : vtt-server `Tests run: 223, Failures: 0, Errors: 0` ; `CampaignStoreTest` 5/5 (données par campagne, campagne inconnue, id `../x` et vide refusés, adaptateur `exists`/`read`/`write`), `FileCampaignStoreTest` 8/8 (`<campagne>/modules/mythic.json` seul fichier, relu par un nouvel adaptateur, `../evade` refusé sans dossier `modules` ni `evade.json`)
- [pass] Task 3 — les trois fichiers de mémoire nomment `vtt-module-api` et `ModuleData` — `grep -c` : `architecture.md` 2 / 1, `codebase-map.md` 1 / 1, `package.md` 2 / 1

## Phase 2 · le moteur Mythic répond à une question

- Date: 2026-11-06
- Validated on: b94866f

- [pass] Task 1 — les 81 cases et les tranches de focus sont celles transcrites par l'utilisateur ; le test de cohérence passe sur chaque case — script Python comparant le collage de l'utilisateur aux lignes de `FateChart.java` : `IDENTIQUE` ; `FateChartTest` 6/6 (`chaqueCase_aDesSeuilsOrdonnes` sur 9 × 9, bornes de « Probable » chaos 5, « X » des deux côtés), `EventFocusTest` 2/2 (12 tranches, début et fin), `ChaosFactorTest` 3/3
- [pass] Task 2 — `mvn -q test -pl mythic-engine -am` vert sur les scénarios ; aucun test de destin à 2d10 — `mvn test -pl mythic-engine -am` : `Tests run: 18, Failures: 0, Errors: 0`, `BUILD SUCCESS` ; `FateQuestionTest` 7/7 (33 et 55 au chaos 5 déclenchent, 66 non, 100 déclenche au chaos 1, focus par second d100) ; `grep -rni 2d10 mythic-engine` : aucun
- [pass] Task 3 — la mémoire nomme `mythic-engine` et ses tests — `grep -c mythic-engine` : `codebase-map.md` 1, `package.md` 1, `testing.md` 1

## Phase 3 · le serveur Mythic tient le chaos et pose la question

- Date: 2026-11-06
- Validated on: 100be8a

- [pass] Task 1 — `mvn -q verify` vert ; `mythic` n'apparaît pas dans `vtt-server/src` — `mvn verify` racine : 7 modules `SUCCESS`, `BUILD SUCCESS` (vtt-server `Tests run: 223, Failures: 0`) ; le premier `grep -rni mythic vtt-server/src` trouvait l'id d'essai `"mythic"` dans `CampaignStoreTest` et `FileCampaignStoreTest`, remplacé par `"essai"` (d4c4106, 13/13 verts) ; ensuite : aucun résultat
- [pass] Task 2 — `mvn -q test -pl mythic-server -am` vert sur chaos par défaut, modifié, borné, question, événement, campagne inconnue — `Tests run: 9, Failures: 0, Errors: 0` dans `MythicResourceTest`, `BUILD SUCCESS`
- [pass] Task 3 — sur 8081 : chaos, question, redémarrage, 404 — serveur `quarkus:dev` 8081, campagnes dans le scratchpad : `GET …/mythic/chaos` campagne neuve `{"chaos":5}` 200 ; `PUT` 7 → 200, relu 7, fichier `modules/mythic.json` = `{"chaos":7}` ; seconde campagne 5 ; `PUT` 0 et 10 → 400 « Le facteur de chaos va de 1 à 9 » ; `POST …/questions` `LIKELY` → 200 `roll` 26, `Oui`, chaos 7, `randomEvent` `null` ; jet 66 au chaos 7 → `randomEvent` `NPC_ACTION` « Action d'un PNJ » ; sans `odds` / `PEUT_ETRE` → 400 ; campagne inconnue → 404 ; redémarrage du serveur → `{"chaos":7}` ; `mythic-server` retiré de `vtt-server/pom.xml` : vtt-server `Tests run: 223, Failures: 0`, `…/mythic/chaos` 404, `…/notebook` 200, puis pom restauré ; 8081 arrêté (jamais 8080)

## Phase 4 · le Carnet accueille le panneau d'un module

- Date: 2026-11-06
- Validated on: ec392ae

- [pass] Task 1 — build vert sans module ; avec le module jetable, il est dans le bundle sans que `src/` le nomme — `npm run build` sans module : `✓ built`, colonne absente ; module jetable `modules/essai/index.ts` (jamais commité) : `✓ built`, « Essai du module » dans `dist/assets/index-*.js`, `grep -rn essai src` vide ; retiré : `✓ built`, « module absent du bundle »
- [pass] Task 2 — écriture au curseur (déverrouillée) ou en fin de page (verrouillée), enregistrée, annulable ; refus sans page — 5174 / 8081, Chrome, campagne « Essai Mythic », page « Session 6 » : verrouillée, deux clics → deux citations à la fin, sans paragraphe vide entre elles (après correction 438b3a2 : la première version en laissait un), page toujours verrouillée, serveur `paragraph,paragraph,blockquote,blockquote,paragraph` ; déverrouillée, curseur en fin de « Premier paragraphe. » → citation insérée juste après ; Ctrl+Z la retire, serveur relu sans elle ; onglet fermé → « Aucune page ouverte : ouvrir une page pour y écrire. »
- [pass] Task 2 — colonne du panneau — fenêtre large : panneau contre la page (grille `17rem minmax(0, 46rem) 18rem`, après correction 438b3a2 : il partait au bord droit) ; iframe de 400px : panneau sous la page (`modulesTop` 972 > `pageBottom` 952), `scrollWidth` 386 ≤ 397, sans défilement horizontal ; sans module : `.modules` absent, deux colonnes
- [pass] Task 3 — mémoire et `DESIGN.md` décrivent le contrat et la colonne — `architecture.md` (`NotebookModule`, `registry.ts`, `writeToOpenPage`, verrou), `codebase-map.md` (`src/modules/`, `vtt-web/modules/`), `DESIGN.md` (colonne de 18rem, empilement sous 900px)

## Phase 5 · le panneau Mythic écrit l'oracle dans la page

- Date: 2026-11-06
- Validated on: f33affa

- [pass] Task 1 — une question écrit question, probabilité, chaos, jet, réponse (et événement avec focus sur un double ≤ chaos) dans la page ouverte, au curseur ou en fin de page verrouillée ; le chaos se garde par campagne et passe un redémarrage — 5174 / 8081, Chrome, campagne « Essai Mythic », page « Session 6 » : + deux fois → chaos 7 ; page déverrouillée, curseur dans « Premier paragraphe. », « Le garde est-il corrompu ? », « Probable », Demander → citation « Oracle : Le garde est-il corrompu ? (Probable, chaos 7) / d100 : 76 → Oui » entre les deux paragraphes ; Demander répété : au 29ᵉ jet, d100 11 → « Oui exceptionnel » et « Événement aléatoire : Action d'un PNJ » écrits et rappelés dans le panneau ; aucune citation imbriquée (`blockquote blockquote` : 0 ; défaut de la première version, corrigé en cea9223), 32 blocs en page comme au serveur ; page verrouillée : réponse à la fin, verrou gardé ; − et + éteints à 1 et à 9 ; rechargement : chaos 7, réponses relues ; recherche « corrompu » → « Session 6 » avec l'extrait de la réponse ; seconde campagne : chaos 5 ; réponse 404 simulée sur `…/questions` → « Question non posée : Not Found » dans le panneau, page inchangée ; onglet fermé → « Aucune page ouverte : ouvrir une page pour y écrire la réponse. », 0 `POST …/questions` ; chaos gardé après redémarrage du serveur : phase 3. Écart constaté : 30 réponses demandées en rafale (moins de 500 ms d'écart) se défont en un seul Ctrl+Z (regroupement de l'historique de ProseMirror), noté dans `architecture.md` ; un Carnet sans page n'a pas de panneau
- [pass] Task 2 — builds verts ; sans `modules/mythic/`, le Carnet marche comme avant ; mémoire, `DESIGN.md`, story et plan à jour — `mvn verify` : `BUILD SUCCESS` (27, 68, 18, 9, 223 tests, 0 échec) ; `npm run build` : `✓ built` ; `modules/mythic/` déplacé : `✓ built`, « Mythic absent du bundle », remis ; `grep -rni mythic vtt-server/src vtt-web/src` : aucun ; `architecture.md`, `codebase-map.md`, `project-brief.md`, `DESIGN.md` décrivent le panneau ; story `status: done` ; 8081 et 5174 arrêtés (jamais 8080 ni 5173)

## Final

- Date: 2026-11-06
- Validated on: f33affa

- [pass] `mvn -q verify` (racine) — `BUILD SUCCESS` ; vtt-engine 27, dnd-engine 68, mythic-engine 18, mythic-server 9, vtt-server 223 tests, 0 échec
- [pass] `npm run build` (`vtt-web/`) — `vue-tsc -b` et Vite : `✓ built`
- [pass] Règle d'assemblage — `grep -rni mythic vtt-server/src vtt-web/src` : aucun résultat
