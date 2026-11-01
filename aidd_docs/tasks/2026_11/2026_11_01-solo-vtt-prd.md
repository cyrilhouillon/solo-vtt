# Solo VTT

Un Virtual TableTop web conçu pour le jeu de rôle en solo. Il ne dépend d'aucun système de jeu : le moteur arbitre les règles, et des outils solo (oracles, générateurs, modules type GME) remplacent le MJ.

## Overview

- **Pour qui :** le joueur solo, qui tient à la fois le rôle du joueur et celui du MJ.
- **Valeur :** réunir dans un seul outil ce qu'on fait aujourd'hui avec un VTT, des PDF, des tables aléatoires et un carnet de notes.
- **Pourquoi maintenant :** `dnd-engine` a validé l'architecture d'un moteur de règles. Il faut en tirer les principes communs avant d'y brancher une interface et du contenu.

## Problem Statement

- Les VTT actuels sont faits pour une table multijoueur avec un MJ humain. En solo, une bonne part de leurs fonctions ne sert à rien, et celles dont on a besoin (oracles, génération, suivi des intrigues) n'y sont pas.
- Le joueur solo jongle donc entre plusieurs outils. Il perd le fil, son lore se disperse et le jeu ralentit.
- Les outils qui automatisent les règles sont liés à un seul système. Changer de système, c'est changer d'outil et perdre ses cartes et ses notes.

## Goals

**Première version : jouer un combat, en local**

1. Mener un combat D&D 2024 complet dans l'outil : feuille de personnage, carte, résolution par le moteur.
2. Résoudre en un clic une action de la feuille, cible choisie sur la carte comprise, avec les règles appliquées par le moteur.
3. Sur la carte, placer les créatures, les déplacer et mesurer les distances. Rien de plus pour l'instant.

**Cible du produit**

4. Brancher un nouveau système sans toucher au cœur. Le critère : Blades in the Dark est jouable.
5. Garder une carte utilisable sous tous les systèmes, avec des distances réelles (mètres ou pieds), la lumière et le déplacement libre des pions.
6. Retrouver en quelques secondes, dans un wiki fluide et ergonomique, ce qui s'est passé lors des sessions précédentes.
7. Ajouter un module (système, oracle, générateur, aide solo type Mythic GME ou Adventure Crafter) sans modifier le cœur, que l'auteur du module soit le projet ou un tiers.
8. Proposer un service hébergé : chaque utilisateur a un compte et retrouve ses campagnes depuis n'importe quel appareil.

## Non-Goals

- Le jeu multijoueur et la synchronisation en temps réel entre plusieurs joueurs.
- Le rôle de MJ humain pour d'autres joueurs (outils de MJ dédiés, vision séparée MJ/joueurs).
- Les fonctionnalités d'IA : elles viendront bien plus tard.
- Dans la première version :
  - les comptes utilisateurs et l'hébergement ;
  - la lumière, la vision et les murs ;
  - le wiki, les oracles, les générateurs et les modules solo.
- Embarquer les tables sous licence commerciale de Mythic GME ou d'Adventure Crafter sans accord. À défaut d'accord, l'utilisateur remplit ses tables lui-même.
- Convertir les entités d'un système vers un autre.
- L'application mobile native.

## User Stories

