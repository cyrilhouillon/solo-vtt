---
objective: "Supprimer une page du Carnet, après une confirmation qui dit ses sous-pages et les pages qui y renvoient, l'envoie à la corbeille avec ses sous-pages (liens morts, onglets fermés) ; la corbeille la restaure à sa place, ou à la racine si sa place n'existe plus, et se vide après confirmation, sans retour possible."
status: implemented
---

# Plan: supprimer une page et la restaurer depuis la corbeille

Source: aidd_docs/backlog/stories/supprimer-une-page-et-la-restaurer-depuis-la-corbeille.md (epic `aidd_docs/backlog/epics/tenir-le-carnet-de-sa-campagne.md`, order 8).

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Le serveur met les pages à la corbeille | [`phase-1.md`](./phase-1.md) | none |
| 2   | Le Carnet supprime, restaure et vide | [`phase-2.md`](./phase-2.md) | 1 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | none |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Aucune dépendance nouvelle : Jackson (déjà dans `vtt-server`) lit les deux champs ajoutés aux fiches, `deletedAt` en texte ISO-8601 car le mapper de `JsonFileNotebookStore.java:37` n'a pas de module `java.time` ; les routes sont montées par `CampaignResource` sans changement ; le client n'ajoute que des appels `fetch` et deux `<dialog>`, comme `MovePageDialog`. | none |
| Regression | Moyenne. `list()` et `find()` des deux adaptateurs (lus, `NotebookStore.java:23-28,125-178`, `JsonFileNotebookStore.java:43-63`) ne rendent plus les pages à la corbeille ; tout le reste passe par eux : `known` (404 sur une page à la corbeille pour contenu, titre, parent, création dessous, onglets), `PageLinks.citing`, `PageSearch` (lien sans `label` vers une page à la corbeille : titre `null`, déjà sauté, `PageSearch.java:109-110`), `NotebookResource.get` (onglets d'une page disparue déjà omis, `NotebookResource.java:44-51`). Une fiche d'avant, sans les nouveaux champs, se lit vivante. Tests couvrant : `NotebookApiTest`, `NotebookStoreTest`, `JsonFileNotebookStoreTest`, `PageLinksTest`, `PageSearchTest`, `NotebookResourceTest` ; pas de phase filet : ces tests verrouillent le comportement des pages vivantes (non relancés pendant le plan : à vérifier en phase 1). Client : `NotebookScreen.vue` (`closing` généralisé à plusieurs onglets), `PageBranch.vue` (menu), `PageTree.vue` / `context.ts` (un geste de plus) ; validé par `npm run build` et parcours sur 8081 / 5174. | 1, 2 |
| Architecture | Départ : une page existe ou non ; aucune suppression. Cible : le port `NotebookStore` gagne `delete`, `trash`, `restore`, `emptyTrash` ; une page à la corbeille garde sa fiche, marquée (`deletedWith` : la page dont la suppression l'a emportée, `deletedAt`), invisible de `list()` et `find()` ; restaurer retire la marque du lot, vider efface les fiches. Le client garde ses liens morts tels quels : ils revivent dès que la page revient dans la liste. | 1, 2 |
| Documentation | `architecture.md` (corbeille du Carnet, routes, onglets fermés à la suppression), `codebase-map.md` (routes, `DeletePageDialog.vue`, `TrashDialog.vue`), `project-brief.md` (fonctionnalité Carnet), `DESIGN.md` (dialogues Supprimer et Corbeille, bouton Corbeille du panneau Pages). Story : `status: done`. Plan : `status: implemented`. | 1, 2 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Lire le Carnet ne rejoue pas la partie : `NotebookResource` est construit sur le `CampaignStore` seul (`architecture.md`) | Les routes de suppression et de corbeille vivent dans `NotebookResource`. |
| Page inconnue : `NotFoundException` (404) levée par la ressource ; `IllegalArgumentException` → 400 (`architecture.md`) | Supprimer, restaurer une page inconnue ou absente de la corbeille rend 404. |
| Une fiche illisible est ignorée, jamais écrasée ; écriture atomique par fichier (`JsonFileNotebookStore`) | Marquer, démarquer ou effacer passe fiche par fiche, sous le verrou du Carnet. |
| Aucune image n'est jamais supprimée (`architecture.md`) | Vider la corbeille efface les fiches, pas les images qu'elles citent. |
| Avant d'ouvrir une autre page, de créer ou de quitter l'écran, le Carnet attend l'envoi et reste sur place s'il échoue (`architecture.md`) | Supprimer une page ouverte attend sa frappe ; un échec ne supprime rien. |
| Un lien vers une page absente de la liste est mort (`architecture.md`, `PageLinkView.vue`) | Aucun changement aux liens : morts à la suppression, vivants à la restauration. |
| Toute nouvelle règle arrive avec son test ; tests d'API par campagne neuve (`testing.md`) | `NotebookApiTest`, `NotebookStoreTest`, `JsonFileNotebookStoreTest`, `PageLinksTest`. |
| Client sans tests automatisés ; jamais de test navigateur sur 8080 / 5173 (`testing.md`) | Parcours sur 8081 / 5174, campagnes dans le scratchpad. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Une page à la corbeille garde sa fiche à sa place, marquée `deletedWith` (id de la page supprimée par le joueur, la sienne pour elle) et `deletedAt` ; `list()` et `find()` l'ignorent | Restaurer ne réécrit que des fiches, sans déplacer de fichier ; tout appelant existant (liens, recherche, onglets, `known`) l'ignore sans changer. Une sous-page supprimée seule avant son parent reste son propre lot : restaurer le parent ne la ramène pas. |
| La corbeille liste les suppressions (une entrée par page supprimée par le joueur, avec son nombre de sous-pages), la plus récente d'abord ; on restaure une entrée, pas une sous-page seule | Le geste annulé est la suppression ; une sous-page revient avec sa page. |
| Restaurer remet le parent d'origine s'il est vivant, sinon la racine ; les sous-pages gardent le leur | Critère de la story ; le lot revient tel quel sous sa page. |
| « N pages y renvoient » compte les pages hors du lot qui ont un lien vers la page ou l'une de ses sous-pages : `GET …/pages/{id}/deletion` rend sous-pages et pages citantes en un appel | Tous ces liens meurent ; compter les seuls liens vers la page sous-estimerait la perte. |
| Vider la corbeille efface tout, sans suppression définitive d'une seule entrée | Seul geste demandé par la story. |
| La corbeille s'ouvre d'un bouton « Corbeille » au pied du panneau Pages, dans un dialogue modal ; la suppression se confirme dans un dialogue modal, ouvert depuis « Supprimer… » du menu de la ligne | Mêmes gestes que « Déplacer sous… » (clavier, toucher) ; l'arbre ne change pas de forme. |
