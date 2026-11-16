---
status: pending
---

# Instruction: Magicien, sorts et emplacements dans `dnd-engine`

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
dnd-engine/src/main/java/dnd/engine/sheet/
├── Spell.java              ✅ les sorts mineurs et du 1er niveau du Magicien : id moteur, libellé, niveau, école, Concentration, rituel, listes
├── CharacterClass.java     ✏️ + WIZARD ; incantation : caractéristique, sorts mineurs, grimoire, préparés, emplacements, liste
├── StartingEquipment.java  ✏️ + WIZARD_A (Dague ×2 et Bâton de combat tenus, Robe, Grimoire, Paquetage d'érudit, 5 po), WIZARD_B (55 po)
├── Build.java              ✏️ + cantrips, spellbook, prepared ; suggested : sorts recommandés ; validate : nombres, liste, niveaux, préparés ⊂ grimoire, refus hors lanceur
├── GuidedValues.java       ✏️ caractéristique d'incantation, sorts, emplacements ; capacités du Magicien
├── CharacterSheet.java     ✏️ + spellcastingAbility, cantrips, spellbook, preparedSpells ; spellSaveDc, spellAttackBonus ; profile avec sorts et emplacements restants ; withSpellSlotExpended
├── SheetEvent.java         ✏️ + SpellSlotExpended(level)
└── SheetHistory.java       ✏️ plie SpellSlotExpended
dnd-engine/src/test/java/dnd/engine/sheet/
├── BuildTest.java            ✏️ nouveaux paramètres ; refus du Magicien
├── GuidedValuesTest.java     ✏️ nouveaux paramètres ; valeurs du Magicien ; Magicien → Guerrier
├── CharacterSheetTest.java   ✏️ DD, bonus d'attaque de sort ; refus de niveaux
├── CharacterProfileTest.java ✏️ Bouclier préparé → knowsSpell, emplacements restants
├── SheetHistoryTest.java     ✏️ emplacement dépensé, jamais au-delà du maximum
├── CombatStatsTest.java      ✏️ nouveaux paramètres de la fiche et de la construction
└── BackgroundChoiceTest.java ✏️ nouveaux paramètres de la fiche et de la construction
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `Build.suggested(WIZARD)` | valide : valeurs 8/12/13/15/14/10, équipement A, Lumière, Main du mage, Rayon de givre ; grimoire Armure du mage, Détection de la magie, Feuille morte, Projectile magique, Sommeil, Vague tonnante ; 4 premiers préparés | system |
| Happy path   | Magicien suggéré, Arcanes et Investigation | PV 7 (6 + 1) ; dé 1d6 ; sauvegardes Int, Sag ; Int d'incantation ; DD 12, attaque +4 ; emplacements niveau 1 : 2, 0 dépensé ; CA 11 (10 + 1) ; Dague, Bâton de combat, mains nues ; Capacités : Sorts, Savoir rituel, Restauration magique | system |
| Happy path   | grimoire avec Bouclier, Bouclier préparé ; `profile()` | `knowsSpell("shield")`, 2 emplacements du 1er niveau | system |
| Happy path   | fiche à 1 emplacement dépensé ; `profile()` | 1 emplacement restant | system |
| Happy path   | `SpellSlotExpended(1)` replié deux fois puis une troisième | dépensés 1, 2, puis 2 (jamais au-delà du maximum) | system |
| Happy path   | `SpellSlotExpended(3)` sur une fiche sans emplacement de niveau 3 | fiche inchangée | system |
| Happy path   | Magicien construit → Guerrier | caractéristique d'incantation, sorts et emplacements retirés ; une retouche de Langues gardée | system |
| Happy path   | Guerrier → Magicien, puis changement d'un sort préparé | seuls les préparés réécrits ; emplacements dépensés gardés | system |
| Edge case    | 2 sorts mineurs ; 7 sorts de grimoire ; 5 préparés ; préparé hors grimoire ; Bouclier en sort mineur ; doublon ; sort d'une autre liste | refus avec raison | system |
| Edge case    | sorts pour un Guerrier | refus avec raison | system |
| Edge case    | fiche libre : sort de niveau 1 en sort mineur ; Lumière préparée ; doublon | refus avec raison | system |
| Edge case    | fiche sans caractéristique d'incantation | DD et attaque de sort `null` | system |

## Tasks to do

### `1)` Table des sorts et Magicien

> Le Magicien et sa liste tels que le SRD 5.2.1 français les donne.

1. Recouper DD et attaque de sort dans `Règles/Incantation.md`.
2. `Spell` : 15 sorts mineurs et 30 sorts du 1er niveau de `Magicien.md` (« Liste des sorts de Magicien »), libellé français, niveau, école, Concentration, rituel, listes (`SpellList.WIZARD`) ; id moteur `shield` pour Bouclier (`ShieldReaction.SPELL`).
3. `CharacterClass.WIZARD` : d6, Int et Sag, 7 compétences, 2 au choix, valeurs suggérées, armes courantes, aucune armure, Bottes 0 ; incantation : Int, 3 sorts mineurs, grimoire 6, 4 préparés, emplacements `[2]`, liste Magicien ; les autres classes sans incantation.
4. `StartingEquipment.WIZARD_A`, `WIZARD_B`.

### `2)` Construction

> Un choix de sort impossible est refusé avec la raison.

1. `Build` : + `cantrips`, `spellbook`, `prepared` (copies modifiables enveloppées, comme `skills`) ; `withX` les gardent.
2. `suggested` : sorts recommandés du SRD pour une classe qui lance des sorts, sinon vides.
3. `validate` : classe sans incantation → aucun sort ; sinon nombres exacts ou au plus (« à choisir » tant qu'incomplet, comme l'Expertise), sorts de la liste de la classe, niveau 0 pour les mineurs, niveau 1 sinon, sans doublon, préparés ⊂ grimoire.

### `3)` Fiche, valeurs dérivées et histoire

> La fiche porte ses sorts, calcule DD et attaque, et suit les emplacements dépensés.

1. `CharacterSheet` : + `spellcastingAbility` (`null` sans), `cantrips`, `spellbook`, `preparedSpells` ; refus d'un sort mineur de niveau ≥ 1, d'un sort de grimoire ou préparé de niveau 0, d'un doublon ; `spellSaveDc()` = 8 + mod + maîtrise, `spellAttackBonus()` = mod + maîtrise, `null` sans caractéristique ; `withSpellSlotExpended(level)`.
2. `profile()` : sorts mineurs et préparés par id moteur, emplacements restants (max − dépensés) par niveau.
3. `GuidedValues` : `spellcastingAbility`, sorts, emplacements (dépensés 0) dérivés, `null` hors lanceur ; `rebuild` les réécrit seulement s'ils changent, et un lanceur → non-lanceur les vide ; à emplacements inchangés, les dépensés de la fiche restent ; capacités du Magicien (Sorts : sorts mineurs, préparés, « à choisir » s'il en manque ; Savoir rituel ; Restauration magique).
4. `SheetEvent.SpellSlotExpended(level)` (niveau 1 à 9) ; `SheetHistory` : dépensés + 1, jamais au-delà du maximum, ignoré sans emplacement de ce niveau.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `Spell` a les 45 sorts de la liste du Magicien, Bouclier d'id `shield` ; le Magicien a le dé de vie, les sauvegardes, les compétences, les valeurs suggérées, les équipements de sa note SRD |
| 2 | Chaque refus du Test Scope a sa raison ; la construction suggérée est valide |
| 3 | Magicien suggéré : PV 7, DD 12, attaque +4, 2 emplacements ; profil avec Bouclier préparé : `knowsSpell("shield")` et emplacements restants ; un emplacement dépensé ne dépasse jamais le maximum ; Magicien → Guerrier retire les sorts et garde une retouche indépendante |
