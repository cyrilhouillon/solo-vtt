---
status: done
---

# Instruction: portées D&D dans `dnd-engine` (allonge, portée, lancer)

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── dnd-engine/src/
    ├── main/java/dnd/engine/
    │   ├── state/
    │   │   ├── Weapon.java            ✏️ + allonge (m, corps à corps) et portée normale/longue (m, à distance ou lancer)
    │   │   ├── Attack.java            ✅ une façon d'attaquer : arme + mode (CORPS_A_CORPS, DISTANCE), id stable, libellé, longueurs en m
    │   │   └── Creature.java          ✏️ weapon → weapons (liste, copiée aussi par le constructeur de copie) ; attacks() dérivées des armes ; builder .weapon(w) ajoute
    │   ├── attack/
    │   │   ├── AttackContext.java     ✏️ + attaque choisie et distance (m)
    │   │   ├── AttackAction.java      ✏️ of(attaquant, attackId, cible, distance m, état) ; refuse soi-même, hors allonge, au-delà de la portée longue, avec la raison
    │   │   └── CoreAttackRules.java   ✏️ ctx.attack().weapon() à la place de ctx.weapon() ; DECLARE nomme l'attaque (« Dague (lancer) »)
    │   ├── rules/
    │   │   ├── RangeRules.java        ✅ MODIFY_ROLL : désavantage « portée longue » au-delà de la portée normale
    │   │   ├── DuelingStyle.java      ✏️ seulement pour une attaque au corps à corps
    │   │   └── StandardRules.java     ✏️ enregistre RangeRules
    │   ├── catalog/SrdCatalog.java    ✏️ allonge 1,5 m partout ; Dague lancer 6/18 m ; Arc court 24/96 m pour le Gobelin et le Squelette ; fiches Squelette et Loup alignées sur le SRD 5.2.1
    │   └── demo/Main.java             ✏️ nouvelle signature de Weapon et d'AttackAction
    └── test/java/dnd/engine/
        ├── AttackRulesTest.java       ✏️ nouvelle signature ; comportements inchangés
        ├── RangeRulesTest.java        ✅ situations de portée
        └── catalog/SrdCatalogTest.java ✏️ attacks() au lieu de weapon() ; CA du Squelette 14, du Loup 12
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `mvn -q test` avant modification | 50 tests verts (filet) | cli |
| Happy path   | Épée longue, cible à 1,5 m, `Dice.scripted` | attaque résolue, un seul d20 | cli |
| Happy path   | Dague lancée à 6 m | un d20, bonus +4, dégâts 1d4+2 | cli |
| Happy path   | Arc court à 30 m (entre 24 et 96) | deux d20, le pire gardé, source « portée longue » | cli |
| Edge case    | Épée longue à 2,12 m (voisin en diagonale) | `IllegalArgumentException` « hors d'allonge (2,1 m pour 1,5 m) », aucun événement | cli |
| Edge case    | Arc court à 97 m | refus « au-delà de la portée longue », aucun événement | cli |
| Edge case    | attaquant = cible ; id d'attaque inconnu | refus avec la raison | cli |
| Edge case    | Duel + Dague lancée | pas de +2 | cli |
| Edge case    | portée longue + cible entravée | avantage et désavantage s'annulent, un d20 | cli |
| Edge case    | rejeu du journal | même `GameState.signature()` | cli |

## Tasks to do

### `1)` Armes et attaques

> Chaque créature expose ses attaques, avec leurs longueurs en mètres.

1. `Weapon` : `reach` (m, nul si l'arme n'est pas de corps à corps) et `Range(normal, long)` (m, nul si l'arme n'a pas de portée). Une arme qui a les deux est une arme de lancer. `oneHandedMelee` reste : il dit si l'arme se tient à une main, ce que le mode ne dit pas (la Morsure du Loup est au corps à corps sans être tenue).
2. `Attack` : mode `MELEE` ou `RANGED`, id stable (nom de l'arme et mode), libellé (« Dague », « Dague (lancer) »), et `refusal(distance)` qui rend la raison d'un refus, ou rien.
3. `Creature.weapons()` et `attacks()` : le corps à corps d'abord, puis la distance.
4. Le catalogue, d'après le PDF officiel du SRD 5.2.1 (voir les sources du plan) :
   - l'allonge vaut 1,5 m partout et la Dague se lance à 6/18 m ;
   - le Gobelin gagne un Arc court (+4, 1d6+2 perforant, 24/96 m) ;
   - le Squelette passe à CA 14, avec une Épée courte à +5 (1d6+3) et un Arc court à +5 (1d6+3, 24/96 m) ;
   - le Loup passe à CA 12, avec une Morsure à 1d6+2.

### `2)` Attaque avec distance

> Le moteur résout l'attaque choisie à la distance donnée, et refuse ce que les règles interdisent.

1. `AttackAction.of(attackerId, attackId, targetId, distanceMetres, state)` : refuse les cas interdits avant toute émission.
2. `AttackContext` porte l'`Attack` et la distance. Les règles existantes lisent `ctx.attack().weapon()`.
3. `DuelingStyle` ne s'applique qu'au mode `MELEE` d'une arme `oneHandedMelee`.

### `3)` Désavantage de portée longue

> Au-delà de la portée normale, le jet est fait avec désavantage.

1. `RangeRules` : un listener `MODIFY_ROLL`, mode `RANGED` et distance supérieure à la portée normale, qui ajoute le désavantage « portée longue ». Il est enregistré dans `StandardRules`.
2. `RangeRulesTest` couvre les situations du Test Scope.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Le Gobelin et le Squelette ont deux attaques, les valeurs du Squelette et du Loup sont celles du SRD 5.2.1, et la Magicienne a « Dague » et « Dague (lancer) ». `SrdCatalogTest` passe. |
| 2 | Une épée à 2,12 m ou un arc à 97 m lèvent une exception avec la raison, sans aucun événement émis. Les 14 tests d'`AttackRulesTest` passent avec les mêmes événements. |
| 3 | Un arc à 30 m lance deux d20 et garde le pire, à 24 m un seul. `mvn -q test` passe. La démo console se lance. |
