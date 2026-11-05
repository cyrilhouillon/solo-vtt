---
status: pending
---

# Instruction: l'adresse suit l'écran et la page

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── aidd_docs/memory/
│   ├── architecture.md ✏️
│   ├── codebase-map.md ✏️
│   ├── package.md ✏️
│   └── project-brief.md ✏️
└── vtt-web/
    ├── package.json ✏️
    ├── package-lock.json ✏️
    └── src/
        ├── router.ts ✅
        ├── main.ts ✏️
        ├── App.vue ✏️
        └── components/
            └── NotebookScreen.vue ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081 / 5174 (`testing.md`), campagnes dans le scratchpad ; campagne « Test » avec pages Auberge, PNJ > Bertrand | Accueil sur `http://localhost:5174/` | browser |
| Happy path   | Ouvrir « Test » depuis l'accueil | Adresse `/campagnes/<id>/carnet/<page active>` (ou `/campagnes/<id>/carnet` sans onglet) | browser |
| Happy path   | Ouvrir Auberge puis Bertrand dans l'arbre, puis Précédent, Précédent, Suivant | Auberge, page d'arrivée, Auberge ; l'adresse suit ; l'application ne se recharge pas | browser |
| Happy path   | « Combat », puis Précédent ; puis Accueil, puis Précédent | Combat puis Carnet sur la même page ; accueil puis Carnet | browser |
| Happy path   | Recharger sur `/campagnes/<id>/carnet/<bertrand>` et sur `/campagnes/<id>/combat` | Même écran, même page | browser |
| Happy path   | Ouvrir l'adresse de Bertrand dans un nouvel onglet du navigateur alors qu'il n'est pas en onglet | Carnet sur Bertrand, ajouté en fin de barre d'onglets | browser |
| Edge case    | Adresse `/campagnes/<uuid inconnu>/carnet` ; adresse `/n-importe-quoi` | Accueil, adresse `/` | browser |
| Edge case    | Adresse `/campagnes/<id>/carnet/<uuid inconnu>` ; puis une page mise à la corbeille | Carnet sur l'onglet actif (ou « Aucune page ouverte. »), adresse remplacée, pas de bandeau | browser |
| Edge case    | Frappe en attente, serveur 8081 arrêté, Précédent | Bandeau « Page non enregistrée », page et adresse restent ; serveur relancé, Précédent passe | browser |
| Edge case    | Fermer l'onglet actif ; supprimer la page ouverte | Adresse remplacée par la page voisine (ou `/carnet`) ; Précédent ne repasse pas par la page fermée | browser |
| Edge case    | Créer une page ; renommer, déplacer, glisser dans l'arbre, chercher, corbeille | Page créée : nouvelle entrée ; les autres gestes ne changent pas l'adresse et marchent comme avant | browser |
| Teardown     | Tuer 8081 puis 5174 ; effacer les campagnes du scratchpad | 8080 / 5173 intacts | system |

## Tasks to do

### `1)` Le routeur

> Déclarer les écrans comme routes.

1. `npm install vue-router@5.4.0 --save-exact` ; vérifier sur la doc de la version 5 les gardes de composant et l'annulation d'un Précédent.
2. `src/router.ts` : `createWebHistory`, routes `home`, `notebook` (`pageId` facultatif), `combat`, et `/:pathMatch(.*)*` → `/`.
3. Routes de campagne : `beforeEnter` résout la campagne (retenue de l'accueil, sinon `api.campaigns()`) ; inconnue ou illisible : `{ name: 'home' }`. La campagne résolue passe à l'écran (prop de route).
4. `main.ts` : `app.use(router)`.

### `2)` L'application rend l'écran de la route

> `App.vue` remplace son état local par `RouterView`.

1. `RouterView` avec les mêmes `key` qu'aujourd'hui (`notebook-<id>`, `combat-<id>`) : changer de page ne remonte pas le Carnet.
2. `open` de l'accueil, `home`, `combat`, `notebook` des écrans → `router.push`.

### `3)` Le Carnet suit l'adresse

> La page de la route est la page ouverte.

1. Prop `pageId` (de la route) ; au montage, ouvrir `pageId` s'il est lisible, sinon l'onglet actif comme aujourd'hui, puis `router.replace` vers la page affichée.
2. Gestes qui ouvrent une page (arbre, lien, onglet, recherche, citante) : `router.push` ; la page s'ouvre au changement de `pageId`. `createPage` : `router.push` après l'affichage, sans relecture. `closeTabs` (fermeture, suppression) et page illisible : `router.replace`.
3. `onBeforeRouteUpdate` et `onBeforeRouteLeave` : attendre `running` (geste en cours), puis `saved()`, inchangé ; `false` annule. `leave` devient une navigation.
4. Page de l'adresse illisible (montage, Précédent) : son erreur est prise avant `call()`, qui l'écrirait au bandeau ; seule une erreur de l'onglet de repli y va.

### `4)` Mémoire

> Dire les routes.

1. `architecture.md` : routes, entrée poussée ou remplacée, gardes, replis ; `codebase-map.md` : `src/router.ts` ; `package.md` : `vue-router` ; `project-brief.md` : recharger, favori, Précédent / Suivant.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe ; une adresse inconnue devient `/` ; une campagne inconnue mène à l'accueil |
| 2 | Accueil, Carnet et combat ont chacun leur adresse ; recharger rouvre le même écran |
| 3 | Précédent / Suivant repassent par les pages visitées et attendent l'enregistrement (refus : page et adresse restent) ; page inconnue : Carnet comme sans page, sans bandeau ; campagne sans page : onglet actif ; tous les gestes existants du Carnet marchent |
| 4 | Les fichiers de mémoire décrivent les routes telles que codées |
