---
objective: "Les pages du Carnet forment un arbre que le joueur construit : il crée une page à la racine ou sous une autre, la renomme, la déplace avec ses sous-pages (glisser-déposer ou menu), sans jamais créer de boucle, ouvre et replie les branches, et retrouve l'arbre après un redémarrage du serveur."
status: implemented
---

# Plan: ranger les pages du Carnet dans un arbre

Source: aidd_docs/backlog/stories/ranger-les-pages-du-carnet-dans-un-arbre.md (epic `aidd_docs/backlog/epics/tenir-le-carnet-de-sa-campagne.md`) ; choix du joueur : déplacer par glisser-déposer et par menu, sœurs triées par titre en ordre numérique, renommer depuis l'arbre et depuis le titre de la page déverrouillée.

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Garder l'arbre des pages sur le serveur | [`phase-1.md`](./phase-1.md) | none |
| 2   | Afficher et ranger l'arbre dans le Carnet | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Déplacer une page en la glissant dans l'arbre | [`phase-3.md`](./phase-3.md) | 2 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/Collator/Collator | `numeric: true` compare les suites de chiffres par leur valeur (« Session 2 » avant « Session 10 ») ; `sensitivity: 'base'` ignore casse et accents. |
| https://developer.mozilla.org/en-US/docs/Web/API/Document/elementFromPoint | Pendant une capture de pointeur, `pointermove` vise l'élément capturant : la ligne survolée se retrouve par `document.elementFromPoint(x, y)`. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Les fiches `carnet/<id>.json` gagnent `parentId` ; une fiche écrite avant (sans ce champ) se lit à la racine, car Jackson laisse `null` un champ absent du record (vérifié : `JsonFileNotebookStore` utilise un `ObjectMapper` sans `FAIL_ON_MISSING_CREATOR_PROPERTIES` ; verrouillé par un test d'adaptateur). Aucune nouvelle dépendance. | 1 |
| Regression | Moyen. Serveur : `Page` gagne un champ et `NotebookStore.create` un parent ; `NotebookStoreTest`, `JsonFileNotebookStoreTest`, `CampaignStoreTest`, `FileCampaignStoreTest` et `NotebookApiTest` (lus) construisent des `Page` ou créent des pages et doivent repasser. Client : `NotebookScreen.vue` (lu) remplace sa liste plate par l'arbre et son titre devient un champ une fois déverrouillé ; pas de tests automatisés, parcours dans le navigateur. Pas de phase filet : les tests serveur existants le sont. | 1, 2 |
| Architecture | Départ : une liste plate de pages (`Page` : id, titre, contenu), triée par titre, sans hiérarchie (`architecture.md`, « Le Carnet ne montre encore qu'une liste plate… que l'arbre remplacera »). Cible : chaque page porte `parentId` (`null` à la racine) ; l'arbre n'est rien d'autre que ces liens, donc déplacer une page n'écrit que sa fiche et ses sous-pages suivent. Le serveur refuse une boucle et rend à la racine une page dont le parent est illisible ; le client construit l'arbre depuis la liste plate. | 1, 2 |
| Documentation | `architecture.md` (`parentId`, refus de boucle, parent illisible → racine, routes `title` et `parent`, arbre côté client, glisser-déposer souris seulement), `codebase-map.md` (`PageTree`, routes, `notebook/tree.ts`, `PageTree.vue`, `MovePageDialog.vue`), `project-brief.md` (fonctionnalité Carnet), `DESIGN.md` (arbre, menu de ligne, dialogue Déplacer, titre éditable, glisser). Story : `status: done`. Plan : `status: implemented`. | 1, 2, 3 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Port et adaptateur dans `persistence/`, mémoire en test, écriture atomique, fiche illisible jamais écrasée (`architecture.md`) | `rename` et `move` passent par `NotebookStore` et réécrivent une seule fiche. |
| Une page inconnue se lève en `NotFoundException` dans la ressource, car `ErrorMapper` rend 400 pour une `IllegalArgumentException` (`architecture.md`) | Page ou parent inconnu : 404 ; boucle : 400. |
| Tests `@QuarkusTest` + RestAssured, une campagne neuve par test, noms en français (`testing.md`) | Nouveaux tests d'API et d'adaptateur. |
| Ne jamais tester sur 8080/5173 ; serveur de test avec `-Dsolovtt.campaigns.path=<scratchpad>/campagnes` (`testing.md`) | Les parcours navigateur créent et déplacent des pages. |
| Le verrou est purement client ; verrouillée ou non, la page garde la même mise en page (`architecture.md`, `DESIGN.md`) | Le titre éditable une fois déverrouillé garde la police, la taille et la place du titre. |
| Toute action a son chemin clavier (pose au catalogue, déplacement D + flèches, `architecture.md`) | Le menu « Déplacer sous… » est le chemin clavier et tactile du glisser-déposer. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| L'arbre est un `parentId` par page, sans ordre stocké ; les sœurs sont triées par titre côté client, en ordre numérique (`Intl.Collator`, `numeric`) | Déplacer une branche n'écrit qu'une fiche ; aucune position à tenir. Choix du joueur : pas d'ordre manuel. |
| Le refus de boucle vit dans le port, sous son verrou (`synchronized`), avec un utilitaire commun `PageTree` | Deux onglets peuvent déplacer en même temps : vérifier puis écrire hors verrou laisserait passer une boucle. |
| Une page dont le parent manque (fiche illisible) ou qui forme une boucle (fiche éditée à la main) est rendue à la racine par `GET …/notebook`, sans réécrire sa fiche | Aucune page ne disparaît de l'arbre, et une fiche n'est jamais réécrite sans geste du joueur. |
| Branches ouvertes gardées en mémoire du Carnet seulement ; à l'ouverture d'une page, ses ancêtres s'ouvrent | La story ne demande pas de les garder. Contrepartie : au retour du combat, seul le chemin de la page ouverte est déplié. |
| Arbre en listes imbriquées, un bouton de dépli par branche (`aria-expanded`), pas le motif ARIA `tree` (focus itinérant, flèches) | Accessible au clavier par Tab avec peu de code ; le motif complet reste possible plus tard. |
| Glisser-déposer à la souris et au stylet seulement ; au toucher, le menu « Déplacer sous… » | Au doigt, glisser une ligne entrerait en conflit avec le défilement de l'arbre et de la page (`touch-action`). |
