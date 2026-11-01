---
status: done
---

# Instruction: extraire le cœur dans le module vtt-engine

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── pom.xml                                       ✅
├── vtt-engine/
│   ├── pom.xml                                   ✅
│   └── src/
│       ├── main/java/solovtt/engine/
│       │   ├── Decision.java                     ✅
│       │   ├── Dice.java                         ✅
│       │   ├── Engine.java                       ✅
│       │   ├── Event.java                        ✅
│       │   ├── Listener.java                     ✅
│       │   ├── Pipeline.java                     ✅
│       │   ├── Resolution.java                   ✅
│       │   ├── RuleBook.java                     ✅
│       │   └── StepResult.java                   ✅
│       └── test/java/solovtt/engine/EngineTest.java  ✅
└── dnd-engine/
    ├── pom.xml                                   ✏️
    └── src/
        ├── main/java/dnd/engine/
        │   ├── core/                             ❌
        │   ├── events/GameEvent.java             ✏️
        │   ├── attack/*.java                     ✏️
        │   ├── rules/*.java                      ✏️
        │   └── demo/Main.java                    ✏️
        └── test/java/dnd/engine/AttackRulesTest.java  ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | aucun | état issu de la phase 1 | cli |
| Happy path   | `mvn -q verify` à la racine | les deux modules compilent, tous les tests passent | cli |
| Edge case    | chercher `import dnd.` dans `vtt-engine/` | aucun résultat | cli |
| Edge case    | `mvn -q install` puis `mvn dependency:tree` à la racine | aucune dépendance hors scope `test` dans les deux modules, hormis `dnd-engine → vtt-engine` | cli |
| Edge case    | depuis `dnd-engine/` : `java -cp "target/classes;../vtt-engine/target/classes" dnd.engine.demo.Main`, avec et sans `--seed=42` | sortie identique aux références de la phase 1 | cli |

## Tasks to do

### `1)` Build multi-module

> `mvn verify` à la racine construit tout.

1. Créer le pom agrégateur racine (`solovtt:solo-vtt`, packaging `pom`, modules `vtt-engine` et `dnd-engine`), qui porte Java 21, l'encodage, JUnit et les plugins.
2. Créer `vtt-engine/pom.xml` (`solovtt:vtt-engine`), sans dépendance runtime.
3. `dnd-engine/pom.xml` hérite du parent, garde `dnd:dnd-engine`, dépend de `vtt-engine`.

### `2)` Déplacer le cœur

> Le cœur vit dans `solovtt.engine`.

1. Déplacer les 9 fichiers de `dnd/engine/core/` vers `vtt-engine/src/main/java/solovtt/engine/` avec `git mv`, changer le package.
2. Remplacer les imports `dnd.engine.core.*` par `solovtt.engine.*` dans dnd-engine (main et test).
3. Mettre à jour la commande de lancement dans la javadoc de `demo/Main` (classpath des deux modules).

### `3)` Tester le cœur seul

> Le module générique prouve ses axiomes sans D&D.

1. Créer `EngineTest` avec un état et des événements jouets (un compteur), dés scriptés.
2. Couvrir : ordre des phases, priorité puis ordre d'enregistrement, suspension et reprise sur décision, résolution empilée avant la suite, `submit` refusé si décision en attente, option invalide refusée, rejeu du journal.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `mvn -q verify` à la racine réussit ; seul `vtt-engine` apparaît comme dépendance non-test de dnd-engine |
| 2 | aucun import `dnd.` dans `vtt-engine/`, les 14 tests D&D passent sans changement d'assertion, la démo est identique |
| 3 | `EngineTest` passe et couvre les sept axiomes listés |
