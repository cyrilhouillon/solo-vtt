---
type: story
status: done
source: aidd_docs/tasks/2026_11/2026_11_01-solo-vtt-prd.md
depends_on:
  - aidd_docs/backlog/stories/mettre-une-image-de-fond-sur-la-carte.md
order: 3
---

# Story : réaligner la grille sur l'image

**En tant que** joueur solo
**Je veux** caler la grille de la carte sur celle de l'image en la voyant bouger
**Afin de** jouer sur une image dont la grille ne commence pas pile au bord ou dont la taille de case est approximative

## Acceptance

- Sur une carte avec image, j'ouvre le calage : la grille s'affiche par-dessus l'image, même si je l'avais masquée.
- Je décale la grille horizontalement et verticalement, et j'ajuste la taille d'une case, au pixel près ; la grille bouge sur l'image au fur et à mesure.
- Le calage se fait à la souris et au clavier.
- Je valide ou j'annule ; annuler rend le calage d'avant.
- Après validation, les dimensions de la carte, les distances et le calage sur la case (Maj) suivent la nouvelle grille.
- Les créatures déjà posées restent au même endroit de l'image.
- Le calage n'est pas possible pendant un combat.
- Le calage est gardé avec la carte, après un redémarrage comme dans une carte enregistrée.
