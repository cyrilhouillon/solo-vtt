---
status: done
---

# Instruction: inventaire, CA et attaques calculées dans `dnd-engine`

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
dnd-engine/src/main/java/dnd/engine/sheet/
├── Item.java               ✅ un objet : arme SrdWeapon, armure ou bouclier SrdArmor, ou texte ; quantité ≥ 1 ; équipé (jamais pour un texte)
├── Inventory.java          ✅ objets dans l'ordre et po ≥ 0 ; une armure portée et un bouclier tenu au plus ; armure, bouclier et armes équipés
├── CombatStats.java        ✅ CA et attaques d'une fiche, sans état (logique tirée de GuidedValues)
├── SrdArmor.java           ✏️ les 12 armures et le bouclier (CA, catégorie, Force requise, Discrétion)
├── CharacterClass.java     ✏️ + maîtrise d'arme et formation aux armures
├── StartingEquipment.java  ✏️ objets en Item ; armor(), shield(), weapons() tirés des objets ; inventory(tool) ; text() tiré des objets
├── Background.java         ✏️ objets de l'équipement A en Item ; weapons(), equipmentText(e) tirés des objets ; inventory(equipment, gamingSet)
├── BackgroundChoice.java   ✏️ inventory() à la place de equipmentText() et weapons()
├── Build.java              ✏️ startingInventory() : classe puis historique, po additionnées
├── CharacterSheet.java     ✏️ characterClass, fightingStyle, armorBonus, otherAttacks, inventory à la place de className, armorClass, attacks, equipment ; armorClass() et attacks() calculées ; profile() refuse une fiche sans classe
└── GuidedValues.java       ✏️ sans armorClass ni attacks ; characterClass, fightingStyle, inventory de départ
dnd-engine/src/test/java/dnd/engine/sheet/
├── InventoryTest.java        ✅ refus, équipés, texte d'une option
├── CombatStatsTest.java      ✅ CA et attaques selon ce qui est porté
├── GuidedValuesTest.java     ✏️ inventaire de départ ; CA et attaques par calcul
├── SheetHistoryTest.java     ✏️ nouveau constructeur ; inventaire gardé ou remplacé par rebuild
├── BackgroundChoiceTest.java ✏️ inventaire
├── BuildTest.java            ✏️ startingInventory
├── CharacterSheetTest.java   ✏️ nouveau constructeur ; doublon d'attaque
└── CharacterProfileTest.java ✏️ profil calculé ; sans classe refusé
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `Build.suggested` de chaque classe, sans et avec historique, appliquée à une fiche vide | CA et attaques calculées identiques à celles d'aujourd'hui ; inventaire = objets de la classe puis de l'historique, po additionnées (Guerrier A + Soldat A : 4 + 14 = 18 po) | system |
| Happy path   | Guerrier A | Cotte de mailles portée ; Épée à deux mains, Fléau d'armes, Javeline ×8 tenues ; « Paquetage d'exploration souterraine » ; 4 po ; CA 17 | system |
| Happy path   | Moine A, outil luth | Lance, Dague ×5 tenues ; « luth » ; « Paquetage d'explorateur » ; 11 po ; sans outil : « outil d'artisan ou instrument de musique (à choisir) » | system |
| Happy path   | Guerrier + bouclier tenu | CA 16 + 1 (Défense) + 2 = 19 | system |
| Happy path   | Barbare (Dex +1, Con +2) en chemise de mailles, puis + bouclier | CA 14 (Défense sans armure perdue), puis 16 | system |
| Happy path   | Moine en armure de cuir, ou avec bouclier | Lance et Dague au calcul ordinaire ; mains nues 1 + For, sans dé ; Défense sans armure perdue | system |
| Happy path   | Moine qui tient une Épée longue | attaque sans le bonus de maîtrise | system |
| Happy path   | Roublard + bouclier | aucun bonus de CA (pas de formation) | system |
| Happy path   | Roublard sans armure portée | CA 10 + Dex | system |
| Happy path   | ajustement de CA +3 ; autre attaque « Trait de feu » | CA + 3 ; « Trait de feu » après les attaques calculées, dans le profil aussi | system |
| Happy path   | caractéristique retouchée (Dex 16 → 18) | CA et attaques suivent sans autre geste | system |
| Happy path   | construction suivante qui ne change pas l'équipement de départ | inventaire de la fiche gardé ; construction qui change d'option ou d'outil du Moine : inventaire remplacé | system |
| Edge case    | deux armures portées ; deux boucliers tenus ; texte équipé ; quantité 0 ; po négatives ; texte vide ou de plus de 80 caractères | refus avec raison | system |
| Edge case    | autre attaque « Dague » et Dague tenue | refus avec raison | system |
| Edge case    | même arme sur deux lignes tenues | une seule attaque | system |
| Edge case    | fiche sans classe | CA et attaques absentes ; `profile()` refusé avec raison | system |

## Tasks to do

### `1)` Tables du SRD

> Les armures, la maîtrise d'arme et la formation aux armures telles que le SRD 5.2.1 les donne.

1. `SrdArmor` : les 12 armures et le bouclier, avec Force requise et désavantage en Discrétion.
2. `CharacterClass` : maîtrise d'arme (Guerrier, Barbare : toutes ; Moine : courantes et de guerre Légères ; Roublard : `rogueProficient`) et formation (Guerrier : toutes et bouclier ; Barbare : légères, intermédiaires, bouclier ; Moine : aucune ; Roublard : légères).

### `2)` Inventaire

> La fiche porte ses objets et ses po.

1. `Item` et `Inventory`, refus avec raison.
2. `StartingEquipment` et `Background` en objets ; `Build.startingInventory()` ; texte d'une option tiré des objets (« Javeline ×8 »).

### `3)` Fiche calculée

> CA et attaques sont des fonctions de la fiche.

1. `CharacterSheet` : `characterClass`, `fightingStyle`, `armorBonus`, `otherAttacks`, `inventory` ; plus de `className`, `armorClass`, `attacks`, `equipment` gardés.
2. `CombatStats` : CA de l'armure portée (Défense si armure), sinon Défense sans armure si permise ; bouclier seulement avec formation ; + ajustement. Attaques des armes tenues, une par arme, maîtrise si la classe maîtrise l'arme, Arts martiaux sans armure ni bouclier ; mains nues ; puis les autres attaques. `profile()` les prend.
3. `GuidedValues` écrit classe, Style et inventaire de départ par `rebuild`, plus CA ni attaques.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Chaque armure a la CA, la catégorie, la Force et la Discrétion du tableau ; chaque classe maîtrise et est formée comme sa note SRD |
| 2 | Chaque refus du Test Scope a sa raison ; l'inventaire de départ de chaque option liste ses objets et ses po |
| 3 | CA et attaques de départ inchangées pour les quatre classes ; chaque cas du Test Scope donne la CA et les attaques attendues |
