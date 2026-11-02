---
status: done
---

# Instruction: Moteur : la résolution `EndCombat` et l'événement `CombatEnded`

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
dnd-engine/src/
├── main/java/dnd/engine/
│   ├── combat/EndCombat.java            ✅
│   └── events/GameEvent.java            ✏️
└── test/java/dnd/engine/CombatTest.java ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `mvn -q test` avant toute modification ; `CombatTest.table()`, dés scriptés, combat lancé | Tests verts ; une créature active | system |
| Happy path   | Après une attaque qui blesse, `submit(EndCombat.of(state))` | `state.combat()` vide ; PV de la cible inchangés ; dernier événement `CombatEnded`, décrit « Le combat se termine » | system |
| Edge case    | `EndCombat.of` hors combat | `IllegalStateException` « Aucun combat en cours », aucun événement | system |
| Edge case    | Terminer pendant « Qui joue ? » (ex æquo) | `submit` lève `IllegalStateException`, combat intact | system |
| Edge case    | Relancer après la fin | `StartCombat.of` accepté ; nouvelle initiative tirée | system |
| Edge case    | Rejouer le journal d'un combat terminé sur une copie de l'état initial | Même `GameState.signature()` | system |

## Tasks to do

### `1)` Événement `CombatEnded`

> La fin du combat est un événement qui vide le combat de l'état.

1. Ajouter `record CombatEnded()` dans la section Combat de `GameEvent` : `apply` appelle `s.endCombat()`, `describe` rend « Le combat se termine ».
2. Vérifier qu'aucun `switch` exhaustif sur `GameEvent` ne casse (recherche dans les quatre modules).

### `2)` Résolution `EndCombat`

> Une `CombatStep` qui émet `CombatEnded`.

1. Créer `combat/EndCombat.java` sur le modèle d'`EndTurn` : `of(GameState)` lève `IllegalStateException("Aucun combat en cours")` hors combat ; `resolve` émet `CombatEnded` et rend `Optional.empty()`.

### `3)` Tests de la situation de table

> Fin, refus, relance, rejeu.

1. Ajouter dans `CombatTest` : `terminerLeCombat_plusDeTours_lesCreaturesGardentLeursPv`, `terminerLeCombat_horsCombat_refuseSansEvenement`, `apresLaFin_leCombatSeRelance`, et la fin dans un test de rejeu.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `CombatEnded` vide `state.combat()` et se décrit « Le combat se termine ». |
| 2 | `EndCombat.of` refuse hors combat sans émettre ; en combat, la résolution n'émet que `CombatEnded`. |
| 3 | `mvn -q test` vert, nouveaux tests compris ; le rejeu d'un combat terminé redonne la même signature. |
