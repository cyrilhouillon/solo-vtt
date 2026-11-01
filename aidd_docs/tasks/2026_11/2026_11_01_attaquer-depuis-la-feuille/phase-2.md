---
status: done
---

# Instruction: serveur — moteur en session, attaque, journal

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── vtt-server/src/
    ├── main/java/solovtt/server/
    │   ├── SceneSession.java     ✏️ un Engine par carte (règles sans Bouclier, dés injectés) ; journal des descriptions ; attack(...)
    │   ├── DiceProducer.java     ✅ produit les Dice du jeu (graine aléatoire)
    │   ├── AttackResource.java   ✅ POST /api/attacks {attackerId, attackId, targetId} → entrées ajoutées au journal + PV de la cible
    │   ├── JournalResource.java  ✅ GET /api/journal
    │   ├── Dtos.java             ✏️ feuille : attaques (libellé, bonus, dégâts, allonge ou portée dans l'unité de la carte) ; AttackRequest, JournalEntryDto, AttackResultDto
    │   └── CreatureResource.java ✏️ si la forme de la feuille l'exige
    └── test/java/solovtt/server/
        ├── ScriptedDice.java     ✅ @Alternative de test : dés scriptés depuis le test
        ├── AttackApiTest.java    ✅ attaque, refus de portée, journal, adversaire
        └── SceneApiTest.java     ✏️ feuille : attacks ; CA du Squelette 14
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | carte 30 × 20 m, Guerrière en (1,5 ; 1,5), Squelette en (3 ; 1,5), Gobelin en (12 ; 1,5), dés scriptés | scène prête | api |
| Happy path   | Guerrière, Épée longue → Squelette (d20 15, d8 6) | 200 ; le journal contient « attaque », « d20 : 15 », « TOUCHÉ », « subit 11 dégâts » (6 + 3 + 2 de Duel), PV 2/13 | api |
| Happy path   | Gobelin, Arc court → Guerrière à 10,5 m | 200, un seul d20 | api |
| Happy path   | `GET /api/creatures/squelette-1` après le coup | `hitPoints` = 2 | api |
| Happy path   | `GET /api/creatures/magicienne-1` sur une carte en pieds | « Dague » allonge 5 ft ; « Dague (lancer) » 20/60 ft | api |
| Edge case    | Guerrière → Gobelin à 10,5 m | 400, message « hors d'allonge… », journal inchangé | api |
| Edge case    | attaque touchée contre la Magicienne | résolue jusqu'aux dégâts, sans décision Bouclier | api |
| Edge case    | attaquant ou cible inconnus ; attackId inconnu | 404 ; 400 | api |
| Edge case    | attaque sans carte | 409 | api |
| Edge case    | nouvelle carte | journal vide | api |
| Teardown     | `@BeforeEach` recrée la carte | session de base | api |

## Tasks to do

### `1)` Moteur en session

> La session résout des actions et garde leur trace lisible.

1. `SceneSession.create` construit `GameState` puis `Engine`, avec `CoreAttackRules`, `ConditionRules`, `RangeRules`, `Bless` et `DuelingStyle`. `ShieldReaction` n'y est pas, avec un commentaire qui renvoie à la story des décisions.
2. Le moteur est abonné : chaque événement est décrit (`describe(state)`) dès qu'il est émis, puis ajouté au journal avec le numéro de son action.
3. `Dice` est injecté : `DiceProducer` dans le code, et `ScriptedDice` (`@Alternative @Priority`) dans les tests.
4. Retirer une créature n'efface pas le journal.

### `2)` Attaque

> Le serveur mesure et convertit, et `dnd-engine` tranche.

1. `SceneSession.attack(attackerId, attackId, targetId)` : la distance vient de `scene.distance`, convertie en mètres (`DistanceUnit.convert`), puis passe par `AttackAction.of` et `engine.submit`.
2. `AttackResource` : 404 pour un id inconnu, 409 sans carte (`ClientErrorException(CONFLICT)` levée dans la ressource, comme `SceneResource.requireScene`, car `ErrorMapper` ne mappe pas `IllegalStateException`), 400 avec la raison pour un refus des règles. La réponse rend les entrées ajoutées et les PV de la cible.
3. `JournalResource` rend tout le journal.

### `3)` Feuille avec attaques

> La feuille donne chaque attaque et ses longueurs dans l'unité de la carte.

1. `CreatureSheetDto.weapon` devient `attacks` : id, libellé, mode, bonus, dégâts, type, `reach` ou `range {normal, long}`, en `Measure`.
2. `SceneApiTest.laFeuilleDecritLaCreature` est adapté à la nouvelle forme.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Une nouvelle carte a un journal vide. Une attaque contre la Magicienne se termine sans décision en attente. |
| 2 | Les scénarios d'`AttackApiTest` passent. Un refus de portée rend 400 avec la raison et laisse le journal inchangé. |
| 3 | La feuille de la Magicienne, sur une carte en pieds, donne 5 ft et 20/60 ft. `mvn -q test` passe. |
