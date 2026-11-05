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
| Edge case    | d100 = 100 | Règle du livre pour « 00 » | system |
| Edge case    | Chaque borne de la table de focus | Le focus de sa tranche | system |
| Edge case    | `ChaosFactor.of(0)`, `of(10)` ; `raise` à 9, `lower` à 1 | `IllegalArgumentException` ; 9 et 1 (bornés) | system |
| Edge case    | 9 probabilités × 9 chaos | Chaque case cohérente (exceptionnel oui ≤ oui < exceptionnel non) | system |

## Tasks to do

### `1)` Les tables du livre

> Le moteur porte la table de destin et la table de focus de Mythic 2e telles que le livre les donne.

1. Obtenir de l'utilisateur la table de destin (9 probabilités × chaos 1–9 : seuils exceptionnel oui, oui, exceptionnel non), la table de focus d'événement (tranches d100) et la règle du « 00 » ; ne rien inventer.
2. `mythic-engine/pom.xml` (`mythic:mythic-engine`, dépend de `vtt-engine` pour `Dice`, JUnit 5) ; `<module>` et `dependencyManagement` dans le `pom.xml` racine.
3. `Odds` (Impossible, Quasi impossible, Très improbable, Improbable, 50/50, Probable, Très probable, Quasi certain, Certain ; libellés français), `ChaosFactor` (1..9, `INITIAL` = 5, `raise`, `lower` bornés), `FateChart` (seuils par probabilité et chaos), `Answer` (`EXCEPTIONAL_YES`, `YES`, `NO`, `EXCEPTIONAL_NO`, libellés), `EventFocus` (focus, tranches d100, libellés).

### `2)` Poser la question

> Un appel rend la réponse, le jet et l'éventuel événement aléatoire.

1. `FateQuestion.ask(Odds, ChaosFactor, Dice)` → `FateAnswer(roll, answer, chaos, Optional<RandomEvent>)` ; événement si double dont le chiffre ≤ chaos, focus tiré par un second d100.
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
