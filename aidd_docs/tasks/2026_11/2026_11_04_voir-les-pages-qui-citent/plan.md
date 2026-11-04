---
objective: "Sous le texte d'une page du Carnet, le joueur voit, par leur titre actuel, les pages qui contiennent un lien vers elle, en ouvre une au clic, et lit « Aucune page ne renvoie ici. » quand rien ne la cite."
status: implemented
---

# Plan: voir les pages qui citent une page

Source: aidd_docs/backlog/stories/voir-les-pages-qui-citent-une-page.md (epic `aidd_docs/backlog/epics/tenir-le-carnet-de-sa-campagne.md`, order 4)

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Le serveur trouve les pages qui citent une page | [`phase-1.md`](./phase-1.md) | none |
| 2   | La page montre qui la cite | [`phase-2.md`](./phase-2.md) | 1 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | Le format du lien est celui du dépôt (`vtt-web/src/notebook/pageLink.ts:12` : `{ type: 'pageLink', attrs: { pageId, label } }`), contrat déjà fixé par la story des liens. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Aucune dépendance nouvelle : Jackson (déjà dans `vtt-server`) parcourt le contenu ; le client n'ajoute qu'un appel `fetch`. | none |
| Regression | Faible. Serveur : une route et une classe ajoutées, `NotebookStore` et son adaptateur inchangés (`list()` rend déjà le contenu de chaque page). Client : `NotebookScreen.vue` (lu) relit la liste à `show` ; `open`, l'enregistrement et l'arbre ne changent pas. Couvert par `NotebookApiTest` (le Carnet de bout en bout) et `mvn -q test`, client par parcours navigateur sur 8081 / 5174. Pas de phase filet : aucun comportement existant ne change. | 1, 2 |
| Architecture | Départ : le serveur garde le contenu d'une page sans l'interpréter (`architecture.md`). Cible : il lit, en lecture seule, les seuls nœuds `pageLink` pour répondre « qui cite cette page » ; il continue de ne rien valider à l'enregistrement. Le calcul se fait à la demande, en parcourant toutes les pages, sans index. | 1 |
| Documentation | `architecture.md` (route `…/pages/{id}/citing`, le serveur lit les nœuds `pageLink`, parcours sans index, liste relue à l'ouverture, titre lu dans la liste des pages), `codebase-map.md` (`PageLinks`, route, `CitingPages.vue`), `project-brief.md` (fonctionnalité Carnet), `DESIGN.md` (bloc « Pages qui renvoient ici »). Story : `status: done`. Plan : `status: implemented`. | 1, 2 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Page inconnue : `NotFoundException` levée par la ressource (404) ; `ErrorMapper` rend 400 pour une `IllegalArgumentException` (`architecture.md`) | La nouvelle route vise une page par son id. |
| Lire le Carnet ne rejoue pas la partie : `NotebookResource` est construit sur le `CampaignStore` seul (`architecture.md`) | La route vit dans `NotebookResource`. |
| Une fiche illisible est ignorée, jamais écrasée (`architecture.md`) | Un contenu qui ne se lit pas ne fait pas échouer la liste : la page est sautée. |
| Le lien lit le titre de sa cible dans la liste des pages que `NotebookScreen` relit à chaque geste (`architecture.md`) | La liste des pages citantes suit la même source : un renommage s'y voit sans nouvel appel. |
| Toute nouvelle règle arrive avec son test ; tests d'API par campagne neuve (`testing.md`) | `NotebookApiTest` et un test unitaire de `PageLinks`. |
| Client sans tests automatisés ; jamais de test navigateur sur 8080 / 5173 (`testing.md`) | Parcours sur les serveurs de test 8081 / 5174, campagnes dans le scratchpad. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Le serveur calcule les pages citantes (`GET …/notebook/pages/{id}/citing`) en parcourant le contenu de toutes les pages à chaque appel, sans index | Le client n'a que la liste des titres, pas le contenu des autres pages. Sans index, rien à tenir à jour ni à reconstruire ; un Carnet solo compte quelques centaines de pages, que `list()` lit déjà en entier pour l'arbre. Un index viendra si la recherche (story suivante) en demande un. |
| Une page qui se cite elle-même n'apparaît pas dans sa propre liste | « Qui pointe ici » sert à trouver les autres pages ; s'y lister n'apprend rien, et la liste n'a ainsi jamais à suivre la page qu'on écrit. |
| La liste est relue à l'ouverture de la page ; son affichage tire le titre de `notebook.pages` | Avec une seule page ouverte, un lien ajouté ou retiré l'est dans une autre page, donc avant de rouvrir celle-ci (`open` attend l'enregistrement) ; un renommage relit déjà `notebook.pages`. Aucun appel de plus après chaque geste. À revoir avec les onglets (story order 7). |
