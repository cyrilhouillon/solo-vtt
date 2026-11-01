---
status: done
---

# Instruction: rendre le cœur générique sur place

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
dnd-engine/src/
├── main/java/dnd/engine/
│   ├── core/
│   │   ├── Event.java              ✅
│   │   ├── Decision.java           ✏️
│   │   ├── Engine.java             ✏️
│   │   ├── Listener.java           ✏️
│   │   ├── Pipeline.java           ✏️
│   │   ├── Resolution.java         ✏️
│   │   └── RuleBook.java           ✏️
│   ├── events/GameEvent.java       ✏️
│   ├── attack/
│   │   ├── AttackAction.java       ✏️
│   │   └── CoreAttackRules.java    ✏️
│   ├── rules/
│   │   ├── Bless.java              ✏️
│   │   ├── ConditionRules.java     ✏️
│   │   ├── DuelingStyle.java       ✏️
│   │   ├── ShieldReaction.java     ✏️
│   │   └── StandardRules.java      ✏️
│   └── demo/Main.java              ✏️
└── test/java/dnd/engine/AttackRulesTest.java  ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | avant tout changement, depuis `dnd-engine/` : `mvn -q compile` puis capturer la sortie de `java -cp target/classes dnd.engine.demo.Main` et de `... --seed=42` dans le scratchpad | deux fichiers de référence | cli |
| Happy path   | `mvn -q test` depuis `dnd-engine/` | 14 tests verts, assertions inchangées | cli |
| Edge case    | relancer la démo avec et sans `--seed=42` et comparer aux références | sortie identique, rejeu « état identique ✔ » | cli |
| Edge case    | chercher `import dnd.engine.state` et `import dnd.engine.events` dans `core/` | aucun résultat | cli |

## Tasks to do

### `1)` Introduire l'événement générique

> Le cœur ne connaît plus que `Event<S>`.

1. Créer `core/Event<S>` avec `default void apply(S state)`.
2. `GameEvent` étend `Event<GameState>`, garde `describe`.

### `2)` Paramétrer le cœur par S et E

> Aucun import de `state` ni `events` dans `core/`.

1. `Engine<S, E extends Event<S>>` : état `S`, journal `List<E>`, observateurs `Consumer<E>`, pile de `Resolution<S, E>`.
2. `Resolution<S, E>` : `advance(Engine<S, E>)`.
3. `Listener<S, E, P, C>` : `appliesTo(C, S)`, `handle(C, Engine<S, E>)`.
4. `Pipeline<S, E, P, C>` et `RuleBook<S, E>` typés en conséquence.
5. `DecisionHandler.onChoice(String option)`, sans moteur ; mettre à jour la javadoc.
6. Retirer de la javadoc d'`Engine` les références à `GameEvent` et `AttackAction` (`{@link #emit(Event)}`, exemple neutre).

### `3)` Adapter le code D&D

> Même comportement, nouvelles signatures.

1. Rendre `CoreAttackRules.AttackRule` public, étendant `Listener<GameState, GameEvent, AttackPhase, AttackContext>` ; `CoreAttackRules.all()` et `ConditionRules.all()` rendent `List<AttackRule>`.
2. `Bless`, `ConditionRules`, `DuelingStyle`, `ShieldReaction` implémentent `AttackRule` ; `handle` prend `Engine<GameState, GameEvent>`.
3. `ShieldReaction` : le handler capture le moteur reçu par `handle`.
4. `AttackAction`, `StandardRules`, `demo/Main` : types génériques.
5. `AttackRulesTest` : signatures seulement (`AttackRule` pour `Probe` et les listeners anonymes, `Engine<GameState, GameEvent>` dans les helpers, `RuleBook<GameState, GameEvent>`, `new Engine<>`, la lambda `Resolution` de `e.push` typée par inférence), aucune assertion modifiée, aucun avertissement unchecked.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `GameEvent` se rejoue sur une copie d'état et redonne la même `signature()` |
| 2 | `core/` compile sans aucun import de `dnd.engine.state` ni `dnd.engine.events` |
| 3 | les 14 tests passent sans changement d'assertion et la démo produit la même sortie qu'avant |
