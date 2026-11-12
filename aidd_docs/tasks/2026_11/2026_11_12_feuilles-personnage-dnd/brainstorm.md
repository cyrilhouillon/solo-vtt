---
status: approved
---

# Feuilles de personnage D&D 2024

Pour le joueur solo : créer et tenir ses personnages D&D 2024 dans le Carnet de sa campagne, puis les jouer en combat, la fiche suivant ce qui leur arrive.

## Clair

- Un **module D&D**, activable par campagne comme Mythic, porte les fiches et les règles. Le combat garde son découpage actuel : le générique dans le cœur, le D&D dans `dnd-engine`.
- Première étape : les **12 classes du SRD, niveau 1**, avec **espèces et historiques** du SRD. Espèces et historiques peuvent arriver dans des stories à part.
- **Création guidée** (classe, espèce, historique, caractéristiques, choix de classe, équipement) et **saisie libre**. Toute valeur se retouche à la main. Changer un choix guidé recalcule ce qui en dérive : changer d'armure recalcule la CA, même si elle avait été retouchée.
- La fiche est un **écran structuré** qui passe pour une page du Carnet, rangé dans la section **« Personnages joueurs »**.
- La fiche est son **propre agrégat, à événements**. Poser un personnage en combat en fait une copie dans l'agrégat du combat. Chaque coup émet aussi un événement sur la fiche : PV perdus, emplacements de sort dépensés. Le rejeu d'un combat ne rejoue que le combat, jamais les événements de la fiche.
- Un même personnage dans deux combats en cours : cas marginal, rien ne l'interdit.
- En préparation de combat, les **PJ de la campagne remplacent la Guerrière et la Magicienne** dans le catalogue.
- Chaque capacité que le moteur ne sait pas jouer (sorts, Rage…) est **listée et devient sa propre story**.
- **Règles** : le SRD 5.2.1 français (notes Obsidian, CC-BY 4.0, donc versable au dépôt avec attribution), en lecture seule et commun aux campagnes D&D. Source : `F:\Obsidian\Cyril\TTRPG\TTRPG\Compendio SRD\Français`. La **section Règles du Carnet est un chantier à part**. Cette étape n'y prend que ce qu'une fiche niveau 1 affiche.
- **Aide au survol, façon BG3** : passer sur un terme ouvre une infobulle avec sa règle. T la fige, la souris peut alors y entrer, et un terme dans l'infobulle ouvre la sienne, et ainsi de suite.
- **Fin de l'étape** : un personnage par classe, créé en guidé, niveau 1, avec espèce et historique. Sa fiche est complète et lisible, et ses capacités ouvrent leur règle au survol. Posé en combat, il attaque avec son arme, et ses PV baissent sur sa fiche. Chaque capacité que le moteur ne joue pas a sa story.

## Supposé

- **Repos court et long** : story à part. D'ici là, PV et emplacements ne remontent qu'à la main.
- La copie du combat est prise **au moment de la pose**. Une retouche de la fiche pendant le combat ne change pas la créature déjà posée.

## Ouvert

- **Parties d'avant** : les combats enregistrés, parties et cartes enregistrées qui citent `guerriere` ou `magicienne` deviendraient illisibles (rejeu en 409, chargement de carte refusé). Faut-il garder ces entrées cachées pour l'ancien contenu ?
- Une **campagne sans module D&D** garde-t-elle l'écran de combat ? Aujourd'hui, il est entièrement D&D.
- **Contenu exact de la création guidée** : méthode des caractéristiques (tableau standard, achat de points, dés), choix de l'équipement de départ, dons d'origine, sorts mineurs et sorts connus des lanceurs de sorts au niveau 1.
