---
status: pending
---

# Instruction: le moteur Mythic répond à une question

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── pom.xml ✏️
├── aidd_docs/memory/
│   ├── codebase-map.md ✏️
│   ├── package.md ✏️
│   └── testing.md ✏️
└── mythic-engine/
    ├── pom.xml ✅
    └── src/
        ├── main/java/mythic/engine/
        │   ├── Odds.java ✅
        │   ├── ChaosFactor.java ✅
        │   ├── FateChart.java ✅
        │   ├── Answer.java ✅
        │   ├── EventFocus.java ✅
        │   ├── RandomEvent.java ✅
        │   ├── FateAnswer.java ✅
        │   └── FateQuestion.java ✅
        └── test/java/mythic/engine/
            ├── ChaosFactorTest.java ✅
            ├── FateChartTest.java ✅
            ├── FateQuestionTest.java ✅
            └── EventFocusTest.java ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Table de destin et table de focus 2e transcrites par l'utilisateur ; `Dice.scripted` | Tables en constantes | system |
| Happy path   | « Probable », chaos 5, d100 scripté sous le seuil oui | `Answer.YES`, jet et chaos rendus | system |
| Happy path   | Pour une case lue dans le livre : jets au seuil exceptionnel oui, juste au-dessus, au seuil oui, juste au-dessus, au seuil exceptionnel non | Oui exceptionnel, oui, oui, non, non exceptionnel (bornes selon le livre) | system |
| Happy path   | d100 = 33, chaos 5, puis d100 de focus scripté | Événement aléatoire avec son focus | system |
| Edge case    | d100 = 66, chaos 5 | Pas d'événement (chiffre > chaos) | system |
| Edge case    | d100 = 55, chaos 5 | Événement (chiffre = chaos) | system |
| Edge case    | d100 = 100 (« 00 ») | Événement aléatoire, quel que soit le chaos (double de 0) | system |
| Edge case    | Chaque borne de la table de focus | Le focus de sa tranche | system |
| Edge case    | `ChaosFactor.of(0)`, `of(10)` ; `raise` à 9, `lower` à 1 | `IllegalArgumentException` ; 9 et 1 (bornés) | system |
| Edge case    | 9 probabilités × 9 chaos | Chaque case cohérente (exceptionnel oui ≤ oui < exceptionnel non) | system |

## Tasks to do

### `1)` Les tables du livre

> Le moteur porte la table de destin et la table de focus de Mythic 2e telles que le livre les donne.

1. Reporter les tables transcrites par l'utilisateur (ci-dessous), sans rien inventer. Chaque case `a b c` : d100 ≤ `a` oui exceptionnel, ≤ `b` oui, ≥ `c` non exceptionnel, sinon non ; `X` : pas de résultat exceptionnel de ce côté. Colonnes : chaos 1 à 9.
   ```txt
   Certain            10 50 91 | 13 65 94 | 15 75 96 | 17 85 98 | 18 90 99 | 19 95 100 | 20 99 X  | 20 99 X  | 20 99 X
   Nearly Certain      7 35 88 | 10 50 91 | 13 65 94 | 15 75 96 | 17 85 98 | 18 90 99  | 19 95 100| 20 99 X  | 20 99 X
   Very Likely         5 25 86 |  7 35 88 | 10 50 91 | 13 65 94 | 15 75 96 | 17 85 98  | 18 90 99 | 19 95 100| 20 99 X
   Likely              3 15 84 |  5 25 86 |  7 35 88 | 10 50 91 | 13 65 94 | 15 75 96  | 17 85 98 | 18 90 99 | 19 95 100
   50/50               2 10 83 |  3 15 84 |  5 25 86 |  7 35 88 | 10 50 91 | 13 65 94  | 15 75 96 | 17 85 98 | 18 90 99
   Unlikely            1  5 82 |  2 10 83 |  3 15 84 |  5 25 86 |  7 35 88 | 10 50 91  | 13 65 94 | 15 75 96 | 17 85 98
   Very Unlikely       X  1 81 |  1  5 82 |  2 10 83 |  3 15 84 |  5 25 86 |  7 35 88  | 10 50 91 | 13 65 94 | 15 75 96
   Nearly Impossible   X  1 81 |  X  1 81 |  1  5 82 |  2 10 83 |  3 15 84 |  5 25 86  |  7 35 88 | 10 50 91 | 13 65 94
   Impossible          X  1 81 |  X  1 81 |  X  1 81 |  1  5 82 |  2 10 83 |  3 15 84  |  5 25 86 |  7 35 88 | 10 50 91

   Focus (d100) : 1-5 Remote Event · 6-10 Ambiguous Event · 11-20 New NPC · 21-40 NPC Action · 41-45 NPC Negative ·
   46-50 NPC Positive · 51-55 Move Toward A Thread · 56-65 Move Away From A Thread · 66-70 Close A Thread ·
   71-80 PC Negative · 81-85 PC Positive · 86-100 Current Context
   ```
   Le test de cohérence porte sur les seuils présents : exceptionnel oui < oui < exceptionnel non.
2. `mythic-engine/pom.xml` (`mythic:mythic-engine`, dépend de `vtt-engine` pour `Dice`, JUnit 5) ; `<module>` et `dependencyManagement` dans le `pom.xml` racine.
3. `Odds` (Impossible, Quasi impossible, Très improbable, Improbable, 50/50, Probable, Très probable, Quasi certain, Certain ; libellés français), `ChaosFactor` (1..9, `INITIAL` = 5, `raise`, `lower` bornés), `FateChart` (seuils par probabilité et chaos), `Answer` (`EXCEPTIONAL_YES`, `YES`, `NO`, `EXCEPTIONAL_NO`, libellés), `EventFocus` (focus, tranches d100, libellés).

### `2)` Poser la question

> Un appel rend la réponse, le jet et l'éventuel événement aléatoire.

1. `FateQuestion.ask(Odds, ChaosFactor, Dice)` → `FateAnswer(roll, answer, chaos, Optional<RandomEvent>)` ; événement si double dont le chiffre ≤ chaos (100 compte comme le double 00, toujours déclenché), focus tiré par un second d100.
2. Tests `ChaosFactorTest`, `FateChartTest`, `FateQuestionTest`, `EventFocusTest` (dés scriptés, noms en français).

### `3)` Mémoire

> Le moteur Mythic est décrit.

1. `codebase-map.md`, `package.md` (`mythic-engine`, sans dépendance runtime hors `vtt-engine`), `testing.md` (tests dans `mythic-engine/src/test/java/`).

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Les 81 cases et les tranches de focus sont celles transcrites par l'utilisateur ; le test de cohérence passe sur chaque case. |
| 2 | `mvn -q test -pl mythic-engine -am` vert sur les scénarios ci-dessus ; aucun test de destin à 2d10. |
| 3 | La mémoire nomme `mythic-engine` et ses tests. |
