---
status: pending
---

# Instruction: Moteur générique : tour, candidats et politique de tour

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── vtt-engine/src/
    ├── main/java/solovtt/engine/
    │   ├── TurnOrder.java          ✅
    │   └── TurnPolicy.java         ✅
    └── test/java/solovtt/engine/
        └── TurnOrderTest.java      ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `TurnOrder.offer(1, ["a", "b"])` | round 1, candidats `a`, `b`, aucune active | system |
| Happy path   | `choose("a")` | active `a` | system |
| Happy path   | `offer(1, ["c"])` | un seul candidat : `c` active d'office | system |
| Happy path   | politique jouet à la Daggerheart (joueurs, puis MJ, en alternance) | après la fin du tour d'un joueur : candidats « joueurs » ou « MJ » selon la politique ; aucun type de jeu dans le moteur | system |
| Edge case    | `choose("x")` hors des candidats, ou alors qu'une créature est active | `IllegalArgumentException` / `IllegalStateException` | system |
| Edge case    | `offer` sans candidat, ou round < 1 | `IllegalArgumentException` | system |

## Tasks to do

### `1)` Vocabulaire du tour

> Une valeur immuable qui dit à quel round on est, qui peut jouer, et qui joue.

1. `TurnOrder` (record) : round (≥ 1), candidats (ids ordonnés, non vides, sans doublon), active (id facultatif, parmi les candidats).
2. `offer(round, candidates)` : un seul candidat devient actif d'office ; plusieurs restent à choisir.
3. `choose(id)` : rend l'ordre avec cette créature active ; refuse un non-candidat, ou quand une créature est déjà active.
4. `active()` (Optional), `candidates()`, `round()`, `awaitingChoice()`.
5. Javadoc en français : l'ordre ne sait rien d'une initiative ; c'est la politique du système qui fournit les candidats.

### `2)` Politique de tour

> Le système dit qui peut jouer, au début et après chaque fin de tour.

1. `TurnPolicy<S>` : `first(S)` et `afterTurn(S, TurnOrder ended)` rendent le `TurnOrder` suivant (round compris).
2. Javadoc : exemples D&D (groupes d'initiative) et Daggerheart (joueurs, puis MJ).

### `3)` Tests

> Les axiomes du tour, et la preuve qu'un système sans initiative s'y décrit.

1. `TurnOrderTest` : les scénarios du Test Scope, dont une politique jouet sans initiative déclarée dans le test.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `TurnOrder` compile dans `vtt-engine` sans aucun import de `dnd` ; un candidat unique est actif d'office. |
| 2 | La politique jouet décrit un tour « joueurs, puis MJ » sans toucher `vtt-engine`. |
| 3 | `mvn -q test -pl vtt-engine` passe. |
