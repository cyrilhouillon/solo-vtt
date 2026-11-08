---
status: in-progress
---

# Instruction: le moteur Mythic tire une ligne dans une liste

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── .gitignore ✏️
└── mythic-engine/src/
    ├── main/java/mythic/engine/
    │   ├── AdventureList.java ✅
    │   ├── ListDraw.java ✅
    │   ├── EventFocus.java ✏️
    │   ├── FateQuestion.java ✏️
    │   └── RandomEvent.java ✏️
    └── test/java/mythic/engine/
        ├── AdventureListTest.java ✅
        ├── EventFocusTest.java ✏️
        └── FateQuestionTest.java ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Listes décrites par leurs lignes (écrite / vide), dés scriptés | Aucun hasard réel | system |
| Happy path   | 3 lignes écrites, d10 = 6 | Pas de dé de section ; ligne 3 | system |
| Happy path   | 7 lignes écrites, d4 = 3, d10 = 1 | Section 2, ligne 6 (exemple du livre, p. 44) | system |
| Happy path   | 21 lignes, d10 = 10 puis d10 = 10 | Section 5, ligne 25 | system |
| Happy path   | Focus 25 (Action d'un PNJ), Personnages à 2 lignes | Tirage dans Personnages, dés après ceux du sens | system |
| Happy path   | Focus 60 (Loin d'un fil) | Tirage dans Fils | system |
| Happy path   | « A », « A », « B » : d10 1-2 et 3-4 → « A », 5-6 → « B » | Une entrée répétée occupe plus de lignes, donc plus de jets | system |
| Edge case    | Ligne tirée vide (ligne 2 vide sur 3) | « Choisir », avec ses dés | system |
| Edge case    | Lignes vides en fin de liste (5 écrites puis 3 vides) | Une seule section active, pas de d4 | system |
| Edge case    | Liste visée vide ou absente | Aucun dé de plus ; focus effectif Contexte actuel, focus tiré gardé | system |
| Edge case    | Focus PJ, Nouveau PNJ, distant, ambigu, contexte | Aucun tirage de liste, aucun dé de plus | system |
| Edge case    | Ligne écrite au-delà de la 25e | Aucun tirage, liste signalée pleine | system |

## Tasks to do

### `1)` Le livre hors du dépôt

> Le PDF commercial ne peut pas être commité.

1. `.gitignore` : `documents/`, avec un commentaire (livre commercial de l'utilisateur).

### `2)` Le focus dit sa liste

> Chaque focus sait s'il tire dans Fils, Personnages ou nulle part.

1. `EventFocus.list()` : `CHARACTERS` pour les trois focus PNJ hors Nouveau PNJ, `THREADS` pour les trois focus de fil, vide sinon (enum `AdventureList.Kind`).

### `3)` Le tirage dans une liste

> La règle des sections du livre, pure, sans contenu.

1. `AdventureList` : les lignes d'une liste (écrite ou vide, dans l'ordre) ; `draw(Dice)` rend un `ListDraw`.
2. `ListDraw` (scellé) : `Line(kind, line, sectionRoll nullable, lineRoll, blank)`, `Empty(kind)`, `Full(kind)`.

### `4)` L'événement tire dans sa liste

> `FateQuestion.ask` reçoit les deux listes et en tire une si le focus le demande.

1. `ask(odds, chaos, tables, lists, dice)` ; `RandomEvent` gagne `Optional<ListDraw> draw` et `effectiveFocus()` (Contexte actuel si `Empty`).

### `5)` Tests

> Chaque ligne du Test Scope, noms en français.

1. `AdventureListTest`, `EventFocusTest`, `FateQuestionTest` adapté.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `git status` ne montre plus `documents/`. |
| 2 | Les six focus de liste et les six autres rendent la bonne liste. |
| 3 | Les sections et les lignes suivent la table du livre, lignes vides finales exclues. |
| 4 | Les anciens tests gardent leurs dés ; un focus sans liste ne lance rien de plus. |
| 5 | `mvn -q test -pl mythic-engine -am` vert. |
