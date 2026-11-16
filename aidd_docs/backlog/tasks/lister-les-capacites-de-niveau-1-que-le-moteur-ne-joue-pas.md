---
type: task
status: proposed
parent: aidd_docs/backlog/epics/tenir-ses-personnages-dnd-2024-dans-le-carnet.md
---

# Tâche : lister les capacités de niveau 1 que le moteur ne joue pas et ouvrir leurs stories

## Outcome

Chaque capacité, trait, don d'origine et sort qu'affiche une fiche de niveau 1, et chaque règle qu'ils supposent (repos, recharge, Concentration…), que le moteur ne sait pas jouer a sa propre story dans le backlog.

## Scope

- **Inclut :**
  - les capacités de niveau 1 des 12 classes du SRD 5.2.1, les traits des 9 espèces, les dons d'origine des 4 historiques, les sorts mineurs et de niveau 1 des listes de classe ;
  - une liste tenue au fil des stories de création guidée (Guerrier ; espèce ; historique ; Barbare, Moine, Roublard ; Magicien ; autres lanceurs de sorts) : toute règle rencontrée en chemin y entre, même si l'epic l'exclut, avec la story qui la portera ; chaque story de l'epic le rappelle dans son acceptance ;
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
| Bottes d'arme (8 bottes) | Guerrier, Barbare, Roublard, niveau 1 | non | [appliquer les Bottes d'arme en combat](../stories/appliquer-les-bottes-d-arme-en-combat.md) |
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
| Transe | Elfe | hors combat (repos non modélisés) | [se reposer, court et long](../stories/se-reposer-court-et-long.md) |
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
| Ingénieux (Inspiration héroïque) | Humain | hors combat (repos non modélisés) | [se reposer, court et long](../stories/se-reposer-court-et-long.md) |
| Polyvalent (don d’origines) | Humain | portée par la fiche (don choisi en création guidée) | — |
| Doué | Polyvalent | portée par la fiche (trois compétences maîtrisées) | — |
| Initié à la magie : sorts mineurs et sort du 1er niveau | Acolyte, Sage ; Polyvalent | non (sorts écrits « à choisir ») | [créer en guidé les autres lanceurs de sorts](../stories/creer-en-guide-les-autres-lanceurs-de-sorts.md) |
| Sauvagerie martiale | Soldat ; Polyvalent | non (écrite sur la fiche) | [relancer les dégâts avec Sauvagerie martiale](../stories/relancer-les-degats-avec-sauvagerie-martiale.md) |
| Vigilant : Maîtrise de l’Initiative | Criminel ; Polyvalent | portée par la fiche (+ bonus de maîtrise à l’initiative) | — |
| Vigilant : Échange d’Initiative | Criminel ; Polyvalent | non | [échanger son initiative avec Vigilant](../stories/echanger-son-initiative-avec-vigilant.md) |
| Acharnement, Poussée d’adrénaline | Orc | non | [jouer Acharnement et Poussée d'adrénaline de l'Orc](../stories/jouer-acharnement-et-poussee-d-adrenaline-de-l-orc.md) |
| Héritage fiélon : Résistance | Tieffelin | non (écrite sur la fiche) | [appliquer les résistances d'espèce en combat](../stories/appliquer-les-resistances-d-espece-en-combat.md) |
| Vol draconique, Forme de géant | Drakéide, Goliath | hors champ : niveau 5 | — |
| Rage | Barbare, niveau 1 | non (écrite sur la fiche) | [entrer en Rage en combat](../stories/entrer-en-rage-en-combat.md) |
| Défense sans armure | Barbare, Moine, niveau 1 | portée par la fiche (CA 10 + Dex + Con ou Sag, sans armure) | — |
| Arts martiaux : dé d'Arts martiaux, Attaques lestes | Moine, niveau 1 | portée par la fiche (1d6, meilleure de For et Dex, armes de Moine et mains nues) | — |
| Arts martiaux : attaque à mains nues supplémentaire | Moine, niveau 1 | non | [frapper à mains nues en action Bonus avec Arts martiaux](../stories/frapper-a-mains-nues-en-action-bonus-avec-arts-martiaux.md) |
| Attaque à mains nues | toute classe | portée par la fiche (attaque « Attaque à mains nues ») | — |
| Expertise | Roublard, niveau 1 | portée par la fiche (deux compétences en expertise) | — |
| Attaque sournoise | Roublard, niveau 1 | non (écrite sur la fiche) | [porter une Attaque sournoise](../stories/porter-une-attaque-sournoise.md) |
| Argot des voleurs | Roublard, niveau 1 | hors combat (langue, écrite dans Capacités) | — |
| Doué : maîtrises d'outils | Polyvalent | non (Doué ne donne que des compétences) | [choisir des outils avec Doué](../stories/choisir-des-outils-avec-doue.md) |
| Repos court et long : dés de vie, PV, recharge des capacités limitées (Rage, Second souffle, Acharnement, Poussée d'adrénaline) et des emplacements, changement d'une Botte d'arme | règle, toute classe | non (PV et utilisations remis à la main) | [se reposer, court et long](../stories/se-reposer-court-et-long.md) |
| Formation aux armures : armure sans formation (Désavantage aux Tests d20 de For et Dex, pas de sorts) | règle, toute classe | non (la CA de l'armure compte) | [subir les pénalités d'armure](../stories/subir-les-penalites-d-armure.md) |
| Formation aux armures : bouclier sans formation (pas de bonus de CA) | règle, toute classe | portée par la fiche (CA calculée sans le bouclier) | — |
| Force requise des armures lourdes (vitesse −3 m) | cotte de mailles, clibanion, harnois | non | [subir les pénalités d'armure](../stories/subir-les-penalites-d-armure.md) |
| Désavantage en Discrétion des armures | armures du SRD | non (Discrétion non jouée) | [subir les pénalités d'armure](../stories/subir-les-penalites-d-armure.md) |
| Maîtrise d'arme (pas de bonus de maîtrise sans elle) | règle, toute classe | portée par la fiche (attaque calculée) | — |
| Propriétés Polyvalente et Deux mains, mains libres | armes du SRD | non (« tenue » sans compter les mains) | [manier une arme à une ou deux mains](../stories/manier-une-arme-a-une-ou-deux-mains.md) |
| Propriété Légère : attaque de la seconde main en action Bonus | armes du SRD | non (deux armes identiques tenues : une seule attaque) | [attaquer avec une seconde arme Légère](../stories/attaquer-avec-une-seconde-arme-legere.md) |
| Sorts : sorts mineurs, grimoire, sorts préparés, caractéristique d'incantation, DD et attaque de sort, emplacements | Magicien, niveau 1 | portée par la fiche (choisis en création guidée, retouchables ; DD et attaque calculés) | — |
| Emplacements de sort dépensés en combat | Magicien, niveau 1 | oui (Bouclier ; la fiche les suit comme les PV) | — |
| Changer de sorts préparés au repos long | Magicien, niveau 1 | non (retouche à la main) | [se reposer, court et long](../stories/se-reposer-court-et-long.md) |
| Savoir rituel, rituels | Magicien, niveau 1 | hors combat (Alarme, Appel de familier, Compréhension des langues, Détection de la magie, Disque flottant, Identification, Serviteur invisible, Texte illusoire) | — |
| Restauration magique | Magicien, niveau 1 | non (repos non modélisés) | [se reposer, court et long](../stories/se-reposer-court-et-long.md) |
| Bouclier : +5 CA en réaction | sort, Magicien | oui | — |
| Bouclier : durée jusqu'au début du tour suivant | sort, Magicien | non (le +5 n'est jamais retiré) | [tenir les effets à durée et la Concentration](../stories/tenir-les-effets-a-duree-et-la-concentration.md) |
| Bouclier contre Projectile magique | sort, Magicien | non | [lancer Projectile magique](../stories/lancer-projectile-magique.md) |
| Sorts à jet d'attaque : Trait de feu, Rayon de givre, Poigne électrique, Contact glacial, Rayon empoisonné, Orbe chromatique, Couteau de glace ; Coup au but | sorts, Magicien | non (une autre attaque saisie peut les approcher) | [lancer un sort d'attaque](../stories/lancer-un-sort-d-attaque.md) |
| Sorts à jet de sauvegarde : Aspersion acide, Bouffée de poison, Mains brûlantes, Vague tonnante, Couteau de glace, Couleurs dansantes, Sommeil, Fou rire, Graisse, Charme-personne | sorts, Magicien | non (pas de sauvegarde au moteur) | [lancer un sort à jet de sauvegarde](../stories/lancer-un-sort-a-jet-de-sauvegarde.md) |
| Projectile magique | sort, Magicien | non | [lancer Projectile magique](../stories/lancer-projectile-magique.md) |
| Effets à durée : Armure du mage, Simulacre de vie, Grande foulée, Repli expéditif, Protection contre le mal et le bien | sorts, Magicien | non (pas d'effet à durée) | [tenir les effets à durée et la Concentration](../stories/tenir-les-effets-a-duree-et-la-concentration.md) |
| Concentration | règle, sorts | non | [tenir les effets à durée et la Concentration](../stories/tenir-les-effets-a-duree-et-la-concentration.md) |
| Sorts utilitaires : Élémentalisme, Illusion mineure, Lumière, Lumières dansantes, Main du mage, Message, Prestidigitation, Réparation, Déguisement, Image silencieuse, Feuille morte, Saut, Nappe de brouillard | sorts, Magicien | hors combat (ni éclairage, ni vision, ni chute au moteur) | — |
| Composantes (V, S, M) et focaliseur d'incantation | règle, sorts | hors moteur (ni mains ni composantes suivies) | — |
