---
status: pending
---

# Instruction: Serveur : les dés tirés en faits structurés dans le journal

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── dnd-engine/src/main/java/dnd/engine/
│   ├── events/GameEvent.java                         ✏️
│   └── attack/CoreAttackRules.java                   ✏️
├── vtt-server/src/main/java/solovtt/server/
│   ├── SceneSession.java                             ✏️
│   └── Dtos.java                                     ✏️
└── vtt-server/src/test/java/solovtt/server/AttackApiTest.java ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `mvn -q test` avant toute modification | vert : filet de l'existant | system |
| Happy path   | Guerrière frappe le Squelette, dés 15 puis 6 | l'entrée « d20 : 15 » porte `dice` `{sides: 20, values: [15], kept: 15}` ; l'entrée des dégâts de l'Épée longue porte `{sides: 10, values: [6], kept: null}` ; les autres entrées ont `dice: null` | api |
| Edge case    | Tir à l'arc en portée longue (désavantage), dés 17 puis 5 | l'entrée du d20 porte `values: [17, 5]`, `kept: 5` | api |
| Edge case    | Attaque ratée | seule l'entrée du d20 porte `dice` | api |
| Edge case    | Coup critique (20 naturel) | l'entrée des dégâts porte deux fois plus de valeurs | api |

## Tasks to do

### `1)` Événement

> Les dés de bonus et de dégâts disent leurs faces.

1. Remplacer `notation` par `int sides` dans `GameEvent.DiceRolled` ; `notation()` devient dérivée (`values.size() + "d" + sides`), `describe()` rend le même texte.
2. Adapter les deux émissions de `CoreAttackRules` (`term.sides()`, `weapon.dieSides()`).

### `2)` Fait `dice` du journal

> Le client lit les dés sans relire le texte.

1. Ajouter `record RolledDice(int sides, List<Integer> values, Integer kept)` à `SceneSession`, et le champ `dice` à `JournalEntry` : `D20Rolled` donne `kept`, `DiceRolled` donne `kept` à `null`, tout autre événement `null`.
2. Ajouter `dice` à `JournalEntryDto` et à sa Javadoc.

### `3)` Tests API

> La story vue de l'API.

1. Étendre `laGuerriereFrappeLeSqueletteAuContact` et ajouter `enPorteeLongue_leD20GardeLePireDesDeux` et `unCoupCritique_doubleLesDesDeDegats` (positions et dés à fixer à la lecture des tests existants).

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `mvn -q test` vert dans `dnd-engine` ; le texte du journal est inchangé. |
| 2 | `POST /api/attacks` rend `dice` sur chaque entrée de dés, `null` ailleurs ; `/q/openapi` montre le champ. |
| 3 | `mvn -q test` vert, nouveaux tests compris. |
