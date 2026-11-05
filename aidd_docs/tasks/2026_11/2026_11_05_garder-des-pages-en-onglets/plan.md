---
objective: "Toute page ouverte dans le Carnet (arbre, lien, résultat de recherche, page citante, création) entre dans un onglet ou active le sien ; le joueur passe d'un onglet à l'autre et les ferme, chaque onglet porte le titre actuel de sa page, et ses onglets et l'actif reviennent au retour du combat comme à la réouverture de la campagne, même après un redémarrage du serveur."
status: implemented
---

# Plan: garder des pages ouvertes en onglets

Source: aidd_docs/backlog/stories/garder-plusieurs-pages-ouvertes-en-onglets.md (epic `aidd_docs/backlog/epics/tenir-le-carnet-de-sa-campagne.md`, order 7). Décision du joueur : un clic ordinaire ouvre toujours un onglet (ou active le sien), sans geste dédié.

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Le serveur garde les onglets du Carnet | [`phase-1.md`](./phase-1.md) | none |
| 2   | Le Carnet montre ses onglets | [`phase-2.md`](./phase-2.md) | 1 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | none |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Aucune dépendance nouvelle : Jackson (déjà dans `vtt-server`) lit et écrit `carnet.json` ; le client n'ajoute qu'un appel `fetch`. | none |
| Regression | Moyenne. La « dernière page consultée » devient l'onglet actif : `NotebookStore.lastPage/setLastPage` (lu, `NotebookStore.java:59-67`), `JsonFileNotebookStore` (lu, `NotebookFile(lastPageId)`), `PUT …/last-page` (lu, `NotebookResource.java:133-141`), `NotebookDto.lastPageId` (lu, `Dtos.java:91`) et `api.setLastPage` (lu, `api.ts:108`, seul appelant : `NotebookScreen.vue:197,255`) sont remplacés. Tests couvrant : `NotebookApiTest` (l. 48, 74-81, 108-109), `NotebookStoreTest` (l. 33-46), `JsonFileNotebookStoreTest` (l. 33, 43-47, 77, 100), réécrits sur les onglets. Un `carnet.json` d'avant (seul `lastPageId`) doit se relire en un onglet actif : testé en phase 1. Pas de phase filet séparée : le comportement « rouvrir la dernière page » est gardé et retesté dans la phase qui le change. Client : `open`, `show`, `createPage`, montage de `NotebookScreen.vue` (lu) changent ; arbre, recherche, liens, citations, enregistrement inchangés ; validé par `npm run build` et parcours sur 8081 / 5174. | 1, 2 |
| Architecture | Départ : le port `NotebookStore` garde une seule page consultée (`lastPage`), écrite à chaque ouverture par `PUT …/last-page`. Cible : il garde les onglets (`Tabs` : ids dans l'ordre, id actif), écrits en un seul `PUT …/notebook/tabs` ; `GET …/notebook` les rend sans les pages disparues. Le client ne monte qu'un éditeur, celui de l'onglet actif ; activer un onglet relit sa page au serveur. | 1, 2 |
| Documentation | `architecture.md` (dernière page → onglets, route `…/notebook/tabs`, un éditeur pour l'onglet actif, onglet relu et verrouillé à l'activation), `codebase-map.md` (route, `PageTabs.vue`), `project-brief.md` (fonctionnalité Carnet), `DESIGN.md` (barre d'onglets au-dessus de la page), epic `tenir-le-carnet-de-sa-campagne.md:23` et story `retrouver-un-ecran-par-son-adresse.md:21` (« dernière page consultée » → onglet actif ; liste vérifiée par recherche de « dernière page » dans `aidd_docs/`). Story : `status: done`. Plan : `status: implemented`. | 1, 2 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Lire le Carnet ne rejoue pas la partie : `NotebookResource` est construit sur le `CampaignStore` seul (`architecture.md`) | La route des onglets vit dans `NotebookResource`. |
| Une fiche illisible est ignorée, jamais écrasée ; `carnet.json` illisible rend « aucune page » (`JsonFileNotebookStore.java:98-101`) | Un `carnet.json` illisible rend aucun onglet ; un ancien fichier se relit. |
| Page inconnue : `NotFoundException` (404) levée par la ressource, `IllegalArgumentException` → 400 (`architecture.md`) | Un onglet sur une page inconnue rend 404, un onglet actif hors de la liste 400. |
| Avant d'ouvrir une autre page, de créer ou de quitter l'écran, le Carnet attend l'envoi et reste sur place s'il échoue (`architecture.md`) | Changer ou fermer l'onglet actif suit la même règle. |
| Une page s'ouvre verrouillée, sauf celle qu'on vient de créer ; un éditeur par page (`:key`) pour que l'annulation ne passe pas d'une page à l'autre (`architecture.md`) | Activer un onglet rouvre sa page verrouillée, avec un éditeur neuf. |
| Toute nouvelle règle arrive avec son test ; tests d'API par campagne neuve (`testing.md`) | `NotebookApiTest`, `NotebookStoreTest`, `JsonFileNotebookStoreTest`. |
| Client sans tests automatisés ; jamais de test navigateur sur 8080 / 5173 (`testing.md`) | Parcours sur 8081 / 5174, campagnes dans le scratchpad. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Les onglets remplacent la dernière page consultée : le port garde `Tabs(pageIds, activeId)` et l'onglet actif est la page rouverte. `PUT …/last-page` disparaît au profit de `PUT …/notebook/tabs` ; `carnet.json` garde `lastPageId` (l'actif) et gagne `openPageIds`, si bien qu'un ancien fichier se lit en un seul onglet | Une seule vérité, écrite en une requête par geste ; aucune migration de fichier. |
| Les onglets vivent sur le serveur, pas dans le navigateur | « À la réouverture de la campagne » vaut aussi après un redémarrage et depuis un autre navigateur, comme la dernière page aujourd'hui. |
| Tout ce qui ouvre une page (arbre, lien, résultat, page citante, création) ajoute un onglet en fin de barre, ou active le sien s'il existe | Choix du joueur : aucun geste nouveau ; le clic du milieu reste libre pour la story « retrouver un écran par son adresse ». |
| Un seul éditeur monté, celui de l'onglet actif ; activer un onglet relit la page et la rouvre verrouillée | Garde intact le modèle d'enregistrement (un contenu en attente, une page ouverte). Contre : l'annulation et la position de défilement ne survivent pas au changement d'onglet. |
| `GET …/notebook` retire des onglets les pages qui n'existent plus, et l'actif s'il en fait partie ; `PUT …/notebook/tabs` refuse une page inconnue (404), un doublon ou un actif hors de la liste (400) | La corbeille (story suivante) et une fiche effacée à la main ne laissent jamais d'onglet mort au client. |
