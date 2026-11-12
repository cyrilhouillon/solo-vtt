---
status: done
---

# Instruction: espèces et choix d'espèce dans `dnd-engine`

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
dnd-engine/src/main/java/dnd/engine/sheet/
├── CreatureSize.java       ✅ M (moyenne), P (petite)
├── Species.java            ✅ les 9 espèces : libellé, tailles, vitesse, vision dans le noir, traits en texte, +PV par niveau, compétences au choix, incantation au choix
├── SpeciesOption.java      ✅ les 24 options (10 dragons, 3 lignages elfiques, 2 gnomes, 6 géants, 3 héritages) : espèce, libellé, texte, vitesse ou vision qu'elle change
├── SpeciesChoice.java      ✅ espèce, taille, option, compétence, caractéristique d'incantation ; validate(classSkills)
├── Build.java              ✏️ + species (SpeciesChoice, null : pas d'espèce) ; validate le contrôle
├── CharacterSheet.java     ✏️ + size, speciesTraits (textes) ; blank() les met à ""
└── GuidedValues.java       ✏️ + species, size, speed, speciesTraits (null sans espèce) ; compétence d'espèce ; +1 PV du Nain ; rebuild garde la fiche pour un champ null
dnd-engine/src/test/java/dnd/engine/sheet/
├── SpeciesChoiceTest.java  ✅
├── BuildTest.java          ✏️ nouveau composant de Build
├── GuidedValuesTest.java   ✏️
├── CharacterSheetTest.java ✏️ nouveaux paramètres
├── CharacterProfileTest.java ✏️ nouveaux paramètres
└── SheetHistoryTest.java   ✏️ nouveaux paramètres
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `Build.fighter()` avec Athlétisme et Perception, appliqué à une fiche vide | Guerrier CA 17, PV 11, vitesse 9 | system |
| Happy path   | espèce Nain | espèce « Nain », taille « M (moyenne) », vitesse 9, PV max et PV 12 ; traits : Vision dans le noir 36 m, Connaissance de la pierre, Résistance naine, Ténacité naine | system |
| Happy path   | Elfe, lignage Elfe sylvestre, Sagesse, Sens aiguisés : Intuition | vitesse 10,5 ; Intuition maîtrisée ; traits avec le lignage, druidisme et la caractéristique d'incantation | system |
| Happy path   | Goliath sans ascendance | vitesse 10,5 ; « Ascendance gigante : à choisir » | system |
| Happy path   | Humain sans taille ni compétence | taille « M ou P (à choisir) » ; « Compétent : une compétence au choix (à choisir) » ; « Polyvalent : un don d'origine au choix » | system |
| Happy path   | vitesse retouchée à 12, puis Nain → Elfe (Haut-elfe) | vitesse gardée à 12 (9 → 9 : valeur dérivée inchangée) ; traits et PV recalculés (12 → 11) | system |
| Happy path   | Elfe sylvestre → Nain | vitesse 10,5 → 9 réécrite ; Intuition retirée si la classe ne la donne pas | system |
| Edge case    | construction sans espèce sur une fiche où « Espèce » et « Vitesse » sont saisis | espèce, taille, vitesse et traits saisis gardés | system |
| Edge case    | option d'une autre espèce (Drow pour un Nain), taille P pour un Nain, compétence hors des Sens aiguisés, incantation Force, compétence pour un Halfelin | `IllegalArgumentException` avec la raison | system |
| Edge case    | Sens aiguisés : Perception alors que la classe a Perception | refusé : déjà maîtrisée | system |

## Tasks to do

### `1)` Données des espèces

> Les 9 espèces et leurs options telles que le SRD 5.2.1 français les donne.

1. `CreatureSize` : MEDIUM « M (moyenne) », SMALL « P (petite) ».
2. `Species` : DRAGONBORN, DWARF, ELF, GNOME, GOLIATH, HALFLING, HUMAN, ORC, TIEFLING ; libellé, tailles, vitesse en mètres, vision dans le noir (0 sans), traits de niveau 1 en texte court (nom : résumé), PV de plus par niveau (Nain 1), compétences au choix (Elfe : Intuition, Perception, Survie ; Humain : toutes ; autres : aucune), incantation au choix (Elfe, Gnome, Tieffelin), libellé de son option (« Ascendance draconique »… ; aucun pour Halfelin, Humain, Nain, Orc), `options()`.
3. `SpeciesOption` : les 24 options avec texte de niveau 1 (dragon : type de dégâts de la résistance et du Souffle ; lignage, héritage : sort mineur, vision ou vitesse) ; Drow vision 36 m, Elfe sylvestre vitesse 10,5.

### `2)` Choix d'espèce et validation

> Un choix impossible est refusé avec la raison.

1. `SpeciesChoice(species, size, option, skill, spellcastingAbility)` : espèce requise ; une taille unique se pose d'office ; `validate(classSkills)` refuse une taille, une option, une compétence ou une incantation (hors Int, Sag, Cha) qui n'est pas de l'espèce, et une compétence que la classe donne déjà.
2. `Build` : + `species` ; `validate` appelle `species.validate(skills)` quand il y en a une.

### `3)` Valeurs dérivées de l'espèce

> La fiche porte l'espèce, et seuls les champs qui changent sont réécrits.

1. `CharacterSheet` : + `size`, `speciesTraits` (texte, `""` absent).
2. `GuidedValues` : + `species` (libellé), `size`, `speed`, `speciesTraits`, `null` sans espèce ; compétence d'espèce maîtrisée ; PV max et PV + PV de l'espèce ; traits : vision dans le noir (de l'option si elle la change), traits de l'espèce, option ou « à choisir », caractéristique d'incantation ou « à choisir », compétence ou « à choisir ».
3. `rebuild` : un champ dont la valeur dérivée est `null` garde la fiche ; espèce, taille, vitesse et traits suivent la règle de différence.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Chaque espèce a la taille, la vitesse, la vision et les traits de sa note SRD ; chaque option son texte et son type de dégâts |
| 2 | Chaque choix impossible du Test Scope est refusé avec sa raison ; une espèce seule est valide |
| 3 | Nain donne PV 12 ; Elfe sylvestre vitesse 10,5 et sa compétence ; changer d'espèce réécrit vitesse, PV et traits et garde une retouche dont la valeur dérivée ne change pas ; sans espèce, la fiche garde espèce et vitesse saisies |
