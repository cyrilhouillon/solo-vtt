---
type: story
status: ready
parent: aidd_docs/backlog/epics/tenir-le-carnet-de-sa-campagne.md
depends_on:
  - aidd_docs/backlog/stories/lier-une-page-du-carnet-a-une-autre.md
---

# Story : retrouver un écran par son adresse

**En tant que** joueur solo
**Je veux** que chaque écran de ma campagne ait sa propre adresse dans le navigateur
**Afin de** ouvrir une page du Carnet dans un autre onglet, revenir en arrière et rouvrir exactement où j'étais

## Acceptance

- L'adresse dit où je suis : l'accueil, le Carnet d'une campagne sur une page donnée, ou l'écran de combat d'une campagne.
- Le clic du milieu sur un lien de page du Carnet, ou sur une page de l'arbre, ouvre cette page dans un nouvel onglet du navigateur.
- Précédent et Suivant du navigateur repassent par les pages et les écrans visités, sans quitter l'application.
- Recharger l'onglet, ou ouvrir une adresse gardée en favori, rouvre le même écran sur la même page.
- Ouvrir une campagne sans page dans l'adresse mène, comme aujourd'hui, à la dernière page consultée.
- Une adresse vers une campagne ou une page qui n'existe pas ne casse rien : campagne inconnue, retour à l'accueil ; page inconnue, le Carnet de la campagne s'ouvre comme sans page.
- Précédent et Suivant attendent l'enregistrement de la page en cours, comme les boutons de l'application, et restent sur place s'il échoue.
