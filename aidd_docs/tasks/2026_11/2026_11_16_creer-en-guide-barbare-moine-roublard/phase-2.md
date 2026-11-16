---
status: pending
---

# Instruction: construction et options de classe au serveur

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
dnd-server/src/main/java/dnd/server/
├── DndDtos.java       ✏️ BuildDto + expertise (absent → vide), tool ; ClassOptionsDto : Styles de la classe, armes de Botte de la classe, expertiseCount, outils au choix ; outil d'historique par son libellé
└── SheetStore.java    ✏️ storedBuild : Style exigé seulement si la classe en prend un
dnd-server/src/test/java/dnd/server/
└── SheetResourceTest.java ✏️ nouveaux paramètres ; trois classes ; option tout en po ; construction d'avant sans expertise ni outil
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `ModuleData` en mémoire, fiche créée | fiche vide | api |
| Happy path   | `GET …/dnd/classes` | 4 classes ; Moine sans Style ni arme de Botte, 27 outils au choix (artisan et instruments) ; Roublard `expertiseCount` 2, armes Finesse ou Légère ; Barbare armes de corps à corps ; chaque classe a son option tout en po ; `defaultBuild` de chacune | api |
| Happy path   | `PUT …/build` Roublard avec Expertise Discrétion, Escamotage | 200 ; valeurs et `build.expertise` rendus ; relecture identique | api |
| Happy path   | `PUT …/build` Moine sans `fightingStyle`, outil `LUTE` | 200 ; CA 14, Attaque à mains nues, « Outils : Luth » | api |
| Happy path   | `GET …/dnd/backgrounds` | outil de l'Acolyte « Matériel de calligraphe », comme avant | api |
| Edge case    | histoire gardée d'avant : construction Guerrier sans `expertise` ni `tool` | relue, expertise vide, outil absent | api |
| Edge case    | construction gardée d'un Guerrier sans Style (fichier édité) | 409 | api |
| Edge case    | `PUT …/build` Barbare avec Style ; Roublard avec 3 Expertises ; Roublard avec un outil | 400 avec la raison | api |

## Tasks to do

### `1)` DTO et relecture

> Le serveur porte et relit l'Expertise et l'outil, et ne demande un Style qu'au Guerrier.

1. `BuildDto` : + `expertise` (liste triée), `tool` ; `toBuild` lit `null` comme vide.
2. `ClassOptionsDto` : `fightingStyles` vide si la classe n'en prend pas ; `weapons` limitées aux armes de Botte de la classe ; + `expertiseCount` ; + `tools` au choix (id, libellé, catégorie), vide si la classe n'en choisit pas.
3. `BackgroundOptionsDto` : outil par `Tool.label()`.
4. `SheetStore.storedBuild` : `fightingStyle` requis si la classe en prend un.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Chaque ligne du Test Scope rend le code et le corps attendus ; une construction d'avant sans `expertise` ni `tool` se relit |
