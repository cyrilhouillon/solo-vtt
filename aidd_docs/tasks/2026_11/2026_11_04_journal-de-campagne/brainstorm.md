---
status: approved
---

# Journal de campagne : le wiki, cœur de la campagne

Pour le joueur solo, le journal devient l'écran principal de chaque campagne. Il y relit où il en était, raconte sa session et tient le lore dans des pages liées. Le combat devient un outil qu'on ouvre depuis ce journal.

## Clair

- Ouvrir une campagne mène au journal, sur la dernière page consultée, et non plus à la carte.
- Le journal et le combat sont deux écrans séparés. On passe de l'un à l'autre, et on revient au journal une fois le combat fini.
- Les pages forment une hiérarchie libre : une page peut contenir des sous-pages, et c'est le joueur qui crée les rubriques (« Lore », « PNJ »…).
- Une session est une page ordinaire de l'arbre, sans type ni date.
- L'édition est riche et se fait directement dans le texte, façon Notion ou Obsidian, avec titres, gras, listes et citations. Pas de zone de texte brute.
- Contenu de la v1 :
  - créer, renommer, déplacer et supprimer une page ;
  - lier une page existante ;
  - recherche ;
  - images dans une page ;
  - onglets de pages ;
  - liens entrants (« qui pointe ici »).
- Un lien survit au renommage et au déplacement de la page qu'il vise.
- La suppression d'une page encore liée avertit d'abord (« 3 pages y renvoient »). Si on confirme, les liens restants s'affichent comme liens morts.
- Les pages ne sont pas destinées à être éditées hors de l'application. Le stockage reste un détail interne, qui changera avec l'hébergement web.
- Hors v1 : créer une page à la volée depuis un lien (`[[Bertrand]]`). La solution retenue doit le permettre ensuite.
- Hors v1 : les liens vers des combats ou des donjons, et les oracles et aides solo. Ces derniers auront plus tard leur place à droite de l'écran du journal.

## Supposé

- Le contenu est stocké en Markdown, enrichi de balises à nous. Un lien y vise l'identifiant de la page, pas son titre, pour survivre au renommage. Ce texte n'est donc pas du Markdown pur.
- Les pages sont gardées par campagne, derrière un port comme `GameStore` et `MapStore`, avec une version fichier aujourd'hui.
- Les onglets ouverts se retrouvent au retour du combat et à la réouverture de la campagne.

## Ouvert

- **Étude technique à faire avant de trancher** : une bibliothèque d'édition (ou un ensemble de bibliothèques) couvre-t-elle l'enregistrement automatique, l'historique des versions, les liens par identifiant et la création à la volée ?
  - Si l'historique ou une corbeille vient sans surcoût, la suppression d'une page à sous-pages demande confirmation (« et ses 2 sous-pages ») puis supprime tout.
  - Sinon, elle refuse tant que la page a des enfants.
- Faut-il l'enregistrement automatique sans bouton dès la v1 ? On le souhaite, mais c'est lié à la même étude.
- Images : partagent-elles le magasin d'images actuel (commun, jamais supprimé) ou appartiennent-elles à la campagne ?
