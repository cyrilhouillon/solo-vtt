---
status: pending
---

# Instruction: moteur, attaques structurÃ©es et profil de crÃ©ature

## Architecture projection

> Tree of the final files. âœ… create Â· âœï¸ modify Â· âŒ delete

```txt
dnd-engine/src/main/java/dnd/engine/
â”œâ”€â”€ state/
â”‚   â”œâ”€â”€ DamageType.java            âœï¸ les 13 types du SRD
â”‚   â”œâ”€â”€ Creature.java              âœï¸ PV actuels sous le maximum au builder
â”‚   â””â”€â”€ CreatureProfile.java       âœ… bloc de stats sÃ©rialisable, create(id, name)
â”œâ”€â”€ catalog/
â”‚   â”œâ”€â”€ CatalogEntry.java          âœï¸ profil facultatif (PJ copiÃ©)
â”‚   â””â”€â”€ PlayerCharacters.java      âœ… port : PJ d'une campagne, copie Ã  la pose
â””â”€â”€ sheet/
    â”œâ”€â”€ SheetAttack.java           âœï¸ donnÃ©es structurÃ©es
    â”œâ”€â”€ CharacterSheet.java        âœï¸ noms d'attaque uniques, profile()
    â”œâ”€â”€ SrdWeapon.java             âœï¸ dÃ©s en nombres, damage() dÃ©rivÃ©
    â””â”€â”€ GuidedValues.java          âœï¸ Ã©crit des attaques structurÃ©es
dnd-engine/src/test/java/dnd/engine/sheet/
â”œâ”€â”€ CharacterSheetTest.java        âœï¸
â”œâ”€â”€ GuidedValuesTest.java          âœï¸
â”œâ”€â”€ SheetHistoryTest.java          âœï¸
â””â”€â”€ CharacterProfileTest.java      âœ…
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Fiche Guerrier guidÃ©e (Ã©quipement A) | Fiche valide, attaques structurÃ©es | system |
| Happy path   | `GuidedValues.of(build)` | Ã‰pÃ©e longue : 1d8, +3 dÃ©gÃ¢ts (For 17), tranchants, allonge 1,5 m, sans portÃ©e ; Javeline : allonge 1,5 m et portÃ©e 9/36 m | system |
| Happy path   | `sheet.profile()` puis `create("pj-1", "Aria")` | CrÃ©ature : CA, PV max, PV actuels, vitesse, initiative de la fiche ; une `Attack` par mode (Javeline et Javeline (lancer)) | system |
| Edge case    | Attaque sans allonge ni portÃ©e, dÃ© d7, portÃ©e longue < normale | `IllegalArgumentException` avec la raison | system |
| Edge case    | Deux attaques du mÃªme nom | Refus Â« deux attaques portent le mÃªme nom Â» | system |
| Edge case    | PV maximum Ã  0 (fiche vierge) puis `profile()` | Refus avec la raison (Â« PV maximum Ã  0 Â») | system |
| Edge case    | PV actuels 5 sur 12 | CrÃ©ature Ã  5 PV, maximum 12 | system |
| Edge case    | Sarbacane guidÃ©e | 0 dÃ©, dÃ©gÃ¢ts fixes 1 + modificateur | system |

## Tasks to do

### `1)` Types de dÃ©gÃ¢ts du SRD

> Un PJ peut infliger tout type de dÃ©gÃ¢ts du SRD.

1. Ajouter Ã  `DamageType` acide, foudre, nÃ©crotiques, poison, psychiques, radiants, tonnerre, au libellÃ© du journal (Â« dÃ©gÃ¢ts â€¦ Â»).

### `2)` Attaque de fiche structurÃ©e

> Une attaque de fiche est jouable sans interprÃ©ter de texte.

1. `SheetAttack(name, bonus, diceCount, dieSides, damageBonus, DamageType damageType, Double reach, Double normalRange, Double longRange)`.
2. Refuser : nom vide ; `diceCount` hors 0â€“20 ; `dieSides` hors 4, 6, 8, 10, 12, 20 ; type absent ; ni allonge ni portÃ©e ; allonge â‰¤ 0 ; une seule des deux portÃ©es ; normale â‰¤ 0 ou longue < normale.
3. `CharacterSheet` refuse deux attaques du mÃªme nom (l'id d'une `Attack` est son nom).
4. `SrdWeapon` porte `diceCount` et `dieSides` (Sarbacane : 0 dÃ©, 1 de dÃ©gÃ¢ts fixes) ; `damage()` reste le texte dÃ©rivÃ©, pour les options de classe.
5. `GuidedValues.attack` Ã©crit les champs : allonge 1,5 m (3 m pour Allonge) au corps Ã  corps, aucune pour une arme Ã  distance ; portÃ©e pour Lancer et Munitions ; dÃ©gÃ¢ts fixes de la Sarbacane dans `damageBonus`.

### `3)` Profil de crÃ©ature

> Une copie de PJ se garde et se rejoue sans relire la fiche.

1. `CreatureProfile(armorClass, hitPointMax, hitPoints, speed, initiative, weapons, features, spellsKnown, spellSlots)` : record sÃ©rialisable, `create(id, name)` construit la `Creature`.
2. `Creature.Builder.currentHitPoints(int)` : PV actuels â‰¤ maximum.
3. `CharacterSheet.profile()` : CA, PV, vitesse, initiative, une `Weapon` par attaque (`oneHandedMelee` faux) ; refuse PV maximum Ã  0.

### `4)` Port des PJ

> Le cÅ“ur obtient les PJ d'une campagne sans nommer le module.

1. `CatalogEntry` gagne un `CreatureProfile` facultatif ; `CatalogEntry.character(id, name, profile)` crÃ©e depuis lui.
2. Interface `PlayerCharacters` : `moduleId()`, `list(campaignId)` (id et nom), `take(campaignId, id)` (nom et profil Ã  cet instant, vide si inconnu, `IllegalArgumentException` si la fiche ne joue pas).

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `DamageType.values()` compte 13 types |
| 2 | Une fiche guidÃ©e a des attaques structurÃ©es conformes au SRD ; chaque refus donne sa raison |
| 3 | `profile().create(...)` rend une crÃ©ature aux valeurs de la fiche, PV actuels compris ; fiche Ã  0 PV max refusÃ©e |
| 4 | `CatalogEntry.character` crÃ©e la crÃ©ature du profil sous le nom donnÃ© ; `mvn -q test -pl dnd-engine` passe |
