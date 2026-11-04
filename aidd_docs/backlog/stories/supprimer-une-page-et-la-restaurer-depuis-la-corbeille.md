---
type: story
status: ready
parent: aidd_docs/backlog/epics/tenir-le-carnet-de-sa-campagne.md
depends_on:
  - aidd_docs/backlog/stories/ranger-les-pages-du-carnet-dans-un-arbre.md
  - aidd_docs/backlog/stories/lier-une-page-du-carnet-a-une-autre.md
order: 8
---

# Story : supprimer une page et la restaurer depuis la corbeille

**En tant que** joueur solo
**Je veux** supprimer une page sans craindre de perdre par erreur
**Afin de** faire le ménage dans mon Carnet en toute sécurité

## Acceptance

- Supprimer une page demande confirmation. Le message dit combien de sous-pages partent avec elle (« et ses 2 sous-pages ») et combien de pages y renvoient (« 3 pages y renvoient »).
- Une fois confirmée, la page et ses sous-pages quittent l'arbre et vont à la corbeille. Les liens vers elles s'affichent comme morts.
- La corbeille liste les pages supprimées. J'en restaure une : elle revient à sa place, avec ses sous-pages, et ses liens revivent.
- Si sa place n'existe plus, la page restaurée revient à la racine.
- Je vide la corbeille, après confirmation. Ce qui est vidé ne se restaure plus.
- Un onglet ouvert sur une page supprimée se ferme.
