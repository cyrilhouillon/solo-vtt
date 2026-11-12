---
status: done
---

# Instruction: historiques et dons d'origine dans `dnd-engine`

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
dnd-engine/src/main/java/dnd/engine/sheet/
├── Background.java           ✅ les 4 historiques : libellé, 3 caractéristiques, don et sa liste de sorts, 2 compétences, outil (ou boîte de jeux au choix), équipement A (texte, armes, po)
├── BackgroundEquipment.java  ✅ A (ensemble de l'historique) ou B (50 po)
├── GamingSet.java            ✅ cartes à jouer, dés, échecs draconiques, jeu des dragons
├── OriginFeat.java           ✅ Doué, Initié à la magie, Sauvagerie martiale, Vigilant : libellé, texte de niveau 1
├── SpellList.java            ✅ Clerc, Druide, Magicien (listes d'Initié à la magie)
├── OriginFeatChoice.java     ✅ don, liste de sorts, caractéristique d'incantation, compétences de Doué ; validate(taken) ; text()
├── BackgroundChoice.java     ✅ historique, augmentations, équipement, boîte de jeux, incantation du don ; validate(scores) ; feat()
├── SpeciesChoice.java        ✏️ + originFeat (OriginFeatChoice, Humain seul) ; validate le contrôle ; Polyvalent écrit le don choisi
├── Species.java              ✏️ trait Polyvalent sans « au choix » figé (le texte vient du choix)
├── Build.java                ✏️ + background ; abilities() = scores + augmentations ; withBackground ; validate : historique, doublons de compétences
├── CharacterSheet.java       ✏️ + feats (texte) ; blank() le met à ""
├── GuidedValues.java         ✏️ + background, feats ; abilities de Build.abilities ; compétences, outil, équipement, attaques et initiative (Vigilant) de l'historique
└── SheetState.java           ✏️ compare Build.abilities ; refuse le retrait de l'historique
dnd-engine/src/test/java/dnd/engine/sheet/
├── BackgroundChoiceTest.java ✅
├── OriginFeatChoiceTest.java ✅
├── BuildTest.java            ✏️
├── GuidedValuesTest.java     ✏️
├── SpeciesChoiceTest.java    ✏️ texte de Polyvalent, don choisi
├── SheetHistoryTest.java     ✏️ nouveaux paramètres ; retrait de l'historique refusé
├── CharacterSheetTest.java   ✏️ nouveaux paramètres
└── CharacterProfileTest.java ✏️ nouveaux paramètres
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `Build.fighter()` avec Acrobaties et Perception, appliqué à une fiche vide | Guerrier For 15, Dex 14, Con 13 ; initiative 2 ; attaques des armes de l'équipement A | system |
| Happy path   | Soldat, +2 For / +1 Con, équipement A, boîte « dés » | For 17, Con 14 ; PV et attaques recalculés ; historique « Soldat » ; Athlétisme et Intimidation maîtrisées ; Maîtrises + « Boîte de jeux (dés) » ; Équipement : ligne de classe puis ligne de l'historique ; attaques + Arc court et Lance ; Dons : Sauvagerie martiale | system |
| Happy path   | Criminel, +1 Dex/Con/Int, équipement A | initiative = Dex + 2 (Vigilant) ; une seule attaque Dague ; Discrétion et Escamotage | system |
| Happy path   | Acolyte, Sagesse en incantation, équipement B | Dons : « Initié à la magie (Clerc) — incantation Sagesse ; 2 sorts mineurs et 1 sort de niveau 1 (à choisir) » ; Équipement + « 50 po » ; aucune attaque ajoutée | system |
| Happy path   | Humain, Polyvalent : Doué (Histoire, Médecine, Religion) | trois compétences maîtrisées ; Dons + Doué ; trait Polyvalent : « Doué » | system |
| Happy path   | Humain, Polyvalent : Initié à la magie, Druide, Intelligence | Dons + « Initié à la magie (Druide) — incantation Intelligence » | system |
| Happy path   | Soldat → Sage | For 17 → 15, Con 14 → 15 ; Athlétisme et Intimidation retirées, Arcanes et Histoire ; Arc court et Lance retirés, Bâton de combat ; Dons réécrits ; une retouche de la CA dont la valeur dérivée ne change pas est gardée | system |
| Edge case    | construction sans historique sur une fiche où « Historique » et « Dons » sont saisis | historique et dons saisis gardés | system |
| Edge case    | augmentation hors des 3 caractéristiques, +2/+2, +3, +1/+1 seulement, aucune augmentation | `IllegalArgumentException` avec la raison | system |
| Edge case    | valeurs tirées aux dés : For 19 et Soldat +2 For | refusé : au-dessus de 20 | system |
| Edge case    | Soldat sans boîte de jeux, boîte de jeux pour un Acolyte, incantation Force pour Initié à la magie, incantation pour Vigilant | refus avec raison, sauf Soldat sans boîte : « Boîte de jeux (à choisir) » | system |
| Edge case    | Guerrier Athlétisme + Soldat ; Elfe Sens aiguisés Perception + Doué Perception ; Doué Religion + Acolyte | refusé : compétence déjà donnée | system |
| Edge case    | Polyvalent pour un Nain ; Doué avec 2 ou 4 compétences ; liste de sorts pour Vigilant | refus avec raison | system |
| Edge case    | historique choisi puis construction suivante sans historique | `IllegalArgumentException` : il se change, ne se retire pas | system |

## Tasks to do

### `1)` Données des historiques et des dons

> Les 4 historiques et les 4 dons d'origine tels que le SRD 5.2.1 français les donne.

1. `GamingSet`, `SpellList` (CLERIC, DRUID, WIZARD), `BackgroundEquipment` (A, B « 50 po »).
2. `OriginFeat` : SKILLED « Doué », MAGIC_INITIATE « Initié à la magie », SAVAGE_ATTACKER « Sauvagerie martiale », ALERT « Vigilant », avec texte de niveau 1.
3. `Background` : ACOLYTE, CRIMINAL, SAGE, SOLDIER ; caractéristiques, don et liste fixée (Clerc, Magicien), compétences, outil (`null` pour le Soldat : boîte de jeux au choix), texte de l'équipement A, ses armes (`SrdWeapon` DAGGER ; QUARTERSTAFF ; SHORTBOW, SPEAR), ses po.

### `2)` Choix et validation

> Un choix impossible est refusé avec la raison.

1. `OriginFeatChoice(feat, spellList, spellcastingAbility, skills)` : liste et incantation pour Initié seulement (Int, Sag, Cha), trois compétences distinctes pour Doué ; chaque sous-choix sauf le don facultatif (« à choisir ») ; `validate(taken)` refuse une compétence déjà donnée ; `text()`.
2. `BackgroundChoice(background, increases, equipment, gamingSet, spellcastingAbility)` : augmentations +2/+1 ou +1/+1/+1 parmi les 3 caractéristiques, requises ; équipement requis ; boîte pour le Soldat seulement ; incantation pour Initié seulement ; `feat()` rend l'`OriginFeatChoice` de l'historique.
3. `SpeciesChoice` : + `originFeat`, Humain seulement ; `traitsText` écrit « Polyvalent : <don> » ou « à choisir ».
4. `Build` : + `background`, `withBackground`, `abilities()` ; `validate` : historique, valeur finale ≤ 20, compétences de classe, d'espèce et de Doué hors de celles de l'historique et l'une de l'autre.

### `3)` Valeurs dérivées de l'historique

> La fiche porte l'historique, et seuls les champs qui changent sont réécrits.

1. `CharacterSheet` : + `feats` (`""` absent).
2. `GuidedValues` : `abilities` de `Build.abilities()` ; + `background` (libellé, `null` sans historique), `feats` (dons de l'historique puis de Polyvalent, `null` sans l'un ni l'autre) ; compétences de l'historique et de Doué ; Maîtrises + outil ; Équipement + ligne de l'historique ; attaques + armes de A absentes de la classe ; initiative + bonus de maîtrise avec Vigilant.
3. `SheetState.rebuild` : compare `abilities()` d'une construction à l'autre ; refuse le retrait de l'historique.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Chaque historique a les caractéristiques, le don, les compétences, l'outil et l'équipement de sa note SRD ; chaque don son texte |
| 2 | Chaque choix impossible du Test Scope est refusé avec sa raison ; un historique avec augmentations et équipement est valide |
| 3 | Soldat donne For 17, Athlétisme, Arc court et Sauvagerie martiale ; Criminel l'initiative + 2 ; changer d'historique réécrit valeurs, compétences, attaques et dons, et garde une retouche dont la valeur dérivée ne change pas ; sans historique, la fiche garde historique et dons saisis |
