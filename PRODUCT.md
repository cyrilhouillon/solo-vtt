# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

Vue 3 + TypeScript (Vite) pour le client web.

- Raison : choix de l'utilisateur, qui connaît Vue ; écosystème mature et API stable. TypeScript type les contrats du moteur (`StepResult`, `Decision`, événements).
- La carte est un module de rendu canvas isolé du framework (bibliothèque type PixiJS ou Konva, non choisie). Le glisser des jetons reste local ; le déplacement n'est soumis au moteur qu'au lâcher. Vue gère les panneaux (feuille, journal, décisions, initiative).
- Le moteur reste en Java (`vtt-engine`, `dnd-engine`) et tourne hors du navigateur. Le transport client ↔ moteur (HTTP, WebSocket…) n'est pas encore décidé.

## Users

- Le joueur solo, qui tient à la fois le rôle du joueur et celui du MJ.
- Il joue seul, en session, sur un grand écran avec souris et clavier. Tablette et mobile sont secondaires.
- Son travail : mener une partie sans jongler entre VTT, PDF, tables aléatoires et carnet de notes ; le moteur arbitre les règles, il répond aux décisions.

## Product Purpose

- `solo-vtt` est un Virtual TableTop web modulaire conçu pour le jeu de rôle en solo, indépendant du système de jeu.
- Première version : jouer un combat D&D 2024 complet, en local — feuille de personnage, carte, résolution par le moteur.
- Succès V1 : un combat de deux personnages contre trois adversaires (tous contrôlés par le joueur) se joue de bout en bout sans toucher au code ni à l'état ; le journal retrace chaque action ; la partie reprend à l'identique après fermeture.

## Positioning

- Les VTT existants supposent une table multijoueur et un MJ humain ; les outils qui automatisent les règles sont liés à un seul système.
- `solo-vtt` vise le joueur seul : un « moteur de moteurs » arbitre les règles de n'importe quel système branché, et des outils solo (oracles, générateurs, modules type Mythic GME) remplacent le MJ. Cartes, wiki et oracles survivent au changement de système.

## Operating Context

- Boucle de jeu V1 : préparer une scène, placer les créatures sur la carte, enchaîner les tours selon l'initiative, cliquer sur une action de la feuille, cibler sur la carte, lire le résultat dans le journal, répondre aux décisions du moteur (réactions comme Bouclier).
- Le moteur suspend la partie sur une décision et attend la réponse ; une seule décision en attente à la fois.
- Longues sessions de jeu : l'outil reste ouvert pendant toute la partie.

## Capabilities and Constraints

- V1 : carte minimale (placer, déplacer dans la limite de la vitesse, distances en mètres ou en pieds) ; feuilles interactives pour personnages et adversaires ; attaques résolues jusqu'aux dégâts (résistance puis vulnérabilité) ; tours par initiative ; fin du combat à tout moment ; persistance durable hors du navigateur.
- Hors V1 : sorts, conditions à durée, lumière/vision/murs, comptes et hébergement, wiki, oracles, générateurs, modules solo, IA, second système.
- Hors produit : multijoueur temps réel, outils de MJ pour d'autres joueurs, conversion d'entités entre systèmes, application mobile native.
- La carte ne connaît que des distances réelles ; chaque système décide de leur usage. Les entités d'un autre système s'affichent et se déplacent mais ne sont pas ciblables par les règles du système actif.
- Vocabulaire : Résolution, Décision, Réaction, Événement, Journal, CA, Avantage/Désavantage (voir `aidd_docs/memory/project-brief.md`).
- Langue : interface et messages en français.
- Indécis : transport client ↔ moteur ; confiance et distribution des modules tiers.

## Brand Commitments

- « Solo VTT » est un nom provisoire. Aucune voix, identité ou ressource de marque imposée.

## Evidence on Hand

- PRD : `aidd_docs/tasks/2026_11/2026_11_01-solo-vtt-prd.md`. Epic et stories V1 : `aidd_docs/backlog/`.
- Moteur fonctionnel avec démo console (`dnd.engine.demo.Main`).
- Contenu D&D : SRD 5.2 (CC-BY-4.0). Pas d'accord avec Word Mill Games : ne pas embarquer les tables de Mythic GME ou d'Adventure Crafter.
- Aucun utilisateur, témoignage, capture ou visuel existant : ne rien inventer.

## Product Principles

1. Le moteur arbitre, le joueur décide : l'interface rend visibles les décisions qui reviennent au joueur, sans exiger qu'il connaisse toutes les règles.
2. Un seul outil pendant la partie : rien ne doit forcer à sortir de la session.
3. Le cœur ignore le système : carte et outils solo fonctionnent sous n'importe quel système branché.
4. Tout est traçable : chaque action laisse une trace lisible dans le journal, et la partie reprend à l'identique.
5. Minimal d'abord : la V1 prouve le combat de bout en bout avant d'élargir.
