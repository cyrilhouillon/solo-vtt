---
status: pending
---

# Instruction: histoire de la fiche et routes de construction

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
dnd-engine/src/main/java/dnd/engine/sheet/
├── SheetEvent.java        ✏️ + SheetBuilt(build, values), AbilitiesRolled(rolls)
├── SheetState.java        ✅ valeurs, construction (ou aucune), jets (ou aucun)
└── SheetHistory.java      ✏️ replay rend un SheetState ; SheetEdited garde la construction
dnd-engine/src/test/java/dnd/engine/sheet/CharacterSheetTest.java  ✏️ replay rend un SheetState
dnd-engine/src/test/java/dnd/engine/sheet/SheetHistoryTest.java    ✅
dnd-server/src/main/java/dnd/server/
├── SheetResource.java     ✏️ + Dice ; GET classes ; PUT sheets/{id}/build ; POST sheets/{id}/ability-rolls
├── SheetStore.java        ✏️ EventFile : types built et rolled
└── DndDtos.java           ✏️ BuildDto, AbilityRollDto, ClassOptionsDto ; SheetDto + build, rolls
dnd-server/src/test/java/dnd/server/SheetResourceTest.java          ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `SheetResource` sur `Donnees` et `Pages` en mémoire, `Dice.scripted` ; une fiche créée | fiche vide, sans construction | api |
| Happy path   | `GET classes` | Guerrier : compétences au choix (2), styles, équipements A et B en texte, armes et leur botte, répartition suggérée, coûts de l'achat | api |
| Happy path   | `PUT sheets/{id}/build` avec `Build.fighter()` puis `GET` | valeurs du Guerrier et `build` rendus ; identiques après un nouveau `SheetResource` sur les mêmes données | api |
| Happy path   | `PUT` des valeurs (CA 20), puis `PUT build` qui ne change que les compétences | CA reste 20 | api |
| Happy path   | `POST ability-rolls` (24 dés scriptés), puis `PUT build` en méthode dés | `rolls` rendu, valeurs assignées | api |
| Edge case    | second `POST ability-rolls` | 409, aucun dé tiré | api |
| Edge case    | `PUT build` invalide (3 compétences) | 400 avec la raison, histoire inchangée | api |
| Edge case    | page qui n'est pas une fiche | 404 | api |
| Edge case    | `dnd.json` d'avant écrit à la main (`created`, `edited` valides seulement) | se relit, sans construction | api |
| Edge case    | `PUT` des valeurs après une construction | la construction est gardée | api |

## Tasks to do

### `1)` Histoire de la fiche

> La construction et les jets font partie de l'histoire, rejouable.

1. `SheetEvent.SheetBuilt(Build build, CharacterSheet values)` et `AbilitiesRolled(List<AbilityRoll> rolls)`.
2. `SheetState(values, build, rolls)` ; `SheetHistory.replay` le plie : `SheetEdited` remplace les valeurs et garde construction et jets ; un second `AbilitiesRolled` est refusé.

### `2)` Routes

> Le client construit, lance les dés et lit les options du Guerrier.

1. `GET …/dnd/classes` : options de chaque classe (`ClassOptionsDto`).
2. `PUT …/dnd/sheets/{pageId}/build` : `validate`, puis `GuidedValues.of(build).rebuild(values, GuidedValues.of(précédente) ou NONE)`, émet `SheetBuilt`, rend la fiche.
3. `POST …/dnd/sheets/{pageId}/ability-rolls` : six `AbilityRoll.roll(dice)`, émet `AbilitiesRolled` ; 409 si déjà lancés.
4. `SheetDto.of` prend un `SheetState` et porte `build` et `rolls` (`null` s'il n'y en a pas) ; `create` (`SheetResource.java:73`) et `edit` (`:103`) rendent l'état replié, `edit` garde construction et jets ; `@APIResponse` sur chaque route.
5. Le constructeur prend `Dice` ; `SheetResourceTest` (`:127`) passe des `Dice.scripted`.

### `3)` Persistance

> `modules/dnd.json` garde les nouveaux événements, et relit ceux d'avant.

1. `EventFile` : `built` (construction et valeurs) et `rolled` (jets) ; type inconnu toujours 409.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Rejouer une histoire créée, construite, saisie redonne valeurs, construction et jets |
| 2 | Une construction valide remplit la fiche, une invalide rend 400 sans rien écrire ; les dés ne se lancent qu'une fois |
| 3 | Une fiche construite se relit à l'identique après un nouveau `SheetResource` ; un `dnd.json` d'avant se relit |
