---
status: done
---

# Instruction: le moteur Mythic lit les tables et tire un sens

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── mythic-engine/src/
    ├── main/java/mythic/engine/
    │   ├── MeaningTables.java ✅
    │   ├── MeaningTablesText.java ✅
    │   ├── Meaning.java ✅
    │   ├── RandomEvent.java ✏️
    │   └── FateQuestion.java ✏️
    └── test/java/mythic/engine/
        ├── MeaningTablesTextTest.java ✅
        ├── MeaningTablesTest.java ✅
        └── FateQuestionTest.java ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Texte de test généré : mots inventés (`action1-001`…), 4 sections + 2 éléments | Aucun mot du livre dans le dépôt | system |
| Happy path   | `MeaningTablesText.parse(texte)` | Actions, Descriptions, éléments dans l'ordre du fichier | system |
| Happy path   | Tirage Actions, dés 1 puis 57 | `action1-001` · `action2-057`, jets gardés | system |
| Happy path   | Tirage d'un élément, dés 3 puis 3 | Deux fois le 3ᵉ mot de la même table | system |
| Happy path   | Question avec tables, dés 33, 25, 4, 90 | Événement, focus, sens `action1-004` · `action2-090` | system |
| Edge case    | Question sans tables, dés 33, 25 | Événement sans sens ; seuls deux dés consommés | system |
| Edge case    | 99 entrées dans `# Actions 2` | Refus : « Actions 2 : 99 entrées, 100 attendues (ligne n) » | system |
| Edge case    | Section obligatoire absente ; titre inconnu ; texte avant le premier titre ; élément en double ; nom d'élément vide | Refus avec la raison et la ligne | system |
| Edge case    | BOM, CRLF, lignes vides, espaces autour | Lu comme le texte propre | system |

## Tasks to do

### `1)` Les tables et le sens

> Le moteur tire deux mots dans une table.

1. `MeaningTables` (record : `actions1`, `actions2`, `descriptions1`, `descriptions2`, `elements` en `LinkedHashMap` nom → liste) ; constructeur qui vérifie 100 entrées non vides par colonne ; listes copiées non modifiables.
2. `Meaning` (record : table, premier mot et son d100, second mot et son d100).
3. `MeaningTables.actions(Dice)`, `descriptions(Dice)`, `element(String nom, Dice)` (nom inconnu : `IllegalArgumentException`), `elementNames()`.

### `2)` Lire le fichier

> Le texte à titres devient des tables, ou un refus qui dit la ligne.

1. `MeaningTablesText.parse(String)` selon la décision du plan ; message en français avec la ligne.

### `3)` Le sens d'un événement

> Un événement aléatoire tire ses deux mots dans les Actions.

1. `RandomEvent` gagne `Optional<Meaning> meaning`.
2. `FateQuestion.ask(odds, chaos, Optional<MeaningTables> tables, dice)` : après le focus, `tables.actions(dice)` s'il y en a ; Javadoc de l'ordre des dés.
3. `FateQuestionTest` passe `Optional.empty()` aux appels existants, plus les cas du Test Scope.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Tirages Actions, Descriptions et élément rendent les mots des jets scriptés. |
| 2 | Chaque cas de refus du Test Scope rend sa raison et sa ligne ; BOM, CRLF, lignes vides tolérés. |
| 3 | `mvn -q test -pl mythic-engine -am` vert ; sans tables, l'ordre des dés et les réponses d'avant ne changent pas. |
