---
type: story
status: ready
source: aidd_docs/tasks/2026_11/2026_11_01-solo-vtt-prd.md
order: 1
---

# Story : mettre une image de fond sur la carte

**En tant que** joueur solo
**Je veux** créer une carte à partir d'une image de battlemap
**Afin de** jouer sur un vrai décor plutôt que sur une grille vide

## Acceptance

- Dans « Nouvelle carte », je choisis une image (PNG, JPEG ou WebP) et j'indique soit le nombre de cases sur sa largeur, soit la taille d'une case en pixels ; l'autre valeur se calcule et s'affiche.
- Les dimensions de la carte se déduisent de l'image : 975 × 975 px sur 30 cases de large donnent 30 × 30 cases, soit 45 × 45 m ou 150 × 150 ft.
- L'image remplit la carte, et la grille de la carte tombe sur celle de l'image.
- Je peux afficher ou masquer la grille de la carte, avec ou sans image ; ce choix est gardé avec la carte.
- Distances, portées, vitesse, pose et calage sur la case (Maj) se comportent comme sur une carte vide de même taille, que la grille soit affichée ou non.
- Une carte sans image reste possible, comme aujourd'hui.
- Après un redémarrage du serveur, la carte revient avec son image et son choix de grille.
