---
status: done
---

# Instruction: moteur, attaques structurées et profil de créature

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
dnd-engine/src/main/java/dnd/engine/
├── state/
│   ├── DamageType.java            ✏️ les 13 types du SRD
│   ├── Creature.java              ✏️ PV actuels sous le maximum au builder
│   └── CreatureProfile.java       ✅ bloc de stats sérialisable, create(id, name)
├── catalog/
│   ├── CatalogEntry.java          ✏️ profil facultatif (PJ copié)
│   └── PlayerCharacters.java      ✅ port : PJ d'une campagne, copie à la pose
└── sheet/
    ├── SheetAttack.java           ✏️ données structurées
    ├── CharacterSheet.java        ✏️ noms d'attaque uniques, profile()
    ├── SrdWeapon.java             ✏️ dés en nombres, damage() dérivé
    └── GuidedValues.java          ✏️ écrit des attaques structurées
dnd-engine/src/test/java/dnd/engine/sheet/
├── CharacterSheetTest.java        ✏️
├── GuidedValuesTest.java          ✏️
├── SheetHistoryTest.java          ✏️
└── CharacterProfileTest.java      ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Fiche Guerrier guidée (équipement A) | Fiche valide, attaques structurées | system |
| Happy path   | `GuidedValues.of(build)` | Épée longue : 1d8, +3 dégâts (For 17), tranchants, allonge 1,5 m, sans portée ; Javeline : allonge 1,5 m et portée 9/36 m | system |
| Happy path   | `sheet.profile()` puis `create("pj-1", "Aria")` | Créature : CA, PV max, PV actuels, vitesse, initiative de la fiche ; une `Attack` par mode (Javeline et Javeline (lancer)) | system |
| Edge case    | Attaque sans allonge ni portée, dé d7, portée longue < normale | `IllegalArgumentException` avec la raison | system |
| Edge case    | Deux attaques du même nom | Refus « deux attaques portent le même nom » | system |
| Edge case    | PV maximum à 0 (fiche vierge) puis `profile()` | Refus avec la raison (« PV maximum à 0 ») | system |
| Edge case    | PV actuels 5 sur 12 | Créature à 5 PV, maximum 12 | system |
| Edge case    | Sarbacane guidée | 0 dé, dégâts fixes 1 + modificateur | system |

## Tasks to do

### `1)` Types de dégâts du SRD

> Un PJ peut infliger tout type de dégâts du SRD.

1. Ajouter à `DamageType` acide, foudre, nécrotiques, poison, psychiques, radiants, tonnerre, au libellé du journal (« dégâts … »).

### `2)` Attaque de fiche structurée

> Une attaque de fiche est jouable sans interpréter de texte.

1. `SheetAttack(name, bonus, diceCount, dieSides, damageBonus, DamageType damageType, Double reach, Double normalRange, Double longRange)`.
2. Refuser : nom vide ; `diceCount` hors 0–20 ; `dieSides` hors 4, 6, 8, 10, 12, 20 ; type absent ; ni allonge ni portée ; allonge ≤ 0 ; une seule des deux portées ; normale ≤ 0 ou longue < normale.
3. `CharacterSheet` refuse deux attaques du même nom (l'id d'une `Attack` est son nom).
4. `SrdWeapon` porte `diceCount` et `dieSides` (Sarbacane : 0 dé, 1 de dégâts fixes) ; `damage()` reste le texte dérivé, pour les options de classe.
5. `GuidedValues.attack` écrit les champs : allonge 1,5 m (3 m pour Allonge) au corps à corps, aucune pour une arme à distance ; portée pour Lancer et Munitions ; dégâts fixes de la Sarbacane dans `damageBonus`.

### `3)` Profil de créature

> Une copie de PJ se garde et se rejoue sans relire la fiche.

1. `CreatureProfile(armorClass, hitPointMax, hitPoints, speed, initiative, weapons, features, spellsKnown, spellSlots)` : record sérialisable, `create(id, name)` construit la `Creature`.
2. `Creature.Builder.currentHitPoints(int)` : PV actuels ≤ maximum.
3. `CharacterSheet.profile()` : CA, PV, vitesse, initiative, une `Weapon` par attaque (`oneHandedMelee` faux) ; refuse PV maximum à 0.

### `4)` Port des PJ

> Le cœur obtient les PJ d'une campagne sans nommer le module.

1. `CatalogEntry` gagne un `CreatureProfile` facultatif ; `CatalogEntry.character(id, name, profile)` crée depuis lui.
2. Interface `PlayerCharacters` : `moduleId()`, `list(campaignId)` (id et nom), `take(campaignId, id)` (nom et profil à cet instant, vide si inconnu, `IllegalArgumentException` si la fiche ne joue pas).

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `DamageType.values()` compte 13 types |
| 2 | Une fiche guidée a des attaques structurées conformes au SRD ; chaque refus donne sa raison |
| 3 | `profile().create(...)` rend une créature aux valeurs de la fiche, PV actuels compris ; fiche à 0 PV max refusée |
| 4 | `CatalogEntry.character` crée la créature du profil sous le nom donné ; `mvn -q test -pl dnd-engine` passe |
