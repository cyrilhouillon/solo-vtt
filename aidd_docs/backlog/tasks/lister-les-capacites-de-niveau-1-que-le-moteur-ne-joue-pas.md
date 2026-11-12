---
type: task
status: proposed
parent: aidd_docs/backlog/epics/tenir-ses-personnages-dnd-2024-dans-le-carnet.md
---

# Tâche : lister les capacités de niveau 1 que le moteur ne joue pas et ouvrir leurs stories

## Outcome

Chaque capacité, trait, don d'origine et sort qu'affiche une fiche de niveau 1 et que le moteur ne sait pas jouer a sa propre story dans le backlog.

## Scope

- **Inclut :**
  - les capacités de niveau 1 des 12 classes du SRD 5.2.1, les traits des 9 espèces, les dons d'origine des 4 historiques, les sorts mineurs et de niveau 1 des listes de classe ;
  - une liste tenue au fil des stories de création guidée (Guerrier ; espèce ; historique ; Barbare, Moine, Roublard ; Magicien ; autres lanceurs de sorts) ;
  - une story par capacité, ou par groupe de capacités au comportement commun, rattachée à son propre epic.
- **Exclut :**
  - mettre en œuvre ces capacités dans le moteur ;
  - les niveaux au-delà de 1 et les sous-classes.

## Done When

- La liste couvre tout ce qu'affichent les fiches de niveau 1 des 12 classes, des 9 espèces et des 4 historiques.
- Chaque élément non joué de la liste renvoie à une story du backlog.

## Liste

| Capacité | Source | Jouée | Story |
| --- | --- | --- | --- |
| Second souffle | Guerrier, niveau 1 | non | [utiliser Second souffle en combat](../stories/utiliser-second-souffle-en-combat.md) |
| Bottes d'arme (8 bottes) | Guerrier, niveau 1 | non | [appliquer les Bottes d'arme en combat](../stories/appliquer-les-bottes-d-arme-en-combat.md) |
| Style de combat : Archerie | Guerrier, niveau 1 | non (le +2 est écrit sur la fiche) | [jouer les styles de combat du Guerrier](../stories/jouer-les-styles-de-combat-du-guerrier.md) |
| Style de combat : Armes à deux mains | Guerrier, niveau 1 | non | [jouer les styles de combat du Guerrier](../stories/jouer-les-styles-de-combat-du-guerrier.md) |
| Style de combat : Combat à deux armes | Guerrier, niveau 1 | non | [jouer les styles de combat du Guerrier](../stories/jouer-les-styles-de-combat-du-guerrier.md) |
| Style de combat : Défense | Guerrier, niveau 1 | portée par la fiche (+1 CA en armure) | — |
| Vision dans le noir (18 m, 36 m) | Drakéide, Elfe, Gnome, Nain, Orc, Tieffelin ; Drow | hors moteur : pas d'éclairage ni de vision | — |
| Résistance aux dégâts (ascendance draconique) | Drakéide | non (écrite sur la fiche) | [appliquer les résistances d'espèce en combat](../stories/appliquer-les-resistances-d-espece-en-combat.md) |
| Souffle | Drakéide | non | [utiliser le Souffle du Drakéide en combat](../stories/utiliser-le-souffle-du-drakeide-en-combat.md) |
| Connaissance de la pierre | Nain | hors combat | — |
| Résistance naine : Résistance au poison | Nain | non | [appliquer les résistances d'espèce en combat](../stories/appliquer-les-resistances-d-espece-en-combat.md) |
| Résistance naine : Avantage contre Empoisonné | Nain | non (pas de sauvegarde au moteur) | [avoir l'Avantage aux sauvegardes d'espèce](../stories/avoir-l-avantage-aux-sauvegardes-d-espece.md) |
| Ténacité naine | Nain | portée par la fiche (+1 PV max) | — |
| Ascendance féerique | Elfe | non (pas de sauvegarde au moteur) | [avoir l'Avantage aux sauvegardes d'espèce](../stories/avoir-l-avantage-aux-sauvegardes-d-espece.md) |
| Sens aiguisés | Elfe | portée par la fiche (compétence maîtrisée) | — |
| Transe | Elfe | hors combat (repos non modélisés) | — |
| Lignage elfique : Elfe sylvestre (vitesse 10,50 m) | Elfe | portée par la fiche (vitesse) | — |
| Lignages elfiques et gnomes, héritages fiélons, Présence d’outre-monde : sorts mineurs et sorts d’espèce | Elfe, Gnome, Tieffelin | non (aucun sort joué) | [créer en guidé les autres lanceurs de sorts](../stories/creer-en-guide-les-autres-lanceurs-de-sorts.md) |
| Ruse gnome | Gnome | non (pas de sauvegarde au moteur) | [avoir l'Avantage aux sauvegardes d'espèce](../stories/avoir-l-avantage-aux-sauvegardes-d-espece.md) |
| Gnome des roches : appareils mécaniques | Gnome | hors combat | — |
| Ascendance gigante (6 bénéfices) | Goliath | non | [utiliser l'ascendance gigante du Goliath en combat](../stories/utiliser-l-ascendance-gigante-du-goliath-en-combat.md) |
| Forte carrure | Goliath | hors combat (charge ; Agrippé non modélisé) | — |
| Agilité halfeline | Halfelin | non : le client refuse déjà deux jetons à moins d’une case, sans règle de taille | — |
| Brave | Halfelin | non (pas de sauvegarde au moteur) | [avoir l'Avantage aux sauvegardes d'espèce](../stories/avoir-l-avantage-aux-sauvegardes-d-espece.md) |
| Chance | Halfelin | non | [relancer un 1 avec la Chance du Halfelin](../stories/relancer-un-1-avec-la-chance-du-halfelin.md) |
| Discrétion naturelle | Halfelin | hors combat (Furtivité non modélisée) | — |
| Compétent | Humain | portée par la fiche (compétence maîtrisée) | — |
| Ingénieux (Inspiration héroïque) | Humain | hors combat (repos non modélisés) | — |
| Polyvalent (don d’origine) | Humain | écrit « au choix » sur la fiche | [choisir un historique en création guidée](../stories/choisir-un-historique-en-creation-guidee.md) |
| Acharnement, Poussée d’adrénaline | Orc | non | [jouer Acharnement et Poussée d'adrénaline de l'Orc](../stories/jouer-acharnement-et-poussee-d-adrenaline-de-l-orc.md) |
| Héritage fiélon : Résistance | Tieffelin | non (écrite sur la fiche) | [appliquer les résistances d'espèce en combat](../stories/appliquer-les-resistances-d-espece-en-combat.md) |
| Vol draconique, Forme de géant | Drakéide, Goliath | hors champ : niveau 5 | — |
