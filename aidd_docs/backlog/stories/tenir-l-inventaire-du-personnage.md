---
type: story
status: ready
parent: aidd_docs/backlog/epics/tenir-ses-personnages-dnd-2024-dans-le-carnet.md
depends_on:
  - aidd_docs/backlog/stories/creer-en-guide-un-barbare-un-moine-ou-un-roublard.md
order: 8
---

# Story : tenir l'inventaire du personnage

**En tant que** joueur solo
**Je veux** que la fiche porte ses objets, ses pièces d'or et ce que le personnage porte ou tient en main
**Afin d'** ajouter, retirer ou changer d'arme ou d'armure à la main, et que la CA et les attaques suivent

## Acceptance

- La fiche porte des objets : armes et armures du SRD, bouclier, autres objets en texte, quantités, et les pièces d'or.
- La création guidée y range l'équipement de départ de la classe et de l'historique (A, B ou C).
- Le joueur ajoute ou retire un objet, change une quantité ou ses po, à la main.
- Il équipe une armure, un bouclier et les armes qu'il tient : la CA et les attaques se recalculent par les règles de la classe (Défense sans armure, Arts martiaux, Style de combat, Finesse…).
- Un Moine qui porte une armure ou tient un bouclier perd Arts martiaux (SRD 5.2.1, « Martial Arts ») : ses armes de Moine et son attaque à mains nues reviennent au calcul ordinaire (Force, ou la meilleure en Finesse ; 1 + For à mains nues, sans dé). Aujourd'hui `GuidedValues.attack` et `unarmedStrike` l'appliquent sans regarder ce qui est porté, aucun équipement de départ du Moine n'ayant d'armure : ils devront recevoir l'armure et le bouclier portés, comme `armorClass`.
- Une fiche d'avant, dont l'Équipement n'est qu'un texte, se relit.
