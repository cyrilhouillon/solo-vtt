---
status: done
---

# Instruction: fiche calculée au serveur

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
dnd-server/src/main/java/dnd/server/
├── DndDtos.java        ✏️ ItemDto, InventoryDto ; SheetValuesDto suit la fiche ; DerivedDto + armorClass, attacks ; EquipmentOptionsDto
└── SheetResource.java  ✏️ GET equipment
dnd-server/src/test/java/dnd/server/
├── SheetResourceTest.java    ✏️ valeurs calculées ; inventaire en saisie ; refus ; catalogue ; documents d'avant retirés
└── SheetCharactersTest.java  ✏️ profil calculé ; fiche sans classe
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | campagne en mémoire, fiche créée, construction Guerrier A | `values.inventory` : cotte de mailles portée, trois armes tenues, 4 po ; `derived.armorClass` 17 ; `derived.attacks` des trois armes et mains nues | api |
| Happy path   | `PUT sheets/{pageId}` avec un bouclier tenu en plus | 200 ; `derived.armorClass` 19 ; l'histoire gagne un `edited` sans CA ni attaques | api |
| Happy path   | `PUT` qui décoche le Fléau d'armes et ajoute une autre attaque | `derived.attacks` sans Fléau, avec l'autre attaque en fin | api |
| Happy path   | fiche libre : `PUT` avec `characterClass` ROGUE et une armure de cuir portée | `derived.armorClass` 11 + Dex | api |
| Happy path   | `GET …/dnd/equipment` | 38 armes et 12 armures plus le bouclier : id, libellé, catégorie | api |
| Happy path   | pose de la fiche (`SheetCharacters.take`) | profil avec la CA et les attaques calculées | system |
| Edge case    | fiche sans classe | `derived.armorClass` et `derived.attacks` `null` ; pose refusée (400 avec la raison) | api |
| Edge case    | `PUT` avec deux armures portées, quantité 0, arme inconnue, autre attaque au nom d'une arme tenue | 400 avec la raison ; histoire inchangée | api |
| Edge case    | document gardé avec `armorClass` ou `equipment` (d'avant) | 409 | api |
| Edge case    | page qui n'est pas une fiche | 404 | api |

## Tasks to do

### `1)` Valeurs et dérivés

> Le serveur rend ce que la fiche calcule.

1. `ItemDto`, `InventoryDto` ; `SheetValuesDto` : `characterClass`, `fightingStyle`, `armorBonus`, `otherAttacks`, `inventory`.
2. `DerivedDto.armorClass` et `attacks`, `null` sans classe.

### `2)` Catalogue d'équipement

> Le client choisit un objet du SRD.

1. `GET …/equipment` : armes et armures du SRD, par catégorie, avec `@APIResponse`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Chaque cas du Test Scope rend le statut et la fiche attendus ; un refus n'écrit rien ; l'histoire ne garde ni CA ni attaques calculées |
| 2 | Le catalogue liste toutes les armes et armures du moteur |
