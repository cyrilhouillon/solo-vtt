---
status: done
---

# Instruction: le Carnet montre ses onglets

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md ✏️
├── aidd_docs/
│   ├── backlog/epics/tenir-le-carnet-de-sa-campagne.md ✏️
│   ├── backlog/stories/garder-plusieurs-pages-ouvertes-en-onglets.md ✏️
│   ├── backlog/stories/retrouver-un-ecran-par-son-adresse.md ✏️
│   ├── tasks/2026_11/2026_11_05_garder-des-pages-en-onglets/plan.md ✏️
│   └── memory/
│       ├── architecture.md ✏️
│       ├── codebase-map.md ✏️
│       └── project-brief.md ✏️
└── vtt-web/src/
    ├── api.ts ✏️
    ├── types.ts ✏️
    └── components/
        ├── NotebookScreen.vue ✏️
        └── PageTabs.vue ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081 / 5174 (`testing.md`) ; campagne avec PNJ > Bertrand, Sessions > Session 1 (qui lie Bertrand), Auberge | Carnet ouvert sur un onglet | browser |
| Happy path   | Ouvrir Bertrand depuis l'arbre, Auberge depuis la recherche, Session 1 depuis « Pages qui renvoient ici » | Quatre pages au plus, une par onglet, ajoutées en fin de barre ; la dernière ouverte est active | browser |
| Happy path   | Cliquer un lien vers Bertrand dans Session 1 | L'onglet Bertrand s'active, aucun nouvel onglet | browser |
| Happy path   | Cliquer un autre onglet, puis Tab jusqu'à un onglet et Entrée | La page de l'onglet s'affiche, verrouillée ; l'arbre la montre courante | browser |
| Happy path   | Renommer Bertrand en « Bertrand le Gris » (arbre ou titre) | Son onglet affiche le nouveau titre | browser |
| Happy path   | Fermer un onglet inactif, puis l'onglet actif | Le premier disparaît sans changer de page ; fermer l'actif active son voisin de droite, sinon de gauche | browser |
| Happy path   | Créer une page | Elle s'ouvre déverrouillée dans un nouvel onglet actif | browser |
| Happy path   | Combat, puis Carnet ; puis Accueil et rouvrir la campagne ; puis redémarrer 8081 et recharger | Mêmes onglets, même ordre, même actif | browser |
| Edge case    | Fermer tous les onglets | « Aucune page ouverte. » à la place de la page ; un clic dans l'arbre rouvre un onglet | browser |
| Edge case    | Taper dans une page déverrouillée puis changer d'onglet ou fermer l'onglet avant une seconde | La frappe est enregistrée avant (relue en revenant) | browser |
| Edge case    | `…/content` bloqué dans les outils du navigateur, taper puis cliquer un autre onglet ou fermer l'actif | Erreur au bandeau, on reste sur la page, rien n'est fermé | browser |
| Edge case    | Dix onglets à 1280px, puis fenêtre à 375px | La barre défile horizontalement, sans défilement de la page | browser |
| Edge case    | `…/notebook/tabs` bloqué, ouvrir une page | Erreur au bandeau ; la page s'ouvre quand même dans son onglet | browser |
| Teardown     | Arrêter 8081 puis 5174 ; effacer le dossier scratchpad | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌─────────────────────────────────────────────────────────────┐
│ (1) Barre : Accueil · Solo VTT · campagne · Combat · thème   │
├──────────────────┬──────────────────────────────────────────┤
│ (2) PAGES        │ (3) Session 1 ✕│ Bertrand ✕│ Auberge ✕│→  │
│ Chercher         │ ═══════════════                          │
│ ▾ PNJ            │ (4) Session 1                          🔒│
│    Bertrand      │ ─────────────────                        │
│ ▾ Sessions       │ Nous retrouvons [Bertrand] à l'auberge…  │
│    Session 1     │                                          │
│   Auberge        │ PAGES QUI RENVOIENT ICI                  │
│ Nouvelle page    │   …                                      │
└──────────────────┴──────────────────────────────────────────┘
```

1. Barre de l'application, inchangée.
2. Panneau Pages, inchangé ; la page de l'onglet actif y est la page courante.
3. Barre d'onglets au-dessus de la page, dans la colonne de la page : un titre par onglet (bouton qui l'active, `aria-current` sur l'actif, souligné d'encre comme les onglets du combat), suivi d'un bouton d'icône « Fermer « … » » ; défile horizontalement au-delà de la largeur.
4. Page de l'onglet actif, inchangée ; sans onglet, « Aucune page ouverte. » en `ink-muted`.

## Tasks to do

### `1)` Appeler la route des onglets

> Le client lit et écrit les onglets de la phase 1.

1. `types.ts` : `NotebookView` remplace `lastPageId` par `openPageIds: string[]` et `activePageId: string | null`.
2. `api.ts` : `setTabs(pageIds, activeId)` sur `PUT …/notebook/tabs` remplace `setLastPage`.

### `2)` La barre d'onglets

> Un composant sans état : titres tirés de la liste des pages, qui suit donc le renommage.

1. `PageTabs.vue` : props `tabIds`, `activeId`, `pages`, `busy` ; émet `activate(pageId)` et `close(pageId)` ; titre lu dans `pages` par id.
2. Style selon le wireframe et `DESIGN.md` (petites capitales, soulignement d'encre de l'actif, pas de laiton) ; `overflow-x: auto`.

### `3)` Ouvrir, activer et fermer dans `NotebookScreen`

> Tout chemin d'ouverture passe par `open` / `show`, qui ajoute ou active l'onglet.

1. État `tabs = ref<string[]>([])` ; `persistTabs()` appelle `api.setTabs(tabs, page?.id ?? null)`, échec au bandeau sans fermer la page.
2. `show(pageId)` : après `saved()` et la lecture de la page, ajouter l'id en fin de `tabs` s'il n'y est pas, `display`, `persistTabs`, puis `readCiting` ; remplace `setLastPage`.
3. `createPage` : ajoute l'onglet de la page créée et le persiste.
4. `closeTab(pageId)` par `call` : onglet inactif → retirer et persister ; onglet actif → `saved()` d'abord (échec : rien ne ferme), retirer, puis `show` du voisin de droite, sinon de gauche, sinon `page = null`, `citing = null` et persister.
5. Montage : `tabs` = `openPageIds` ; page à ouvrir = `activePageId`, sinon le premier onglet, sinon la première page de l'arbre.
6. Template : `PageTabs` en tête de `<main class="page">` quand il y a des onglets ; « Aucune page ouverte. » quand `page` est `null` et que le Carnet a des pages.

### `4)` Valider dans le navigateur

> Le Test Scope, sur 8081 / 5174.

1. `npm run build` dans `vtt-web/`.
2. Dérouler le Test Scope, à 1280px et 375px, thèmes sombre et clair.

### `5)` Documentation et statuts

> La mémoire et le design disent les onglets.

1. `architecture.md` : ouvrir une page l'ajoute en onglet ou active le sien ; un seul éditeur, celui de l'actif, relu et verrouillé à l'activation ; fermer l'actif attend l'envoi et active le voisin.
2. `codebase-map.md` : `components/PageTabs.vue`.
3. `project-brief.md` : onglets dans la fonctionnalité Carnet.
4. `DESIGN.md` : section Carnet, barre d'onglets.
5. Epic `tenir-le-carnet-de-sa-campagne.md:23` et story `retrouver-un-ecran-par-son-adresse.md:21` : « dernière page consultée » → « onglet actif ».
6. Story `status: done` ; plan `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe sans `lastPageId` ni `setLastPage` dans `vtt-web/src` |
| 2 | Chaque onglet montre le titre actuel de sa page, l'actif souligné, et se ferme par son bouton, à la souris comme au clavier |
| 3 | Une page ouverte depuis l'arbre, un lien, un résultat ou une page citante entre en onglet ou active le sien ; fermer l'actif active son voisin ; une frappe en attente part avant tout changement ou fermeture, et un échec garde la page |
| 4 | Onglets et actif reviennent au retour du combat, à la réouverture de la campagne et après redémarrage de 8081 |
| 5 | Mémoire, `DESIGN.md`, story et plan à jour |
