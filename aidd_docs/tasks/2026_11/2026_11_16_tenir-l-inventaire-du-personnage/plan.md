---
objective: "La fiche porte un inventaire (armes et armures du SRD, bouclier, objets en texte, quantités, pièces d'or, ce qui est porté ou tenu) que la création guidée remplit et que le joueur retouche ; CA et attaques ne sont plus gardées mais toujours calculées depuis la classe, le Style, les caractéristiques, l'inventaire et des ajustements saisis, Arts martiaux compris."
status: implemented
---

# Plan: tenir l'inventaire du personnage

Source: aidd_docs/backlog/stories/tenir-l-inventaire-du-personnage.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Inventaire, CA et attaques calculées dans `dnd-engine` | [`phase-1.md`](./phase-1.md) | none |
| 2   | Fiche calculée au serveur | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Inventaire, classe et valeurs calculées sur la fiche | [`phase-3.md`](./phase-3.md) | 2 |
| 4   | Règles non jouées, mémoire et backlog | [`phase-4.md`](./phase-4.md) | 1, 2, 3 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf | Lu dans sa traduction française (notes Obsidian `Compendio SRD/Français/Règles/Armures.md`, `Glossaire/Formation aux armures.md`, `Glossaire/Maîtrise.md`, CC-BY 4.0). Les 12 armures et le bouclier : légères matelassée 11 + Dex (Discrétion désavantage), cuir 11 + Dex, cuir clouté 12 + Dex ; intermédiaires (Dex max 2) peaux 12, chemise de mailles 13, écailles 14 (désavantage), cuirasse 14, demi-plate 15 (désavantage) ; lourdes broigne 14 (désavantage), cotte de mailles 16 (For 13, désavantage), clibanion 17 (For 15), harnois 18 (For 15) ; bouclier +2. Une seule armure et un seul bouclier à la fois. Armure sans formation : Désavantage aux tests d20 de For et Dex, pas de sorts ; bouclier sans formation : pas de bonus de CA. Force insuffisante : vitesse −3 m. Une arme non maîtrisée n'ajoute pas le bonus de maîtrise. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| architecture | Aujourd'hui CA, attaques et équipement sont des valeurs gardées de la fiche (`CharacterSheet.java:22-24`) : `GuidedValues.of` les écrit depuis `StartingEquipment` (`GuidedValues.java:80-107`) et `rebuild` ne les réécrit que si leur valeur dérivée change (`GuidedValues.java:154, 160, 164`) ; la classe est un texte (`className`) ; Arts martiaux ignore armure et bouclier (`GuidedValues.java:219-257`) ; `SrdArmor` n'a que 3 armures (`SrdArmor.java:5-8`) ; ni maîtrise d'arme ni formation aux armures en données (`CharacterClass.java:21`). Changement : la fiche garde `characterClass` (`CharacterClass`, `null` tant qu'elle n'est pas choisie), `fightingStyle`, `inventory` (`Item`, `Inventory`), un ajustement de CA saisi et ses « autres attaques » saisies ; `armorClass()` et `attacks()` deviennent des fonctions de la fiche (`CombatStats`), sans état ; `GuidedValues` n'écrit plus CA ni attaques, mais classe, Style et inventaire de départ. | phase 1 |
| architecture | `SheetValuesDto` suit la fiche ; `DerivedDto` gagne `armorClass` et `attacks` (`null` sans classe) ; un geste d'inventaire est une saisie ordinaire (`PUT sheets/{pageId}`) ; `GET …/dnd/equipment` sert armes et armures. `SheetCharacters` pose la fiche par `profile()`, qui calcule (`SheetCharacters.java:81`). | phase 2 |
| regression | Élevé dans `dnd-engine` et `dnd-server`, nul ailleurs : le constructeur de `CharacterSheet` change (`CharacterSheet.blank`, `withHitPoints`, `GuidedValues.rebuild`, `DndDtos.toSheet`, et les tests `BackgroundChoiceTest`, `CharacterProfileTest`, `CharacterSheetTest`, `GuidedValuesTest`, `SheetHistoryTest`, `SheetResourceTest`, `SheetCharactersTest`) ; les attentes sur `armorClass()`, `attacks()`, `equipment()` passent par le calcul. `StartingEquipment.weapons()` reste un accesseur (`Build.java:88`, `SheetHistoryTest:58`, `GuidedValuesTest:232`, `SheetResourceTest:167`), `Background.equipmentText(e)` aussi (`DndDtos.java:297`). Les CA et attaques de départ des quatre classes doivent rester identiques : `GuidedValuesTest` et `SheetResourceTest` sont le filet. `vtt-server` ne lit que `profile()` (`CampaignCatalog.java:63`). Les documents gardés d'avant (`equipment`, `armorClass`, `attacks`, `className`) ne se relisent plus : 409, à recréer, et les tests qui les relisent (`SheetResourceTest:490, 588, 655-690`) sont réécrits ou retirés. | phase 1, 2 |
| runtime | Aucune dépendance hors dépôt. Parcours client non observé : à vérifier sur 8081 / 5174. Les fiches de la campagne du joueur (8080) deviendront illisibles (409). | phase 3 |
| documentation | `architecture.md` (fiche calculée, inventaire, maîtrise d'arme, formation, route d'équipement, fiches d'avant illisibles), `codebase-map.md` (`Item`, `Inventory`, `CombatStats`, `InventoryPanel.vue`), `project-brief.md` (inventaire, classe obligatoire), epic D&D (« toute valeur se retouche » : sauf CA et attaques, calculées avec ajustements), backlog (story `done` ; liste de la tâche ; story des pénalités d'armure). | phase 4 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Les moteurs n'ont aucune dépendance runtime | inventaire, tables et calcul dans `dnd-engine` |
| Un module n'est nommé que par les assemblages | tout reste dans `dnd-server` et `vtt-web/modules/dnd/` |
| Le client mesure, le serveur tranche | CA et attaques calculées par le serveur ; le client les montre depuis la réponse |
| `ErrorMapper` rend 400 pour `IllegalArgumentException` | inventaire impossible, nom d'attaque en double |
| Code et messages en français, identifiants en anglais ; tests aux noms français | toutes les phases |
| Règles tirées du SRD 5.2.1 ; texte français des notes Obsidian (CC-BY 4.0) | tables d'armures, formation, maîtrise |
| Chaque règle rencontrée que le moteur ne joue pas entre dans la liste de la tâche avec sa story | pénalités d'armure |
| Tests navigateur sur 8081 / 5174 uniquement | phase 3 |
| Vérifications avant commit : `mvn -q compile`, `mvn -q test`, `npm run build` | chaque phase |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| CA et attaques ne sont jamais gardées : `CombatStats` les calcule de la fiche (classe, Style, caractéristiques, bonus de maîtrise, inventaire, ajustement de CA, autres attaques) | transparence référentielle : une seule source, pas de valeur périmée, pas de comparaison avant/après |
| Retouche : un ajustement de CA saisi (± ; Armure du mage, objet magique) et une liste « autres attaques » saisie (sorts…) après les attaques calculées | décision du joueur ; ce que le moteur ne modélise pas reste écrivable |
| La classe de la fiche est une `CharacterClass` du moteur, choisie aussi en saisie libre ; sans classe, ni CA ni attaques, et la fiche ne se pose pas (400) ; le Style aussi se choisit en saisie libre | décision du joueur : pas de calcul générique ; la saisie libre est limitée aux quatre classes du moteur jusqu'aux stories des autres |
| Pas de compatibilité : un document gardé d'avant rend 409 | décision du joueur : pas de code de reprise |
| Un geste d'inventaire est une saisie ordinaire, envoyée sans attendre ; la fiche montre la CA et les attaques de la réponse | plus de route ni de recalcul dédiés : la saisie suffit, le calcul est à la lecture |
| Une autre attaque qui porte le nom d'une attaque calculée est refusée (400 avec la raison) | pas de règle cachée sur les doublons |
| Changer un choix qui change l'équipement de départ remplace tout l'inventaire ; un autre choix le garde | même règle que les champs dérivés restants de `rebuild` |
| Équipement de départ : armure portée, bouclier tenu, toutes les armes tenues ; pas de limite de mains | CA et attaques de départ inchangées ; mains non jouées |
| Arme non maîtrisée : sans bonus de maîtrise ; bouclier sans formation : sans bonus de CA ; armure sans formation : CA de l'armure, pénalités listées non jouées | SRD 5.2.1 ; les pénalités ont leur story |
| PV max, initiative, sauvegardes, compétences gardent `rebuild` | périmètre de la story ; leur calcul à la lecture est une refonte à part |