- En tant que joueur solo, je veux choisir le système de jeu de ma campagne, afin que feuilles, règles et carte s'y adaptent.
- En tant que joueur solo, je veux une feuille de personnage interactive, afin de lancer une action en cliquant dessus.
- En tant que joueur solo, je veux désigner la cible d'une action sur la carte, afin que le moteur résolve l'action avec la bonne cible.
- En tant que joueur solo, je veux que le moteur me pose les décisions qui me reviennent (réaction, choix d'option, portée dans un système narratif), afin de garder la main sans avoir à connaître toutes les règles.
- En tant que joueur solo, je veux placer mon groupe et des adversaires sur une carte et connaître les distances, afin de jouer un combat tactique.
- En tant que joueur solo, je veux charger une carte prête, afin de lancer une scène vite.
- En tant que joueur solo, je veux créer une carte à la volée, afin d'improviser une scène imprévue.
- En tant que joueur solo, je veux une carte qui gère lumière et vision, afin de jouer l'exploration de façon tactique.
- En tant que joueur solo, je veux déplacer librement n'importe quel pion, même un pion d'un autre système, afin d'utiliser la carte comme support visuel.
- En tant que joueur solo, je veux poser une question à un oracle, afin de trancher ce qu'un MJ aurait décidé.
- En tant que joueur solo, je veux générer un donjon pendant que je l'explore, afin de découvrir des lieux que je n'ai pas préparés.
- En tant que joueur solo, je veux générer une région en hexcrawl pendant que je l'explore, afin de voyager dans un monde ouvert.
- En tant que joueur solo, je veux activer un module solo type Mythic GME ou Adventure Crafter, et remplir ses tables si elles ne sont pas fournies, afin de suivre mes intrigues avec sa méthode.
- En tant que joueur solo, je veux un wiki fluide et ergonomique pour le lore et les résumés de session, afin de garder la mémoire de ma campagne.
- En tant que joueur solo, je veux me connecter à mon compte, afin de retrouver mes campagnes sur tous mes appareils.
- En tant que créateur de contenu, je veux publier un système ou un module, afin d'étendre l'outil sans toucher à son cœur.

## Acceptance Criteria

**Première version**

- Une campagne a un système de jeu choisi à sa création, et ses feuilles de personnage suivent le modèle de ce système.
- Une carte peut être créée vide, avec une échelle en mètres ou en pieds. On peut y placer et y déplacer des créatures, et la distance entre deux créatures s'affiche.
- Un clic sur une action de la feuille qui demande une cible passe en mode ciblage sur la carte. L'action se résout ensuite, et son résultat apparaît dans le journal.
- Une action qui demande une décision au joueur suspend la partie jusqu'à la réponse. La partie reprend ensuite sans perte.
- Un combat D&D 2024 se joue du premier jet jusqu'aux dégâts appliqués aux créatures de la carte. Les portées sont calculées à partir des distances de la carte.
- Fermer puis rouvrir une campagne restitue l'état exact. Les données sont conservées durablement, en dehors du navigateur.

**Cible du produit**

- La carte ne connaît que des distances réelles. Chaque système décide de leur usage :
  - soit il traduit la distance en portée selon ses règles ;
  - soit il ne s'en sert pas, et obtient la portée autrement (en la demandant au joueur pendant la résolution, ou en faisant confiance au joueur).
- Les règles d'un système ne s'appliquent qu'à travers son moteur. Retirer ce système ne casse ni les cartes, ni le wiki, ni les oracles.
- Une carte reste jouable (terrain, lumière, distances, déplacement) sous n'importe quel système installé.
- Les entités d'un autre système s'affichent et se déplacent, mais elles ne peuvent pas servir de cible aux règles du système actif.
- Blades in the Dark se joue avec les mêmes cartes, le même wiki et les mêmes oracles que D&D 2024.
- Un oracle donne une réponse à une question, et chaque tirage est conservé dans l'historique de la session.
- Les générateurs de donjon et d'hexcrawl révèlent le contenu au fil de l'exploration. Le résultat reste cohérent quand on revient sur ses pas.
- Un module s'installe, s'active et se désactive pour une campagne, sans effet sur les autres campagnes. Un module de tiers suit le même chemin qu'un module du projet.
- Une page du wiki se crée, s'édite, se lie à d'autres pages et se retrouve par une recherche, sans quitter la partie en cours.
- Une fois connecté, un utilisateur ne voit que ses propres campagnes, et les retrouve à l'identique sur un autre appareil.

## Dependencies

- `dnd-engine` : c'est la source des principes du « moteur de moteurs ». Il faut en extraire ce qui est commun à tous les systèmes et le séparer de ce qui est propre à D&D.
- Licences :
  - le SRD 5.2 (CC-BY-4.0) pour D&D 2024 ;
  - le SRD de Blades in the Dark (CC BY 3.0) ;
  - un accord commercial avec Word Mill Games pour les tables de Mythic GME et d'Adventure Crafter (recherche en cours, sinon l'utilisateur fournit les tables).
- Le format de carte, et le format des modules et des systèmes, doivent être définis et stables avant d'ouvrir aux tiers.

## Open Questions

1. **Modules de tiers (reporté) :** quel niveau de confiance leur accorder (code exécuté ou contenu seulement) ? Et comment les distribuer ? À trancher avant d'ouvrir aux tiers.
