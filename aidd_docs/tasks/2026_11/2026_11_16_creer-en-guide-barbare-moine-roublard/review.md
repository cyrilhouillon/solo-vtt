# Review: créer en guidé un Barbare, un Moine ou un Roublard

- Rounds: 1
- Started: 2026-11-16

## Round 1 · r-7c3e

- Date: 2026-11-16
- By: Cyril Houillon
- Diff: `main...08110a0`
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 8/8 met, 0 unmet, 0 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1·1 Données des classes, équipements, maîtrises, `Tool`, outils d'historique — `CharacterClass.java:248-274`, `StartingEquipment.java:636-655`, `Tool.java`, `Background.java:10-16` ; `run.md` Final (`mvn verify` sur b482770, seul du Markdown depuis)
- [x] P1·2 Refus avec raison, constructions suggérées valides — `Build.java:113-168`, `BuildTest` ; `run.md` Final
- [x] P1·3 Barbare PV 14 et CA, Moine CA 14 / Dague 1d6 + 2 / mains nues, Guerrier mains nues, Roublard CA 13 / Expertise / outils de voleur une fois, options tout en po, Guerrier → Moine — `GuidedValues.java:68-110, 198-256`, `GuidedValuesTest` ; le plan écrit « CA 15 (10 + 1 + 2) » : la formule donne 13, valeur livrée et juste (SRD)
- [x] P2·1 Test Scope serveur, construction d'avant relue — `DndDtos.java:193-221, 336-365`, `SheetStore.java:122`, `SheetResourceTest` ; `run.md` Final
- [x] P3·1 `npm run build` — `run.md` Phase 3 (f1dcf44, seul du Markdown depuis)
- [x] P3·2 Parcours 5174 — `run.md` Phase 3 (f1dcf44) : Barbare CA 13 (même coquille du plan), Moine, Roublard, Expertise retirée dans le même envoi, rechargement, Guerrier
- [x] P4·1 Capacités listées, chaque « non » vers une story — `lister-les-capacites-de-niveau-1-que-le-moteur-ne-joue-pas.md`, stories créées
- [x] P4·2 Mémoire à jour, story `done` — `architecture.md`, `codebase-map.md`, `project-brief.md`, `creer-en-guide-un-barbare-un-moine-ou-un-roublard.md`

### Findings

- `dnd-engine/src/main/java/dnd/engine/sheet/GuidedValues.java:221` : Arts martiaux (`attack`, et `unarmedStrike` l. 245) s'applique sans regarder armure ni bouclier, alors que la décision du plan veut que CA et attaques dérivent de ce qui est porté pour que l'inventaire ne change que la source ; inatteignable aujourd'hui (aucun équipement de Moine n'a d'armure) → passer armure et bouclier portés à `attack` / `unarmedStrike` et couper Arts martiaux s'ils sont présents, comme `armorClass` — 🟢 minor, fit
- `dnd-engine/src/main/java/dnd/engine/sheet/GuidedValues.java:97` : un Moine qui choisit l'outil de son historique (matériel de calligraphe + Acolyte) le voit fusionné en silence et perd une maîtrise, alors qu'un doublon de compétence est refusé (`Build.validate`, « déjà donnée par l'historique ») → refuser l'outil déjà donné par l'historique dans `Build.checkTool` (et l'éteindre côté client), ou l'acter en décision — 🟢 minor, rot
- `dnd-engine/src/main/java/dnd/engine/sheet/Build.java:26` : `expertise == null` devient `Set.of()`, dont `contains(null)` lève `NullPointerException` dans `checkExpertise` (l. 146) : un `new Build(…, null, …)` validé rend 500 au lieu de 400 (même motif déjà présent pour `skills`) → normaliser en `Collections.unmodifiableSet(new HashSet<>())` ou tester `null` sans `contains` — 🟢 minor, error-handling
- `dnd-engine/src/main/java/dnd/engine/sheet/StartingEquipment.java:30` : `TOOL_TO_CHOOSE` dupliqué à l'identique dans `GuidedValues.java:43` → une seule constante (par ex. dans `CharacterClass` ou `Tool`) — 🟢 minor, code-health
- `dnd-server/src/main/java/dnd/server/DndDtos.java:356` : l'option « Équipement A » du Moine est servie par `equipment.text()`, donc avec « outil … (à choisir) » même après le choix de l'outil (écart noté dans `run.md`) → servir un texte neutre (« l'outil choisi ») ou recalculer le libellé côté client avec `build.tool` — 🟢 minor, frontend
- `dnd-server/src/main/java/dnd/server/SheetStore.java:122` : une construction gardée d'une classe sans Style qui en porte un (fichier édité) est lue comme complète, et `GuidedValues` applique alors Défense (+1 CA) ou Archerie au Roublard → exiger aussi `fightingStyle == null` hors Guerrier, pour rendre 409 comme le Style manquant — 🟢 minor, error-handling
- `dnd-engine/src/main/java/dnd/engine/sheet/GuidedValues.java:270` : « Rage (2 par jour…) » ; le SRD 5.2.1 donne 2 rages, une regagnée au repos court, toutes au repos long → « 2 utilisations ; 1 au repos court, toutes au repos long », comme Second souffle — 🟢 minor, standards
