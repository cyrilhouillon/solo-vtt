---
status: in-progress
---

# Instruction: le moteur Mythic teste une scène attendue et ajuste le chaos en fin de scène

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── mythic-engine/src/
    ├── main/java/mythic/engine/
    │   ├── ChaosFactor.java ✏️
    │   ├── FateQuestion.java ✏️
    │   ├── RandomEvent.java ✏️
    │   ├── SceneAdjustment.java ✅
    │   ├── SceneCheck.java ✅
    │   └── SceneOutcome.java ✅
    └── test/java/mythic/engine/
        ├── ChaosFactorTest.java ✏️
        ├── FateQuestionTest.java ✏️
        └── SceneCheckTest.java ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Dés scriptés, tables et listes de test (mots inventés) | Aucun hasard réel | system |
| Happy path   | Chaos 5, d10 = 6 | Attendue, aucun dé de plus | system |
| Happy path   | Chaos 5, d10 = 3 puis d10 = 2 | Modifiée, Ajouter un personnage | system |
| Happy path   | Chaos 5, d10 = 4, focus 25, Personnages à 2 lignes | Interrompue ; événement Action d'un PNJ, ligne tirée, dés dans l'ordre focus, sens, liste | system |
| Happy path   | Ajustement d10 = 8, puis 1, 6 | Deux ajustements : Retirer un personnage, Ajouter un objet | system |
| Happy path   | Contrôle gardé à 5 / perdu à 5 | 4 / 6 | system |
| Edge case    | Chaos 9, d10 = 10 | Attendue (10 ne peut être sous le chaos) | system |
| Edge case    | Chaos 1, d10 = 1 | Modifiée | system |
| Edge case    | Deux ajustements : 9, 2, 10, 2 | 7-10 relancés ; doublon gardé : Ajouter un personnage deux fois | system |
| Edge case    | Deux ajustements : 7, 2, 1 | Contraire ignoré : un seul ajustement, Ajouter un personnage ; trois jets rendus | system |
| Edge case    | Interrompue sans tables | Pas de sens, pas de d100 de sens | system |
| Edge case    | Contrôle gardé à 1 / perdu à 9 | Reste 1 / 9 | system |
| Edge case    | Anciens tests de `FateQuestion` | Mêmes dés, mêmes réponses | system |

## Tasks to do

### `1)` Un événement se tire seul

> Question et scène interrompue tirent le même événement.

1. `RandomEvent.roll(tables, lists, dice)` : focus (d100), sens (si tables), tirage dans la liste visée ; `FateQuestion.ask` l'appelle à la place des l. 30-35.

### `2)` La table d'ajustement

> Six ajustements, leurs contraires, la règle des deux ajustements.

1. `SceneAdjustment` (enum, libellés français) : `of(d10)` (vide pour 7-10), `opposite()`.
2. Tirage : un d10 ; 7-10 → relancer jusqu'à deux résultats de 1 à 6 (7-10 relancés) ; le second contraire du premier → un seul ajustement ; rend les ajustements et tous les jets.

### `3)` Le test de scène

> Attendue, modifiée ou interrompue au chaos.

1. `SceneOutcome` (EXPECTED, ALTERED, INTERRUPTED, libellés « Scène attendue », « Scène modifiée », « Scène interrompue »).
2. `SceneCheck.test(chaos, tables, lists, dice)` → `SceneCheck(roll, chaos, outcome, adjustments, adjustmentRolls, Optional<RandomEvent> event)`.

### `4)` La fin de scène

> La règle de contrôle.

1. `ChaosFactor.afterScene(boolean inControl)` : `lower()` si gardé, sinon `raise()`.

### `5)` Tests

> Chaque ligne du Test Scope, noms en français.

1. `SceneCheckTest`, `ChaosFactorTest` ; `FateQuestionTest` inchangé dans ses dés (adapté seulement si une signature bouge).

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `FateQuestionTest` vert sans changer un dé. |
| 2 | Relances 7-10, doublon et contraire suivent la règle du livre (p. 72). |
| 3 | Seuil et parité suivent la table du livre (p. 67) ; l'interruption porte un événement complet. |
| 4 | −1 / +1, bornes 1 et 9 tenues. |
| 5 | `mvn -q test -pl mythic-engine -am` vert. |
