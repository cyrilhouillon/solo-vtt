---
type: story
status: proposed
parent: aidd_docs/backlog/epics/tenir-ses-personnages-dnd-2024-dans-le-carnet.md
depends_on:
  - aidd_docs/backlog/stories/manier-une-arme-a-une-ou-deux-mains.md
order: 30
---

# Story : attaquer avec une seconde arme Légère

**En tant que** joueur solo
**Je veux** que mon personnage qui tient une arme Légère dans chaque main porte son attaque de la seconde main
**Afin de** jouer le combat à deux armes sans le résoudre à la main

## Acceptance

- Après avoir attaqué avec une arme Légère par l'action Attaque, une créature peut porter, plus tard dans le même tour, une attaque supplémentaire par une action Bonus avec une autre arme Légère qu'elle tient (SRD 5.2.1, propriété Légère) : deux dagues tenues en sont deux.
- Cette attaque n'ajoute pas le modificateur de caractéristique aux dégâts, sauf s'il est négatif ; le style Combat à deux armes l'ajoute ([jouer les styles de combat du Guerrier](jouer-les-styles-de-combat-du-guerrier.md)).
- Une seule action Bonus par tour.
- Aujourd'hui, deux armes identiques tenues ne donnent qu'une attaque (`Inventory.weapons` les dédoublonne) : l'attaque de la seconde main se propose à part sur la feuille de combat, l'attaque principale reste celle de l'arme.
- L'attaque s'écrit au journal, et le combat se rejoue à l'identique.
