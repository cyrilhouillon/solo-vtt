# Review: attaquer depuis la feuille en ciblant sur la carte

- Rounds: 1
- Started: 2026-11-01

## Round 1 · r-7c3e

- Date: 2026-11-01
- By: Cyril Houillon
- Diff: `main...1c621d5` plus the working tree (`.impeccable/critique/`, hors périmètre)
- Axes: code, functional, relevancy
- Verdict: changes-requested
- Score: 2/10 met, 0 unmet, 8 unverified at runtime, 0 out of the diff

### Criteria

- Unverified at runtime: P1.1 Gobelin et Squelette à deux attaques, fiches Squelette et Loup du SRD 5.2.1, Magicienne « Dague » et « Dague (lancer) », `SrdCatalogTest` passe — valeurs conformes dans `SrdCatalog.java:37-56`, le passage du test demande un run
- Unverified at runtime: P1.2 épée à 2,12 m ou arc à 97 m refusés avec la raison sans événement, les 14 tests d'`AttackRulesTest` inchangés passent — refus avant `Pipeline` (`AttackAction.java:32-35`), tests présents (`RangeRulesTest`), un run montrerait qu'ils passent
- Unverified at runtime: P1.3 arc à 30 m deux d20 le pire gardé, à 24 m un seul, `mvn -q test` passe, la démo se lance — `RangeRulesTest.arcAuDelaDeLaPorteeNormale_desavantage` / `arcExactementALaPorteeNormale_pasDeDesavantage` ; aucun run enregistré
- [x] P2.1 nouvelle carte à journal vide ; attaque contre la Magicienne sans décision en attente — `SceneSession.java:66-67` (`journal.clear()`), `SceneSession.java:77-84` (règles sans `ShieldReaction`)
- Unverified at runtime: P2.2 scénarios d'`AttackApiTest` verts ; refus de portée en 400 avec la raison, journal inchangé — `ErrorMapper` mappe l'`IllegalArgumentException` levée avant `engine.submit` ; un run montrerait le vert
- Unverified at runtime: P2.3 feuille de la Magicienne en pieds : 5 ft et 20/60 ft, `mvn -q test` passe — `Dtos.AttackDto.of` convertit, `AttackApiTest.surUneCarteEnPieds_…` le couvre
- Unverified at runtime: P3.1 `npm run build` passe ; la feuille de la Magicienne montre deux attaques avec allonge et portée — un build et un écran le montreraient
- Unverified at runtime: P3.2 parcours navigateur sur 5174 (clic, clavier, refus affiché, Échap, PV qui baissent) — aucun parcours enregistré
- Unverified at runtime: P3.3 le journal montre jet, résultat et dégâts et survit au rechargement — chargé dans `onMounted` (`App.vue:143`), l'affichage demande un run
- [x] P4.1 classes, ressources et commandes citées existent ; story en `status: done` — `RangeRules`, `Attack`, `DiceProducer`, `ScriptedDice`, `JournalPanel`, `SceneSession.rules` présents ; story `status: done`

### Findings

- `vtt-web/src/App.vue:298` : `attackTarget` n'a aucune garde pendant la requête ; `targeting` ne passe à `null` qu'après la réponse, donc un double-clic sur la cible (ou Entrée répétée dans « Sur la carte ») envoie deux attaques, deux jets et deux fois des dégâts → poser un drapeau `attacking` (ou mettre `targeting` à `null` avant l'appel et le restaurer en cas de refus) et ignorer les clics tant qu'il est levé — 🟡 major, frontend
- `dnd-engine/src/main/java/dnd/engine/state/Attack.java:28` : l'id `mode:nom` collisionne si une créature porte deux armes du même nom (deux Dagues) ; la seconde devient injoignable par `AttackAction.of` → documenter l'unicité du nom dans `Creature.Builder.weapon` ou la vérifier au `build()` — 🟢 minor, code-health
- `dnd-engine/src/main/java/dnd/engine/attack/AttackAction.java:25` : `state.get(targetId);` n'est appelé que pour son exception, sans le dire → un commentaire « vérifie que la cible existe » ou une variable nommée — 🟢 minor, standards
- `vtt-server/src/main/java/solovtt/server/SceneSession.java:77` : la liste des règles recopie `StandardRules.create()` moins `ShieldReaction` ; toute nouvelle règle doit être ajoutée aux deux endroits (piège documenté dans `architecture.md`, mais toujours une duplication) → exposer dans `StandardRules` une variante sans réactions, appelée par les deux — 🟢 minor, rot
