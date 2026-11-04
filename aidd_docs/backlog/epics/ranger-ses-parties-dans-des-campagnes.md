---
type: epic
status: done
source: aidd_docs/tasks/2026_11/2026_11_01-solo-vtt-prd.md
depends_on:
  - aidd_docs/backlog/epics/jouer-un-combat-dnd-2024-en-local.md
---

# Epic : ranger ses parties dans des campagnes

À l'ouverture, le joueur choisit ou crée une campagne ; chaque campagne garde ses propres cartes enregistrées et sa partie en cours.

## Context and Value

- **Pour qui :** le joueur solo qui mène plusieurs histoires, ou veut en commencer une sans perdre l'autre.
- **Constat aujourd'hui :** l'outil ne tient qu'une partie et une liste de cartes, communes à tout : commencer une autre histoire écrase la partie en cours.
- **Valeur :** la campagne devient l'unité du produit (PRD : « fermer puis rouvrir une campagne restitue l'état exact »), socle du wiki, des modules et du choix de système à venir.

## Boundaries

- **Inclut :**
  - un écran d'accueil qui liste les campagnes existantes et propose d'en créer une ;
  - une campagne n'a qu'un nom ;
  - ouvrir une campagne mène à l'écran de jeu actuel, avec la partie en cours (carte, combat, journal, décision en attente) et les cartes enregistrées de cette campagne seulement ;
  - revenir à l'accueil et changer de campagne ; revenir à une campagne la retrouve à l'identique ;
  - la partie et les cartes enregistrées existantes sont reprises dans une campagne au premier lancement, sans perte.
- **Exclut :**
  - les comptes et la connexion : « se connecter » = ouvrir l'application ;
  - le choix du système de jeu (D&D 2024 seul) ;
  - plusieurs combats nommés ou un historique de combats par campagne : une partie en cours par campagne ;
  - renommer, supprimer, archiver ou dupliquer une campagne ;
  - le wiki, les personnages persistants de campagne, les modules.

## Success Evidence

- Le joueur crée deux campagnes, prépare dans chacune une carte enregistrée et un combat, passe de l'une à l'autre : chacune ne montre que ses cartes et reprend son combat à l'identique, y compris après redémarrage du serveur.
- Après la mise à jour, la partie et les cartes d'avant sont retrouvées dans une campagne.

## Dependencies and Unknowns

| Élément | Nature | Traitement |
| --- | --- | --- |
| Persistance d'une partie et des cartes enregistrées | dépendance | epic « jouer un combat D&D 2024 en local » (done) |
| Une partie en cours par campagne | décision | prise par le joueur |
| Reprise de l'existant dans une campagne | décision | prise par le joueur |
| Pas de comptes ni de choix de système | hypothèse | acceptée |
| Ni renommer ni supprimer une campagne | hypothèse | acceptée |
| Images de fond communes à toutes les campagnes (stockées par empreinte) | hypothèse | acceptée : sans effet visible pour le joueur |
