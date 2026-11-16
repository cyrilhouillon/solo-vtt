---
status: done
---

# Instruction: Barbare, Moine, Roublard et outils dans `dnd-engine`

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
dnd-engine/src/main/java/dnd/engine/sheet/
├── Tool.java               ✅ les outils du SRD : libellé, catégorie (artisan, instrument de musique, autre)
├── Background.java         ✏️ tool : Tool (null : boîte de jeux au choix)
├── BackgroundChoice.java   ✏️ toolText depuis Tool
├── CharacterClass.java     ✏️ + BARBARIAN, MONK, ROGUE ; Style ou non, armes de Botte permises, Expertises, outil fixe ou catégories au choix, Défense sans armure (caractéristique, bouclier permis), Arts martiaux
├── StartingEquipment.java  ✏️ + BARBARIAN_A/B, MONK_A/B, ROGUE_A/B, FIGHTER_C ; option tout en po (sans armure ni arme)
├── SrdArmor.java           ✏️ + LEATHER (11 + Dex) ; javadoc générale
├── Build.java              ✏️ + expertise, tool ; suggested(classe) dérivé des données ; validate : Style, armes de Botte, Expertise, outil
└── GuidedValues.java       ✏️ CA sans armure ; Arts martiaux ; mains nues pour toute classe ; Expertise ; capacités par classe ; outils sans doublon ; outil du Moine dans l'équipement A
dnd-engine/src/test/java/dnd/engine/sheet/
├── BuildTest.java            ✏️ nouveaux paramètres ; refus des trois classes
├── GuidedValuesTest.java     ✏️ nouveaux paramètres ; valeurs des trois classes ; mains nues du Guerrier
├── SheetHistoryTest.java     ✏️ nouveaux paramètres ; Guerrier → Moine
├── BackgroundChoiceTest.java ✏️ nouveaux paramètres ; outil par Tool
└── CharacterProfileTest.java ✏️ nouvelle signature de GuidedValues.attack
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `Build.suggested` de chaque classe, valeurs suggérées | constructions valides ; Guerrier : valeurs inchangées sauf la ligne « Attaque à mains nues » (+4, 1 + 2 = 3 contondants) | system |
| Happy path   | Barbare suggéré (For 15, Dex 13, Con 14), Athlétisme et Perception, équipement A | PV 14 ; sauvegardes For, Con ; CA 15 (10 + 1 + 2) ; Hache à deux mains, Hachette, Attaque à mains nues (+4, 3) ; Capacités : Bottes (2), Rage (2, +2), Défense sans armure ; aucun Style | system |
| Happy path   | Moine suggéré (For 12, Dex 15, Sag 14), outil Luth | PV 9 ; CA 14 (10 + 2 + 2) ; Lance 1d6 + 2 ; Dague 1d6 + 2 ; Attaque à mains nues +4, 1d6 + 2 contondants ; Maîtrises « Outils : Luth » ; Équipement A avec le luth ; aucune Botte | system |
| Happy path   | Moine For 15, Dex 13 (achat de points) | mains nues et Lance au toucher +4, 1d6 + 2 : la For l'emporte | system |
| Happy path   | Moine sans outil choisi | « Outils : outil d'artisan ou instrument de musique (à choisir) » | system |
| Happy path   | Roublard suggéré, Discrétion, Escamotage, Perception, Tromperie ; Expertise Discrétion et Escamotage | PV 9 ; CA 13 (cuir 11 + 2) ; Dague, Épée courte (Finesse : Dex), Arc court, mains nues ; Discrétion et Escamotage en expertise ; « Outils : Outils de voleur » ; Capacités : Expertise, Attaque sournoise 1d6, Argot des voleurs, Bottes (2) | system |
| Happy path   | Roublard + Criminel | « Outils : Outils de voleur », une seule fois | system |
| Happy path   | Roublard, équipement B | Équipement « 100 po » ; CA 12 (10 + 2) ; seule l'attaque à mains nues | system |
| Happy path   | Guerrier, équipement C | Équipement « 155 po » ; CA 12 (10 + 2) | system |
| Happy path   | Barbare, achat de points Dex 8, Con 14 | CA 11 (10 − 1 + 2) | system |
| Happy path   | Guerrier construit → Moine | Style retiré des Capacités ; CA, PV, attaques, sauvegardes réécrites ; une retouche de Langues gardée | system |
| Edge case    | Style de combat pour un Barbare ; aucun Style pour un Guerrier | refus avec raison | system |
| Edge case    | Botte Arc long pour un Barbare ; Botte Hache à deux mains pour un Roublard ; une Botte pour un Moine | refus avec raison | system |
| Edge case    | Expertise pour un Guerrier ; 3 Expertises ; Expertise d'une compétence non maîtrisée | refus avec raison | system |
| Edge case    | Expertise sur une compétence de l'historique ou de l'espèce | acceptée | system |
| Edge case    | outil pour un Roublard ; Outils de voleur pour un Moine | refus avec raison | system |

