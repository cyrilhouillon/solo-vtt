---
status: pending
---

# Instruction: Règles D&D : initiative, groupes, tours, arrivées et départs

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── dnd-engine/src/
    ├── main/java/dnd/engine/
    │   ├── attack/AttackAction.java       ✏️
    │   ├── catalog/SrdCatalog.java        ✏️
    │   ├── combat/InitiativePolicy.java   ✅
    │   ├── combat/StartCombat.java        ✅
    │   ├── combat/EndTurn.java            ✅
    │   ├── combat/JoinCombat.java         ✅
    │   ├── combat/LeaveCombat.java        ✅
    │   ├── events/GameEvent.java          ✏️
    │   └── state/
    │       ├── Creature.java              ✏️
    │       └── GameState.java             ✏️
    └── test/java/dnd/engine/
        ├── CombatTest.java                ✅
        └── catalog/SrdCatalogTest.java    ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `GameState` avec Guerrière (+2), Gobelin guerrier (+2), Squelette (+3), ajoutés dans cet ordre ; `Dice.scripted` | état sans combat | system |
| Happy path   | `StartCombat`, d20 10, 15, 4 | ordre Gobelin guerrier (17), Guerrière (12), Squelette (7) ; round 1, Gobelin guerrier actif ; journal : début du combat, trois initiatives, ordre, « Round 1 », « Tour de Gobelin guerrier » | system |
| Happy path   | `EndTurn` trois fois | Guerrière, Squelette, puis « Round 2 » et Gobelin guerrier | system |
| Edge case    | d20 14, 5, 13 (Guerrière 16, Gobelin guerrier 7, Squelette 16) | Squelette d'abord (modificateur +3), puis Guerrière | system |
| Edge case    | d20 10, 10, 1 (Guerrière et Gobelin guerrier à 12, même modificateur) | décision « Qui joue ? » avec deux options ; répondre « Guerrière » la rend active ; après sa fin de tour, Gobelin guerrier actif d'office, puis Squelette | system |
| Edge case    | une créature arrive en combat après le groupe en cours, ou avant | elle tire son d20 ; elle joue ce round-ci, ou au round suivant | system |
| Edge case    | la créature active quitte le combat | son tour se termine ; les candidats suivants sont proposés | system |
| Edge case    | en combat, une créature inactive attaque | `IllegalArgumentException` « Ce n'est pas le tour de … », aucun événement | system |
| Edge case    | la Magicienne a lancé Bouclier, puis son tour commence | `ReactionRegained` : réaction disponible ; bonus de CA de Bouclier toujours là | system |
| Edge case    | `StartCombat` sans créature ou combat en cours ; `EndTurn` hors combat ou pendant « Qui joue ? » | `IllegalStateException` avec la raison, aucun événement | system |
| Edge case    | rejouer le journal sur une copie de l'état initial | même `signature()`, combat compris | system |

## Tasks to do

### `1)` Modificateur d'initiative

> Chaque créature porte son modificateur d'initiative, tiré du SRD.

1. `Creature` : champ `initiative` (int), copié par le constructeur de copie, `initiative()`, `Builder.initiative(int)` (défaut 0) ; mutation `regainReaction()`.
2. `SrdCatalog` : Guerrière +2, Magicienne +2, Gobelin guerrier +2, Squelette +3, Loup +2 ; commentaire de source à jour.
3. `SrdCatalogTest` : les cinq modificateurs.

### `2)` État du combat

> L'état sait si un combat est en cours, ses groupes d'initiative, qui a joué ce round, et le tour.

1. `GameState` : combat facultatif, avec les groupes d'initiative ordonnés (total, ids), les créatures qui ont joué ce round et le `TurnOrder` ; accesseurs en lecture.
2. Mutations réservées à `GameEvent.apply`.
3. `copy()` copie le combat ; `signature()` l'inclut.

### `3)` Événements

> Tout ce qui arrive au combat est un événement qui se décrit dans le journal.

1. `CombatStarted` : « Le combat commence ».
2. `InitiativeRolled(creatureId, roll, bonus)` : « Initiative de Guerrière : d20 10 + 2 = 12 ».
3. `InitiativeOrderSet(groups)` : fixe les groupes ; « Ordre de jeu : Gobelin guerrier (17), Guerrière (12), Squelette (7) ».
4. `RoundStarted(round)` : remet à zéro qui a joué ; « Round 2 ».
5. `TurnOffered(round, candidates)` : place le `TurnOrder` (actif d'office s'il y a un seul candidat). Il ne s'écrit au journal (« À jouer : Guerrière ou Gobelin guerrier ») que s'il y a plusieurs candidats.
6. `TurnStarted(creatureId)` : créature active, marquée comme ayant joué ; « Tour de Guerrière ».
7. `TurnEnded(creatureId)` : plus de créature active ; « Fin du tour de Guerrière ».
8. `ReactionRegained(creatureId)` : « Guerrière récupère sa réaction ».
9. `JoinedCombat(creatureId, total)` et `LeftCombat(creatureId)` : mettent à jour les groupes ; « X entre dans le combat (initiative 14) », « X quitte le combat ».

### `4)` Politique et résolutions

> L'initiative D&D, et les résolutions qui pilotent le combat par le moteur.

1. `InitiativePolicy implements TurnPolicy<GameState>` : candidats = membres du groupe en cours qui n'ont pas joué ; sinon, groupe suivant ; après le dernier, round + 1 et premier groupe.
2. Proposer un tour (code commun) : `TurnOffered`. Avec un seul candidat, `TurnStarted` (et `ReactionRegained` si la réaction est dépensée). Avec plusieurs, `Waiting` sur `engine.ask(…, "Qui joue ?", noms des candidats, …)` ; le handler émet `TurnStarted` pour le nom choisi, puis `ReactionRegained` si besoin.
3. `StartCombat` : refuse sans créature ou si un combat est en cours. Émet `CombatStarted` et un d20 par créature dans l'ordre d'ajout (`InitiativeRolled`), groupe par total puis modificateur (`InitiativeOrderSet`), émet `RoundStarted(1)`, puis propose le premier tour.
4. `EndTurn` : refuse hors combat ou sans créature active. Émet `TurnEnded`, puis `RoundStarted` si la politique en ouvre un, puis propose le tour suivant.
5. `JoinCombat(creatureId)` : d20 + modificateur, `JoinedCombat` ; avant le groupe en cours, marquée comme ayant joué ce round.
6. `LeaveCombat(creatureId)` : `LeftCombat` ; si elle était active, la politique propose le tour suivant.
7. Les refus se font avant toute émission.

### `5)` Attaque réservée à la créature active

> En combat, seule la créature dont c'est le tour attaque.

1. `AttackAction.of` : en combat, refuse un attaquant qui n'est pas actif (`IllegalArgumentException` « Ce n'est pas le tour de X »), avant toute émission.

### `6)` Tests de table

> Chaque règle a sa situation de table.

1. `CombatTest` : les scénarios du Test Scope, dés scriptés, événements et état final vérifiés, rejeu par `signature()`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Une créature du catalogue rend son modificateur SRD par `initiative()`. |
| 2 | Une copie de l'état en combat a la même `signature()` ; hors combat, il n'y a pas de combat. |
| 3 | Le journal d'un lancement se lit : début, initiatives, ordre, « Round 1 », « Tour de … ». |
| 4 | Des ex æquo complets posent « Qui joue ? » ; chacun joue une fois par round ; après le dernier groupe, « Round 2 ». |
| 5 | Hors combat, les tests d'attaque existants passent inchangés. |
| 6 | `mvn -q test` passe à la racine. |
