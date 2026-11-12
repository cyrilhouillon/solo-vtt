---
status: done
---

# Instruction: fiche de personnage à événements dans `dnd-engine`

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── dnd-engine/src/
    ├── main/java/dnd/engine/sheet/
    │   ├── Ability.java              ✅ les 6 caractéristiques
    │   ├── Skill.java                ✅ les 18 compétences et leur caractéristique
    │   ├── Proficiency.java          ✅ aucune, maîtrise, expertise
    │   ├── SheetAttack.java          ✅ une attaque saisie
    │   ├── SpellSlots.java           ✅ emplacements d'un niveau : maximum, dépensés
    │   ├── CharacterSheet.java       ✅ les valeurs saisies et leurs dérivés
    │   ├── SheetEvent.java           ✅ sealed : SheetCreated, SheetEdited
    │   └── SheetHistory.java         ✅ le pli des événements en fiche
    └── test/java/dnd/engine/sheet/
        └── CharacterSheetTest.java   ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `SheetCreated` seul | fiche vide : niveau 1, caractéristiques à 10, bonus de maîtrise +2, le reste vide | system |
| Happy path   | `SheetEdited` avec Dex 14, maîtrise +2, Discrétion en expertise, Perception maîtrisée, Sag 12 | mod. Dex +2, Discrétion +6, Perception +3, Perception passive 13 | system |
| Happy path   | deux `SheetEdited` successifs | l'état est celui du dernier | system |
| Edge case    | caractéristique 0 ou 31, niveau 0 ou 21, PV ou CA négatifs, emplacements dépensés au-delà du maximum | `IllegalArgumentException` à la création de la fiche, avec la raison | system |
| Edge case    | modificateur d'une valeur impaire sous 10 (9) | −1 | system |

## Tasks to do

### `1)` Écrire la fiche et ses dérivés

> Une fiche porte ce qu'une fiche papier fait écrire ; les formules du SRD en tirent le reste.

1. `CharacterSheet` (record) : classe, niveau (1–20), espèce, historique (textes) ; caractéristiques (1–30) ; bonus de maîtrise ; sauvegardes maîtrisées ; maîtrise par compétence ; CA, PV maximum et actuels, dés de vie, vitesse (mètres), initiative ; attaques (`SheetAttack` : nom, bonus, dégâts, type, portée, en texte libre sauf le bonus) ; capacités, équipement, maîtrises, langues (textes) ; emplacements par niveau de sort (`SpellSlots`).
2. Dérivés : `modifier(Ability)`, `savingThrow(Ability)`, `skill(Skill)`, `passivePerception()`.
3. Validation à la construction, `IllegalArgumentException` avec la raison en français.
4. `CharacterSheet.blank()` : la fiche d'une création.

### `2)` Écrire l'agrégat

> L'état d'une fiche est le pli de ses événements.

1. `SheetEvent` sealed : `SheetCreated`, `SheetEdited(CharacterSheet values)`.
2. `SheetHistory` : liste d'événements → `CharacterSheet` ; refuse une histoire qui ne commence pas par `SheetCreated`.
3. Tests `CharacterSheetTest`, noms en français.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Les dérivés suivent les formules du SRD (modificateur, sauvegarde, compétence, expertise, Perception passive) ; toute valeur hors bornes est refusée avec sa raison. |
| 2 | Plier `SheetCreated` puis des `SheetEdited` donne les valeurs du dernier ; une histoire sans `SheetCreated` est refusée ; `dnd-engine` n'a toujours aucune dépendance runtime. |