## Tasks to do

### `1)` Données des classes et des outils

> Les trois classes et les outils tels que le SRD 5.2.1 français les donne.

1. `Tool` : les 17 outils d'artisan, les 10 instruments de musique, accessoires de déguisement, instruments de navigateur, matériels de contrefaçon, d'empoisonneur et d'herboriste, outils de voleur ; catégorie ; `Background` le référence (calligraphe, voleur).
2. `SrdArmor.LEATHER` (« Armure de cuir », légère, 11).
3. `StartingEquipment` : `BARBARIAN_A` (sans armure ; Hache à deux mains, Hachette ; 15 po), `MONK_A` (Lance, Dague ; l'outil choisi ; 11 po), `ROGUE_A` (Armure de cuir ; Dague, Épée courte, Arc court ; 8 po) ; options tout en po `BARBARIAN_B` 75, `MONK_B` 50, `ROGUE_B` 100, `FIGHTER_C` 155.
4. `CharacterClass` : PV, sauvegardes, compétences et nombre, valeurs suggérées, nombre de Bottes, texte des maîtrises, outil fixe (Roublard : outils de voleur) ou catégories au choix (Moine : artisan ou instrument), Style de combat (Guerrier seul), armes de Botte permises (Guerrier : toutes ; Barbare : corps à corps ; Roublard : courantes et de guerre Finesse ou Légère ; Moine : aucune), nombre d'Expertises (Roublard 2), Défense sans armure (Con, bouclier permis ; Sag, sans bouclier), Arts martiaux (Moine).

### `2)` Construction et validation

> Un choix impossible pour la classe est refusé avec la raison.

1. `Build` : + `expertise` (ensemble, vide par défaut) et `tool` (`null` : à choisir) ; `withSpecies` et `withBackground` les gardent.
2. `suggested(classe)` : valeurs suggérées, premier équipement, ses premières armes permises en Bottes, Style Défense pour le Guerrier, sinon `null` ; `fighter()` inchangé.
3. `validate` : Style requis ou refusé selon la classe ; chaque Botte parmi les armes permises ; Expertise au plus le nombre de la classe, chacune parmi les compétences maîtrisées (classe, historique, espèce, Doué) ; outil seulement pour une classe qui en choisit un, dans ses catégories.

### `3)` Valeurs dérivées

> La fiche porte ce que la classe donne, et changer de classe ne réécrit que ce qui change.

1. CA : armure (et bouclier, et Défense) ; sans armure, la meilleure de 10 + Dex et de la Défense sans armure de la classe. CA et attaques se calculent depuis ce qui est porté (armure, bouclier, armes) et la classe, jamais depuis `StartingEquipment` lui-même, pour que l'inventaire ne change que leur source.
2. Attaques : arme de Moine (corps à corps courante, ou de guerre Légère) → meilleure de For et Dex au toucher et aux dégâts, dé de l'arme ou 1d6 s'il est plus fort ; toute classe : « Attaque à mains nues » après les armes, allonge 1,5 m, contondants, For + maîtrise et 1 + For, ou pour le Moine meilleure de For et Dex + maîtrise et 1d6 + meilleure de For et Dex.
3. Compétences : Expertise → `EXPERTISE`.
4. Capacités : Bottes (si la classe en a), puis par classe (Second souffle et Style ; Rage et Défense sans armure ; Arts martiaux et Défense sans armure ; Expertise, Attaque sournoise, Argot des voleurs).
5. Maîtrises : « Outils : » outil de classe puis outil d'historique, sans doublon, ligne absente sans outil ; Équipement A du Moine : l'outil choisi, « à choisir » sinon.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Chaque classe a le dé de vie, les sauvegardes, les compétences, les valeurs suggérées, les équipements et les maîtrises de sa note SRD ; `Tool` a les outils des notes `Équipement/Outils` ; les historiques gardent leur outil |
| 2 | Chaque refus du Test Scope a sa raison ; les constructions suggérées sont valides |
| 3 | Barbare CA 15 et PV 14 ; Moine CA 14, Dague 1d6 + 2, mains nues 1d6 + meilleure de For et Dex ; Guerrier mains nues 1 + For ; Roublard CA 13, Discrétion en expertise, Outils de voleur une fois ; options tout en po sans arme ni armure ; Guerrier → Moine retire le Style et garde une retouche indépendante |
