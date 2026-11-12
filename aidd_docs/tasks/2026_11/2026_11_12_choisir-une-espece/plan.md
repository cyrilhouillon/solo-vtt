---
objective: "Sur la fiche d'un personnage construit en guidé, le joueur choisit l'une des 9 espèces du SRD 5.2.1 et ses sous-choix ; taille, vitesse et traits s'écrivent sur la fiche, les traits qui changent un calcul (vitesse, PV, compétence) sont recalculés, changer d'espèce les recalcule, et chaque trait que le moteur ne joue pas a sa story."
status: in-progress
---

# Plan: choisir une espèce en création guidée

Source: aidd_docs/backlog/stories/choisir-une-espece-en-creation-guidee.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Espèces et choix d'espèce dans `dnd-engine` | [`phase-1.md`](./phase-1.md) | none |
| 2   | Fiche, construction et options d'espèce au serveur | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Choix de l'espèce sur la fiche | [`phase-3.md`](./phase-3.md) | 2 |
| 4   | Stories des traits non joués et mémoire | [`phase-4.md`](./phase-4.md) | 1, 2, 3 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf | Source de règles du projet, lue dans sa traduction française (notes Obsidian `Compendio SRD/Français/Personnages/Espèces/*.md`, CC-BY 4.0). Tailles : M pour Drakéide, Elfe, Goliath, Nain, Orc ; P pour Gnome, Halfelin ; M ou P au choix pour Humain et Tieffelin. Vitesse 9 m, Goliath 10,50 m, Elfe sylvestre 10,50 m. Vision dans le noir 18 m (Drakéide, Elfe, Gnome, Tieffelin), 36 m (Nain, Orc, Drow). Sous-choix : ascendance draconique (10 dragons → type de dégâts : Airain, Or, Rouge feu ; Argent, Blanc froid ; Bleu, Bronze foudre ; Cuivre, Noir acide ; Vert poison) ; lignage elfique (Drow, Elfe sylvestre, Haut-elfe) ; lignage gnome (des forêts, des roches) ; ascendance gigante (6 géants) ; héritage fiélon (Abyssal poison, Chtonien nécrotique, Infernal feu) ; caractéristique d'incantation Int, Sag ou Cha (Elfe, Gnome, Tieffelin) ; Sens aiguisés (Intuition, Perception ou Survie) ; Compétent (une compétence au choix) ; Ténacité naine (+1 PV max par niveau) ; Polyvalent (un don d'origine au choix). |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| architecture | Aujourd'hui `Build` porte classe, caractéristiques, compétences, style, équipement et Bottes (`Build.java:15`) ; espèce, vitesse et taille restent au joueur (`GuidedValues.java:14`). Changement : `Build` gagne un `SpeciesChoice` facultatif (espèce, taille, option, compétence, caractéristique d'incantation, chacun facultatif sauf l'espèce) ; `GuidedValues` dérive espèce, taille, vitesse, traits d'espèce, compétence d'espèce et +1 PV du Nain ; un champ d'espèce `null` (construction sans espèce) n'est pas dérivé : la fiche garde ce qu'elle porte. | phase 1 |
| architecture | `CharacterSheet` gagne `size` et `speciesTraits` (textes, comme la fiche 2024 sépare Species Traits et Class Features). Les options d'espèce sont communes aux classes : nouvelle route `GET …/dnd/species`, à côté de `classes` (`SheetResource.java:57`). | phase 1, 2 |
| regression | Moyen : le constructeur de `CharacterSheet` gagne deux paramètres, appelé par `CharacterSheet.blank`, `GuidedValues.rebuild`, `DndDtos.SheetValuesDto.toSheet` et les tests `CharacterProfileTest`, `CharacterSheetTest`, `GuidedValuesTest`, `SheetHistoryTest` ; celui de `Build` gagne `species`, appelé par `DndDtos.java:145` et les tests `BuildTest` (`:32`, `:38`, `:44`), `GuidedValuesTest` (`:22`, `:99`), `SheetHistoryTest` (`:56`, `:95`, `:115`), `SheetResourceTest` (`:155`, `:405`) ; `SheetResourceTest` construit aussi `SheetValuesDto` (`:160`, `:171`, `:540`, `:556`) et `BuildDto` (`:422-433`) ; `CharacterSheet.profile` (`CharacterSheet.java:111`) passe la vitesse au combat (`SheetCharacters`), inchangé : la vitesse dérivée de l'espèce y arrive d'elle-même ; `BuildDto` et `SheetValuesDto` gagnent des champs lus par `SheetStore` (`SheetStore.java:70`). Couvert par `SheetResourceTest` (fiches d'avant écrites à la main, `:472`, `:570`) et `GuidedValuesTest` ; la phase 2 ajoute la relecture d'une construction d'avant sans espèce. Pas de phase filet séparée : ces tests existent. | phase 1, 2 |
| runtime | Aucune dépendance hors dépôt. Le parcours client (choix d'espèce, retouche, changement d'espèce) n'est pas observé : à vérifier dans le navigateur sur 8081 / 5174. | phase 3 |
| documentation | `architecture.md` (espèce dans la construction, champs `size` et `speciesTraits`, route `species`), `codebase-map.md` (`Species`, `SpeciesOption`, `CreatureSize`, `SpeciesChoice`), `project-brief.md` (choix d'espèce), backlog (stories des traits non joués, tâche de la liste, story au statut `done`). | phase 4 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Les moteurs n'ont aucune dépendance runtime | données des espèces et calcul dans `dnd-engine`, sans Jackson |
| Un module n'est nommé que par les assemblages | tout reste dans `dnd-server` et `vtt-web/modules/dnd/` |
| Code et messages en français, identifiants en anglais ; tests aux noms français | toutes les phases |
| `ErrorMapper` rend 400 pour `IllegalArgumentException` | refus d'un choix d'espèce |
| Règles tirées du SRD 5.2.1 ; texte français des notes Obsidian (CC-BY 4.0) | données et textes des traits |
| Une fiche d'avant se relit ; un document abîmé rend 409 et n'est jamais écrasé (`SheetStore`) | nouveaux champs absents des fichiers existants |
| Tests navigateur sur 8081 / 5174 uniquement | phase 3 |
| Vérifications avant commit : `mvn -q compile`, `mvn -q test`, `npm run build` | chaque phase |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Tous les sous-choix du SRD sont proposés (taille, ascendance, lignage, héritage, compétence, caractéristique d'incantation), chacun facultatif : un sous-choix manquant s'écrit « à choisir » dans les traits | décision du joueur ; même règle que les compétences de classe, qui peuvent rester à choisir ; une espèce choisie donne d'emblée une construction valide |
| Le don d'origine de l'Humain (Polyvalent) s'écrit « un don d'origine au choix » ; son choix arrive avec la story Historique, qui introduit les dons d'origine | décision du joueur ; une seule story porte les dons |
| Deux champs neufs sur la fiche, « Taille » et « Traits d'espèce », hors de « Capacités » | décision du joueur ; la fiche 2024 sépare traits d'espèce et capacités de classe, et le recalcul par différence réécrit chaque texte à part |
| L'espèce appartient à la construction (une classe d'abord) ; « — » ne se propose qu'avant le premier choix d'espèce, comme pour la classe | un champ d'espèce dérivé qu'on pourrait retirer devrait savoir quoi rendre ; une espèce saisie à la main avant le choix survit |
| Une compétence d'espèce déjà maîtrisée par la classe est refusée (400), et le client l'éteint | le SRD ne donne pas deux fois la même maîtrise ; refus explicite plutôt que doublon silencieux |
