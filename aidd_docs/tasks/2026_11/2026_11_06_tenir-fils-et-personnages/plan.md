---
objective: "Le Carnet de chaque campagne montre, sous les pages ordinaires, une section « Mythic » avec les pages « Fils » et « Personnages », créées à la première ouverture, qui sont des pages ordinaires (onglet, verrou, édition, recherche, liens, sous-pages) sauf qu'elles ne se déplacent ni ne se suppriment ; le cœur ne connaît qu'un marqueur de section générique, jamais Mythic."
status: in-progress
---

# Plan: tenir ses fils et personnages dans la section Mythic du Carnet

Source: aidd_docs/backlog/stories/tenir-ses-fils-et-personnages-dans-la-section-mythic-du-carnet.md (epic `aidd_docs/backlog/epics/jouer-sa-campagne-avec-mythic-gme-depuis-le-carnet.md`, order 3 ; spike `aidd_docs/backlog/spikes/definir-les-points-d-extension-d-un-module-du-carnet.md` : listes en pages, vraie section par module, marqueur générique).

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Le cœur range des pages dans la section d'un module | [`phase-1.md`](./phase-1.md) | none |
| 2   | Mythic crée ses listes Fils et Personnages | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Le Carnet montre la section et la prépare à l'ouverture | [`phase-3.md`](./phase-3.md) | 2 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | Tout repose sur le code lu et le spike résolu (découverte d'un bean de module par `META-INF/beans.xml`, déjà en place pour `MythicResource`). |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Aucune dépendance nouvelle. Une requête de plus à l'ouverture du Carnet (`POST …/mythic/lists`, idempotente). Fiche de page : champ `section` facultatif (`@JsonInclude(NON_NULL)`, comme `deletedWith`) ; une fiche d'avant doit se lire sans section : non vérifié, couvert par un test de la phase 1. | 1, 2, 3 |
| Regression | Moyenne. `Page` gagne un champ : 23 `new Page(` lus dans 8 fichiers (`NotebookStore`, `PageTree`, `JsonFileNotebookStore`, 5 tests). `move` et `delete` gagnent un refus, limité aux racines de section (aucune aujourd'hui). `MythicResource` : `State(int chaos)` est réécrit en entier par `setChaos` (lu) ; il doit garder les ids des listes (lecture-fusion-écriture), et son constructeur gagne un port (appels de `MythicResourceTest`). `mythic-server` est une dépendance de `vtt-server` (lu dans `vtt-server/pom.xml`), donc présent dans ses `@QuarkusTest` : `NotebookApiTest` (16 lignes sur `pages`, dont `empty()`) reste vert parce que la création passe par une route Mythic, jamais par `GET …/notebook`. Pas de phase filet : `NotebookStoreTest`, `JsonFileNotebookStoreTest`, `NotebookApiTest`, `MythicResourceTest` verrouillent l'existant. Client : `canMove`, `buildTree`, menu de ligne et dialogue Déplacer touchés ; `PageSummary.section` se propage à `PageView` et `SearchResult` (`types.ts` l. 66, 71), d'où `section` aussi dans `PageDto` et `SearchResultDto` ; les autres lecteurs de `PageSummary` (`PageLinkList`, `links.ts`, `PageTabs`, `CitingPages`) n'en lisent que l'id et le titre : non vérifié un par un, `vue-tsc` le tranche ; aucun test automatisé (`testing.md`), parcours navigateur sur 5174. | 1, 2, 3 |
| Architecture | Départ : une page n'a qu'un parent ; un module n'a que ses données (`ModuleData`, `CommonModuleData`) et un panneau. Cible : `Page.section` (id d'un module) marque une racine de section, que le module seul crée par le port `solovtt.module.NotebookPages` ; une page est dans une section si sa racine en est une (déduit, jamais stocké sur les sous-pages) ; une racine de section ne se déplace ni ne se supprime. Côté client, `NotebookModule.prepare(campaignId)` est attendu avant la première lecture du Carnet. Le cœur ne nomme toujours pas Mythic. | 1, 3 |
| Documentation | `architecture.md` (marqueur de section, port `NotebookPages`, `prepare`, listes Mythic dans `mythic.json`), `codebase-map.md` (port, adaptateur, route `lists`), `project-brief.md` (section Mythic), `DESIGN.md` (en-tête de section dans l'arbre, menu sans Déplacer ni Supprimer). Story `status: done` ; plan `status: implemented`. | 1, 2, 3 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Seuls les assemblages nomment un module ; ni `vtt-server/src` ni `vtt-web/src` (`architecture.md`) | `grep -ri mythic vtt-server/src vtt-web/src` reste vide : le cœur ne voit qu'un id de section. |
| `IllegalArgumentException` → 400 par l'`ErrorMapper` ; page inconnue → `NotFoundException` (404) levée par la ressource | Déplacer ou supprimer une racine de section : 400 avec la raison. |
| Écriture atomique, fiche illisible jamais écrasée (`JsonFileNotebookStore`) | Le champ `section` passe par la même fiche. |
| Id de module : minuscules, chiffres et tirets (`CampaignStore.checkModuleId`) | Le marqueur de section est un id de module, jamais un chemin. |
| Serveurs de test sur 8081 / 5174, chemins dans le scratchpad (`testing.md`) | Parcours navigateur de la phase 3. |
| Noms de tests en français, pas de mock (`testing.md`) | Faux `NotebookPages` en mémoire dans `mythic-server`. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Seule la racine d'une section porte le marqueur ; une sous-page en est par sa racine | Décision de l'utilisateur : déplacement libre, la page suit son parent ; aucun déplacement ne réécrit de sous-page. |
| Une racine de section ne se déplace ni ne se supprime (400 au serveur, absente du menu et du glisser) ; elle se renomme ; ses sous-pages se déplacent et se suppriment normalement | Décision de l'utilisateur : pas de doublon de liste, pas de liste perdue. |
| Seul le module crée une racine de section, par le port `NotebookPages` ; il garde les ids de ses listes dans ses données (`mythic.json` : `chaos`, `threadsPageId`, `charactersPageId`) | La story 4 retrouve Fils et Personnages même renommées ; le cœur ne connaît aucun rôle de page. |
| Création à la première ouverture par le client : `NotebookModule.prepare` appelle `POST /api/campaigns/{id}/mythic/lists`, idempotent, avant la première lecture du Carnet | Pas d'effet de bord sur `GET …/notebook` ; les tests du Carnet du cœur ne voient pas Mythic. Écart : une campagne jamais ouverte au Carnet n'a pas encore ses listes, ce que la story 4 devra prévoir. |
| Fils et Personnages naissent avec une liste à puces vide | « Une entrée est un élément de liste » : le joueur écrit d'emblée dans une liste. |
