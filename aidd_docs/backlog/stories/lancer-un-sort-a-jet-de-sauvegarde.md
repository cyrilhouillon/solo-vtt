---
type: story
status: proposed
parent: aidd_docs/backlog/epics/tenir-ses-personnages-dnd-2024-dans-le-carnet.md
depends_on:
  - aidd_docs/backlog/stories/creer-en-guide-un-magicien.md
order: 32
---

# Story : lancer un sort à jet de sauvegarde

**En tant que** joueur solo
**Je veux** que mon lanceur de sorts lance un sort qui force ses cibles à une sauvegarde contre son DD
**Afin de** jouer ses sorts de zone et d'état sans les résoudre à la main

## Acceptance

- Le moteur sait faire jouer un jet de sauvegarde à une créature, contre le DD des sorts du lanceur (8 + caractéristique d'incantation + maîtrise, SRD 5.2.1, « Incantation ») ; réussite et échec s'écrivent au journal.
- Sorts du Magicien concernés : Aspersion acide (une ou deux cibles), Bouffée de poison, Mains brûlantes (cône), Vague tonnante (cube, repousse), Couteau de glace (explosion), Couleurs dansantes (Aveuglé), Sommeil (Neutralisé), Fou rire (À terre, Neutralisé), Graisse (À terre), Charme-personne (Charmé).
- Une zone (cône, cube, sphère) vise les créatures de la carte qu'elle couvre ; moitié des dégâts sur une sauvegarde réussie quand le sort le dit.
- Un sort du 1er niveau dépense un emplacement, qui baisse sur la fiche ; sans emplacement, il n'est pas proposé.
- Chaque sort, capacité ou règle rencontrée que le moteur ne joue pas entre dans la [liste de la tâche](../tasks/lister-les-capacites-de-niveau-1-que-le-moteur-ne-joue-pas.md) avec sa story.
