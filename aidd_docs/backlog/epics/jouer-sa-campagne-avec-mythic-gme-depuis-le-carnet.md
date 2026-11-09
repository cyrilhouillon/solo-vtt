---
type: epic
status: done
goal: aidd_docs/tasks/2026_11/2026_11_01-solo-vtt-prd.md
depends_on:
  - aidd_docs/backlog/epics/tenir-le-carnet-de-sa-campagne.md
---

# Epic : jouer sa campagne avec Mythic GME depuis le Carnet

Depuis le Carnet, le joueur pose ses questions à l'oracle, ouvre et clôt ses scènes et tient ses fils et personnages avec Mythic GME 2e édition ; chaque jet s'écrit dans la page ouverte, et Mythic est un module branché sans toucher au cœur.

## Context and Value

- **Pour qui :** le joueur solo sans MJ, qui remplace les décisions d'un MJ par un oracle.
- **Constat aujourd'hui :** le Carnet garde le récit, mais l'oracle, le facteur de chaos et les listes vivent hors de l'outil (livre, dés, papier) ; le joueur recopie chaque résultat.
- **Valeur :** objectif 7 du PRD, « ajouter un module (système, oracle, générateur, aide solo type Mythic GME ou Adventure Crafter) sans modifier le cœur » : Mythic est le premier module et ouvre la voie à Adventure Crafter ; objectif 6 : la mémoire de la campagne contient aussi les réponses de l'oracle.

## Boundaries

- **Inclut :**
  - poser une question oui/non avec une probabilité : table de destin et facteur de chaos donnent oui, non, exceptionnels compris ;
  - les événements aléatoires : déclenchement, focus d'événement, sens tiré des tables de mots ;
  - la boucle de scène : scène prévue, modifiée ou interrompue selon le chaos, ajustement de scène, ajustement du chaos en fin de scène ;
  - table de destin, facteur de chaos, focus d'événement et ajustement de scène sont de la mécanique codée (non protégés, selon l'utilisateur) ;
  - les listes de fils et de personnages, vivantes, que les événements tirent ; visibles dans l'arborescence du Carnet, dans une section à part, quel que soit leur stockage ;
  - un jet lancé depuis le Carnet s'écrit dans la page ouverte ;
  - les tables de mots importées depuis un fichier fourni par le joueur, gardées dans ses données, jamais dans le dépôt ;
  - Mythic est un module : le cœur offre des points d'extension (panneau du Carnet, entrée d'arborescence, stockage propre), Mythic les utilise, sans code Mythic dans le cœur.
- **Exclut :**
  - livrer les tables de mots, en attente d'un accord commercial ;
  - Adventure Crafter, les variantes de Mythic et les autres oracles ;
  - les feuilles de personnage dans l'arborescence : le point d'extension doit les permettre, elles ne sont pas livrées ici ;
  - jeter l'oracle depuis l'écran de combat ;
  - publier un module tiers (chargement dynamique, place de marché).

## Success Evidence

- Le joueur importe ses tables de mots une fois.
- Dans « Session 6 », il pose « Le garde est-il corrompu ? (probable) » : réponse et chaos s'écrivent dans la page.
- Un double déclenche un événement aléatoire, qui tire un fil de sa liste et deux mots de sens.
- Il clôt la scène et ajuste le chaos ; la scène suivante sort modifiée.
- Ses fils et personnages sont dans l'arborescence, à côté des pages, et survivent à un redémarrage.
- Le code Mythic vit dans son propre module ; le cœur ne référence pas Mythic.

## Dependencies and Unknowns

| Item | Kind | Handling |
| --- | --- | --- |
| Epic Carnet | dependency | `done` |
| Forme des points d'extension | spike | `resolved` : [définir les points d'extension d'un module du Carnet](../spikes/definir-les-points-d-extension-d-un-module-du-carnet.md) ; une story « socle de module » précède toute story Mythic |
| Listes stockées en pages ou en données propres | decision | des pages, dans une vraie section de l'arbre par module (marqueur générique) |
| Format du fichier d'import des tables de mots | decision | tranchée : texte à titres, 100 entrées par section ([importer ses tables de mots](../stories/importer-ses-tables-de-mots-et-tirer-un-sens.md)) |
| Table de destin, facteur de chaos, focus d'événement et ajustement de scène libres de droits | assumption | déclarée par l'utilisateur, non vérifiée |
