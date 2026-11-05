---
objective: "Chaque écran d'une campagne a son adresse (accueil, Carnet sur une page, combat) : recharger, un favori ou Précédent / Suivant y ramènent, en attendant l'enregistrement de la page en cours, et le clic du milieu sur une page de l'arbre ou un lien de page l'ouvre dans un nouvel onglet du navigateur."
status: in-progress
---

# Plan: retrouver un écran par son adresse

Source: aidd_docs/backlog/stories/retrouver-un-ecran-par-son-adresse.md (epic `aidd_docs/backlog/epics/tenir-le-carnet-de-sa-campagne.md`, `status: ready`, seule story non faite).

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | L'adresse suit l'écran et la page | [`phase-1.md`](./phase-1.md) | none |
| 2   | Le clic du milieu ouvre une page dans un nouvel onglet | [`phase-2.md`](./phase-2.md) | 1 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://www.npmjs.com/package/vue-router (`npm view vue-router`, 2026-11-05) | `latest` = 5.4.0 (2026-10-07) ; seul pair obligatoire : `vue` `^3.5.34` (projet : 3.5.44) ; `vite`, `pinia`, `@pinia/colada`, `@vue/compiler-sfc` optionnels. Ses 16 dépendances servent au routage par fichiers (plugin de build), non utilisé ici. La branche 4 (4.6.4, 2025-12) n'évolue plus. |
| https://router.vuejs.org/guide/advanced/navigation-guards.html | `onBeforeRouteLeave` / `onBeforeRouteUpdate` acceptent une garde asynchrone ; rendre `false` annule la navigation, y compris un Précédent du navigateur, dont le routeur remet l'adresse. À confirmer sur la doc de la version 5 en phase 1. |
| https://router.vuejs.org/guide/essentials/history-mode.html | `createWebHistory` demande que le serveur rende `index.html` pour toute adresse : Vite le fait en dev. |
| https://developer.mozilla.org/docs/Web/API/Element/auxclick_event | Le clic du milieu émet `auxclick`, pas `click` : sur un `<a href>`, le navigateur ouvre l'onglet de lui-même. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Nouvelle dépendance `vue-router` 5.4.0, version figée (`package.md`). Vite sert `index.html` pour toute adresse inconnue en dev (`appType: 'spa'` par défaut ; `vite.config.ts` lu, sans `appType`) ; le serveur de test 5174 aussi (`createServer` sans `appType`). Aucun service du client en production (`vtt-server` ne sert pas `vtt-web` : ni Quinoa ni `META-INF/resources`, cherché). Non vérifié dans le navigateur : à observer en phase 1 (recharger sur `/campagnes/<id>/carnet/<page>`). Ids de page et de campagne : UUID, sans point, donc jamais pris pour un fichier par Vite. | 1 |
| Regression | Moyenne à haute, client seul. `App.vue` (lu) monte l'écran d'un état local (`campaign`, `screen`) : il devient un `RouterView`. `NotebookScreen.vue` (lu, 875 lignes) : `show`, `open`, `closeTabs`, `createPage`, `confirmDelete`, `leave` changent la page active, chacun passe par le routeur ; le montage choisit la page de l'adresse avant l'onglet actif ; `call()` écrit toute erreur au bandeau (`NotebookScreen.vue:169-180`), à contourner pour une page d'adresse illisible. `PageBranch.vue` (lu) : le titre d'une ligne passe de `<button>` à `<a href>`, que `PageTree.onTreePointerDown` (lu, sélecteur `.page-link`, `button !== 0` ignoré) arme toujours ; un lien se glisse nativement, d'où `draggable="false"`. `PageLinkView.vue` (lu) : `href="#page-…"` devient l'adresse de la page ; le clic du milieu d'un nœud atomique en écriture n'a pas été observé (phase 2). `HomeScreen.vue` et `GameScreen.vue` (lus) : leurs `emit` restent, `App.vue` les traduit en navigation. Pas de tests automatisés client (`testing.md`) ; pas de phase filet : `npm run build` et parcours sur 8081 / 5174, qui rejouent aussi les gestes existants. | 1, 2 |
| Architecture | Départ : l'écran est un état de `App.vue`, l'adresse reste `/`. Cible : `vue-router` (`src/router.ts`) porte les routes ; `App.vue` rend l'écran de la route ; la campagne d'une route est résolue avant d'entrer ; le Carnet garde sa sortie et ses changements de page par les gardes du routeur ; la page de l'adresse est la page ouverte. Le serveur ne change pas. | 1 |
| Documentation | `architecture.md` (routes, garde, Précédent refusé, replis, clic du milieu), `codebase-map.md` (`src/router.ts`), `package.md` (`vue-router`), `project-brief.md` (recharger, favori, Précédent / Suivant). Story : `status: done`. Plan : `status: implemented`. | 1, 2 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Avant d'ouvrir une autre page, de créer ou de quitter l'écran, le Carnet attend l'envoi et reste sur place s'il échoue (`architecture.md`) | Précédent / Suivant passent par la même attente (`saved()`) dans les gardes du routeur. |
| Chaque écran relit la campagne au serveur en se montant ; rien n'est gardé côté client entre deux écrans (`architecture.md`) | Recharger ou ouvrir une adresse ne demande aucun état client. |
| Toute ouverture d'une page l'ajoute en onglet, ou active le sien, puis envoie tous les onglets (`architecture.md`) | Une adresse vers une page, ou un Précédent vers une page dont l'onglet est fermé, la rouvre en onglet. |
| Les dépendances de `vtt-web` restent confinées, aux versions figées (`package.md`) | `vue-router` à version exacte, sans `^`. |
| Client sans tests automatisés ; jamais de test navigateur sur 8080 / 5173 (`testing.md`) | Parcours sur 8081 / 5174, campagnes dans le scratchpad. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| `vue-router` 5.4.0, `createWebHistory`, routes déclarées dans `src/router.ts`, sans routage par fichiers | Le routeur officiel de Vue : gardes asynchrones, Précédent annulé, routes à venir sans rien réécrire. Branche 5 plutôt que 4, qui n'évolue plus. |
| Routes en français : `/` (`home`), `/campagnes/:campaignId/carnet/:pageId?` (`notebook`), `/campagnes/:campaignId/combat` (`combat`) ; toute autre adresse redirige vers `/` | Elles se lisent dans la barre du navigateur ; les ids restent les UUID du serveur, sans slug de titre qu'un renommage casserait. |
| L'adresse fait foi : un geste qui ouvre une page navigue (`router.push`), et le Carnet ouvre la page quand sa route change ; le montage, la fermeture d'un onglet et une suppression qui change la page active remplacent l'entrée (`router.replace`) | Un seul chemin pour un geste et pour Précédent / Suivant ; l'historique repasse par les pages visitées, pas par les conséquences d'un geste. |
| La campagne d'une route se résout avant d'entrer (`beforeEnter`), dans `GET /api/campaigns`, sans nouvelle route serveur ; inconnue ou illisible : redirection vers `/`, sans bandeau. Page inconnue (ou à la corbeille) : le Carnet s'ouvre comme sans page et l'adresse est remplacée, sans bandeau | Critères de la story (« ne casse rien ») ; la liste est déjà lue par l'accueil. |
| Le clic du milieu passe par un vrai `<a href>` (adresse tirée de `router.resolve`) sur une ligne de l'arbre et un lien de page ; tout `click` au bouton principal, modifié ou non, reste un geste de l'application, comme aujourd'hui (pas `RouterLink`, qui laisse Ctrl+clic au navigateur) | Comportement natif, sans gestionnaire `auxclick` ; aucun geste existant ne change. Résultats de recherche, pages citantes et onglets restent des boutons : la story ne les demande pas. |
