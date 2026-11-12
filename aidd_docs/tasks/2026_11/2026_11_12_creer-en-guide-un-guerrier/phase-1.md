---
status: done
---

# Instruction: construction du Guerrier dans `dnd-engine`

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
dnd-engine/src/main/java/dnd/engine/sheet/
├── SrdWeapon.java          ✅ les 38 armes du SRD : nom, catégorie, dégâts (`state.DamageType` réutilisé), propriétés, portée, botte
├── SrdArmor.java           ✅ Cotte de mailles, Armure de cuir clouté, Bouclier (+2)
├── FightingStyle.java      ✅ Archerie, Défense, Armes à deux mains, Combat à deux armes
├── AbilityMethod.java      ✅ valeurs standard, achat de points, dés ; validation des valeurs
├── AbilityRoll.java        ✅ quatre d6, total des trois plus hauts ; AbilityRoll.roll(Dice)
├── CharacterClass.java     ✅ FIGHTER : dé de vie, sauvegardes, compétences au choix, répartition suggérée, Bottes
├── StartingEquipment.java  ✅ FIGHTER_A, FIGHTER_B : objets, armes, armure, po
├── Build.java              ✅ les choix guidés ; Build.fighter() ; validate(rolls)
└── GuidedValues.java       ✅ ce que la construction écrit sur la fiche, champ par champ ; rebuild(sheet, before)
dnd-engine/src/test/java/dnd/engine/sheet/
├── BuildTest.java          ✅
└── GuidedValuesTest.java   ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `Build.fighter()` (standard For 15 Dex 14 Con 13 Int 8 Sag 10 Cha 12, Défense, équipement A), Athlétisme et Perception | construction valide | system |
| Happy path   | `GuidedValues.of(build).rebuild(CharacterSheet.blank(), GuidedValues.NONE)` | Guerrier niveau 1, maîtrise +2, sauvegardes For et Con, Athlétisme et Perception maîtrisées, CA 17, PV 11 / 11, « 1d10 », initiative +2, attaques Épée à deux mains +4 2d6+2 tranchants 1,5 m, Fléau d'armes +4 1d8+2, Javeline +4 1d6+2 « 1,5 m ; lancer 9/36 m » ; Capacités : Bottes d'arme (trois armes et leur botte), Second souffle (2), Style de combat : Défense ; Équipement A et 4 po ; Maîtrises | system |
| Happy path   | équipement B, Archerie | CA 14, Cimeterre +4 1d6+2 (Finesse), Épée courte +4, Arc long +6 1d8+2 45/180 m | system |
| Happy path   | CA retouchée à 20 et PV max à 30, puis équipement A → B (Défense) | CA recalculée (15 : 12 + Dex 2 + Défense 1), PV max gardé à 30 | system |
| Happy path   | Perception retouchée en expertise, puis Constitution et Intelligence échangées (Con 8) | Perception garde l'expertise ; PV max et PV passent à 9 | system |
| Edge case    | standard qui n'est pas une permutation de 15, 14, 13, 12, 10, 8 | `IllegalArgumentException` avec la raison | system |
| Edge case    | achat de points à 28 points, ou une valeur hors 8–15 | refusé avec le coût | system |
| Edge case    | dés sans jets, ou valeurs qui ne sont pas les totaux des jets | refusé | system |
| Edge case    | trois compétences, ou une hors de la liste du Guerrier ; deux Bottes identiques ; quatre Bottes | refusé | system |
| Edge case    | dés scriptés 6, 6, 6, 1 six fois | six `AbilityRoll` de total 18, le 1 écarté | system |

## Tasks to do

### `1)` Données SRD

> Les armes, armures et le Guerrier tels que le SRD 5.2.1 les donne, en français.

1. `SrdWeapon` : les 38 armes des fiches `Équipement/Armes/*.md`, avec catégorie (courante / de guerre, corps à corps / distance), dés et type de dégâts, Finesse, Légère, Lourde, Deux mains, Lancer et Munitions (portée en mètres), nom de la botte.
2. `SrdArmor` : Cotte de mailles (16, lourde), Armure de cuir clouté (12 + Dex, légère), Bouclier (+2).
3. `FightingStyle` : les quatre dons de Style de combat, nom et texte court.
4. `CharacterClass.FIGHTER` : d10, For et Con, les 9 compétences au choix (2), répartition standard suggérée, 3 Bottes, maîtrises d'arme et d'armure en texte ; `StartingEquipment.FIGHTER_A` / `FIGHTER_B` : objets, armes, armure, po.

### `2)` Construction et validation

> Les choix guidés, refusés avec la raison s'ils sont impossibles.

1. `AbilityMethod` : valide six valeurs (permutation du standard ; achat 8–15, 27 points au plus ; dés : permutation des totaux des jets).
2. `AbilityRoll.roll(Dice)` : quatre d6, total des trois plus hauts.
3. `Build(characterClass, method, scores, skills, fightingStyle, equipment, masteries)` : `validate(List<AbilityRoll>)` ; au plus 2 compétences de la classe ; 3 Bottes distinctes, armes courantes ou de guerre ; `Build.fighter()` part du standard suggéré, Défense, A et ses trois armes.

### `3)` Valeurs guidées et recalcul par différence

> Ce que la construction écrit, et seulement ce qui change.

1. `GuidedValues.of(build)` : classe, niveau 1, maîtrise +2, chaque caractéristique, chaque sauvegarde, chaque compétence maîtrisée, CA (armure + Dex selon catégorie, + bouclier, + 1 Défense en armure), PV max et PV (10 + mod. Con), dés de vie, initiative (mod. Dex), attaques des armes de l'équipement (maîtrise + For, Dex à distance, la meilleure en Finesse ; + 2 Archerie à distance), Capacités, Équipement, Maîtrises.
2. `rebuild(sheet, before)` : chaque champ dont la valeur dérivée diffère de `before` est réécrit ; les autres gardent la fiche ; `GuidedValues.NONE` réécrit tout.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Chaque arme et armure du Guerrier a les valeurs de sa fiche SRD (dégâts, portée, CA, botte) |
| 2 | Une construction impossible est refusée avec sa raison ; `Build.fighter()` est valide |
| 3 | La fiche d'un Guerrier A Défense donne CA 17, PV 11 et les trois attaques attendues ; passer à B recalcule CA et attaques et garde les retouches qui n'en dépendent pas |
