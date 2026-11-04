---
type: story
status: done
parent: aidd_docs/backlog/epics/tenir-le-carnet-de-sa-campagne.md
order: 1
---

# Story : écrire une page dans le Carnet

**En tant que** joueur solo
**Je veux** arriver sur le Carnet de ma campagne et y écrire une page en texte riche
**Afin de** raconter ma session et la relire la prochaine fois

## Acceptance

- Ouvrir une campagne mène au Carnet, et non plus à l'écran de jeu.
- Une campagne sans page affiche un Carnet vide, qui invite à créer la première.
- Je crée une page en lui donnant un titre, puis j'écris au fil du texte avec mise en forme : titres, gras, italique, listes, citations. Il n'y a pas de zone de texte brute.
- Chaque page a un verrou :
  - verrouillée, elle se lit et ses liens s'ouvrent, mais rien ne s'y modifie ;
  - déverrouillée, je l'édite au même endroit, avec la même mise en page : seuls l'icône du verrou et le curseur changent.
- Une page que je viens de créer reste déverrouillée jusqu'à ce que je la quitte. Toute page déjà existante s'ouvre verrouillée, et je la déverrouille d'un geste.
- Ce que j'écris s'enregistre tout seul, sans bouton. Annuler et rétablir (Ctrl+Z, Ctrl+Y) défont et refont la frappe.
- Rouvrir la campagne ramène à la dernière page consultée, même après un redémarrage du serveur.
- Depuis le Carnet, j'ouvre l'écran de combat. Depuis le combat, je reviens au Carnet sur la même page.
