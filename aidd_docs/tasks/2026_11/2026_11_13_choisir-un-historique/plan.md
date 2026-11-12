---
objective: "Sur la fiche d'un personnage construit en guidé, le joueur choisit l'un des 4 historiques du SRD 5.2.1 (augmentations, compétences, outil, don d'origine, équipement A ou B) et l'Humain son don d'origine (Polyvalent) ; la fiche s'en remplit, changer d'historique recalcule ce qui en dérive, et chaque don d'origine que le moteur ne joue pas a sa story."
status: in-progress
---

# Plan: choisir un historique en création guidée

Source: aidd_docs/backlog/stories/choisir-un-historique-en-creation-guidee.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Historiques et dons d'origine dans `dnd-engine` | [`phase-1.md`](./phase-1.md) | none |
| 2   | Fiche, construction et options d'historique au serveur | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Choix de l'historique et de Polyvalent sur la fiche | [`phase-3.md`](./phase-3.md) | 2 |
| 4   | Stories des dons non joués et mémoire | [`phase-4.md`](./phase-4.md) | 1, 2, 3 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf | Source de règles, lue dans sa traduction française (notes Obsidian `Compendio SRD/Français/Personnages/Historiques/*.md`, `Dons/*.md`, `Règles/Historiques de personnage.md`, `Règles/Création de votre personnage.md`, `Équipement/Outils/Boîte de jeux.md`, CC-BY 4.0). Acolyte : Int, Sag, Cha ; Initié à la magie (Clerc) ; Intuition, Religion ; matériel de calligraphe ; A : matériel de calligraphe, livre (de prières), symbole sacré, parchemin (10 feuilles), robe, 8 po. Criminel : Dex, Con, Int ; Vigilant ; Discrétion, Escamotage ; outils de voleur ; A : 2 dagues, outils de voleur, 2 bourses, pied-de-biche, tenue de voyage, 16 po. Sage : Con, Int, Sag ; Initié à la magie (Magicien) ; Arcanes, Histoire ; matériel de calligraphe ; A : bâton de combat, matériel de calligraphe, livre (d'histoire), parchemin (8 feuilles), robe, 8 po. Soldat : For, Dex, Con ; Sauvagerie martiale ; Athlétisme, Intimidation ; une boîte de jeux au choix (cartes à jouer, dés, échecs draconiques, jeu des dragons) ; A : arc court + 20 flèches, carquois, lance, boîte de jeux (même type), trousse de soins, tenue de voyage, 14 po. B : 50 po pour tous. Augmentations : +2 et +1, ou +1 aux trois, jamais au-dessus de 20. Dons d'origine : Doué (trois compétences ou outils), Initié à la magie (deux sorts mineurs et un sort de niveau 1 de la liste du Clerc, du Druide ou du Magicien ; Int, Sag ou Cha), Sauvagerie martiale (relance des dés de dégâts une fois par tour), Vigilant (+ bonus de maîtrise à l'Initiative ; échange d'Initiative). Le SRD ne dit rien d'une compétence reçue deux fois. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| architecture | Aujourd'hui `Build` porte classe, caractéristiques, compétences, style, équipement, Bottes et espèce (`Build.java:16`) ; l'historique reste au joueur (`GuidedValues.java:15`, `rebuild` garde `sheet.background()`), et `SheetState.rebuild` compare les valeurs de base `build.scores()` (`SheetState.java:46-47`). Changement : `Build` gagne un `BackgroundChoice` facultatif (historique, augmentations, équipement A/B, boîte de jeux, caractéristique d'incantation du don) ; `SpeciesChoice` gagne le don de Polyvalent (`OriginFeatChoice` : don, liste de sorts, caractéristique d'incantation, compétences de Doué) ; `Build.abilities()` = valeurs + augmentations, que `GuidedValues` et `SheetState.rebuild` lisent à la place de `scores()`. | phase 1 |
| architecture | `CharacterSheet` gagne `feats` (texte « Dons », comme la fiche 2024 sépare Feats, Class Features et Species Traits). `GuidedValues` dérive historique, compétences, outil (dans Maîtrises), équipement (classe puis historique), attaques des armes de l'équipement A, Dons et l'initiative de Vigilant ; `SheetCharacters` passe déjà l'initiative de la fiche au combat (`CharacterSheet.profile`), sans changement. Nouvelles routes `GET …/dnd/backgrounds` et `GET …/dnd/origin-feats`, à côté de `species` (`SheetResource.java:67`). | phase 1, 2 |
| regression | Moyen : le constructeur de `CharacterSheet` gagne `feats`, appelé par `CharacterSheet.blank`, `GuidedValues.rebuild`, `DndDtos.SheetValuesDto.toSheet` et les tests `CharacterProfileTest`, `CharacterSheetTest`, `GuidedValuesTest`, `SheetHistoryTest`, `SheetResourceTest` ; celui de `Build` gagne `background` (`Build.fighter`, `withSpecies`, `DndDtos.BuildDto.toBuild`, `BuildTest`, `GuidedValuesTest`, `SheetHistoryTest`, `SheetResourceTest`) ; celui de `SpeciesChoice` gagne `originFeat` (`SpeciesChoice.of`, `SpeciesChoiceDto.toChoice`, `SpeciesChoiceTest`, `BuildTest:137`, `GuidedValuesTest:143`, `:168`, `:182`, `SheetResourceTest:644`) ; les DTO `SheetValuesDto` (`SheetResourceTest:164`, `:175`, `:544`, `:560`), `BuildDto` (`:426-437`) et `SpeciesChoiceDto` (`:633`) changent d'arité ; `GuidedValues` gagne `background` et `feats` (`NONE`, `of`, internes). Le texte « Polyvalent : un don d’origine au choix » change, vérifié par `GuidedValuesTest:162` et `SpeciesChoiceTest:80`. `SheetStore.storedBuild` (`SheetStore.java:110`) relit une construction gardée : une construction d'avant, sans historique, doit se relire. Couvert par `SheetResourceTest` (fiches d'avant écrites à la main) et `GuidedValuesTest` ; aucun test ne montre encore la relecture d'une construction d'avant sans historique ni `feats` : la phase 2 l'ajoute (non vérifié jusque-là). Pas de phase filet : les tests de fiches d'avant existent. | phase 1, 2 |
| runtime | Aucune dépendance hors dépôt. Le parcours client (choix d'historique, augmentations, Polyvalent, changement d'historique) n'est pas observé : à vérifier dans le navigateur sur 8081 / 5174. | phase 3 |
| documentation | `architecture.md` (historique et don d'origine dans la construction, `Build.abilities`, champ `feats`, routes `backgrounds` et `origin-feats`), `codebase-map.md` (`Background`, `BackgroundChoice`, `BackgroundEquipment`, `OriginFeat`, `OriginFeatChoice`, `GamingSet`, `SpellList`), `project-brief.md` (choix d'historique), backlog (stories des dons non joués, liste de la tâche, story au statut `done`, story « saisie libre » encore `ready` alors que livrée). | phase 4 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Les moteurs n'ont aucune dépendance runtime | données des historiques et des dons dans `dnd-engine`, sans Jackson |
| Un module n'est nommé que par les assemblages | tout reste dans `dnd-server` et `vtt-web/modules/dnd/` |
| Code et messages en français, identifiants en anglais ; tests aux noms français | toutes les phases |
| `ErrorMapper` rend 400 pour `IllegalArgumentException` | refus d'un choix d'historique ou de don |
| Règles tirées du SRD 5.2.1 ; texte français des notes Obsidian (CC-BY 4.0) | données et textes des historiques et des dons |
| Une fiche d'avant se relit ; un document abîmé rend 409 et n'est jamais écrasé (`SheetStore`) | nouveaux champs absents des fichiers existants |
| Tests navigateur sur 8081 / 5174 uniquement | phase 3 |
| Vérifications avant commit : `mvn -q compile`, `mvn -q test`, `npm run build` | chaque phase |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Initié à la magie s'écrit avec sa liste et la caractéristique d'incantation choisie ; ses sorts restent « à choisir », repris par les stories des lanceurs de sorts | décision du joueur ; le moteur n'a aucune donnée de sort |
| Polyvalent (Humain) se choisit ici : l'un des 4 dons d'origine, avec ses sous-choix (liste de sorts et incantation d'Initié à la magie ; trois compétences de Doué) | décision du joueur ; promis par le plan de l'espèce |
| Doué donne trois compétences, pas d'outil | les outils ne sont qu'un texte de la fiche, sans table au moteur ; le joueur écrit un outil à la main dans Maîtrises |
| Une compétence que l'historique donne ne se choisit ni dans la classe, ni dans l'espèce, ni dans Doué : refus 400, le client l'éteint ; choisir un historique qui en chevauche une la remet « à choisir » | décision du joueur ; même règle que la compétence d'espèce déjà maîtrisée par la classe |
| Les armes de l'équipement A de l'historique deviennent des attaques, après celles de la classe, sans doublon de nom | décision du joueur ; jouables une fois le PJ posé |
| Un champ neuf « Dons » sur la fiche, hors de « Capacités » | la fiche 2024 sépare Feats ; le recalcul par différence réécrit chaque texte à part |
| L'historique appartient à la construction ; il se change, ne se retire pas ; « — » ne se propose qu'avant le premier choix | même règle que l'espèce : un historique saisi à la main avant le choix survit |
| Augmentations requises à l'appel ; le client propose +1 aux trois en choisissant un historique | toujours valide, quelles que soient les valeurs ; le joueur passe ensuite à +2/+1 |
