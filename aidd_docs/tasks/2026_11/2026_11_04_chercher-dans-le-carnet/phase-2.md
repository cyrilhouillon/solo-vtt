---
status: done
---

# Instruction: le Carnet montre les résultats

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md ✏️
├── aidd_docs/
│   ├── backlog/stories/retrouver-une-page-du-carnet-par-la-recherche.md ✏️
│   └── memory/
│       ├── architecture.md ✏️
│       ├── codebase-map.md ✏️
│       └── project-brief.md ✏️
└── vtt-web/src/
    ├── api.ts ✏️
    ├── types.ts ✏️
    └── components/
        ├── NotebookScreen.vue ✏️
        └── SearchResults.vue ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081 / 5174 (`testing.md`) ; campagne avec PNJ > Évêque Aldric, Sessions > Session 1, Session 2, textes écrits dans l'application | Carnet ouvert | browser |
| Happy path   | Taper « eveque » dans la recherche | L'arbre laisse place aux résultats : Évêque Aldric puis Session 1, titre et extrait, « évêque » surligné | browser |
| Happy path   | Cliquer Session 1, puis Tab jusqu'à un autre résultat et Entrée | La page s'ouvre, notée dernière consultée ; les résultats restent | browser |
| Happy path   | Écrire « gargouille » dans Session 2 déverrouillée, chercher « gargouille » aussitôt | Session 2 trouvée (l'envoi en attente part avant la recherche) | browser |
| Happy path   | Échap dans le champ, ou « Effacer » | Champ vide, l'arbre revient tel qu'il était déplié | browser |
| Edge case    | Chercher « dragon » | « Aucune page ne contient « dragon ». » | browser |
| Edge case    | Taper vite « e », « ev », « eve » | Seuls les résultats de « eve » restent affichés (réponses périmées ignorées) | browser |
| Edge case    | Ouvrir une autre campagne et chercher « eveque » | Aucun résultat de la première | browser |
| Edge case    | L'appel `…/search` échoue (route bloquée dans les outils du navigateur) | Erreur dans le bandeau ; la page ouverte et le champ restent | browser |
| Teardown     | Arrêter 8081 puis 5174 ; effacer le dossier scratchpad | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌─────────────────────────────────────────────────────────────┐
│ (1) Barre : Accueil · Solo VTT · campagne · Combat · thème   │
├──────────────────┬──────────────────────────────────────────┤
│ (2) PAGES        │ (5) Session 1                          🔒│
│ ┌──────────────┐ │ ─────────────────                        │
│ │eveque      ✕ │ │ Nous rencontrons l'évêque à l'auberge…   │
│ └──────────────┘ │                                          │
│ (3) 2 pages      │ ─────────────────                        │
│  Évêque Aldric   │ PAGES QUI RENVOIENT ICI                  │
│   Il garde la…   │   …                                      │
│  Session 1       │                                          │
│   …rencontrons   │                                          │
│   l'[évêque] à…  │                                          │
│ (4) Nouvelle page│                                          │
└──────────────────┴──────────────────────────────────────────┘
```

1. Barre de l'application, inchangée.
2. Panneau Pages : sous son titre, un champ de recherche avec un bouton « Effacer » quand il n'est pas vide.
3. Résultats, à la place de l'arbre tant que le champ n'est pas vide : leur nombre, puis chaque page (titre, extrait avec les mots surlignés), un lien qui l'ouvre ; vide, « Aucune page ne contient « … ». ».
4. Création de page, inchangée.
5. Page ouverte, inchangée.

## Tasks to do

### `1)` Appeler la recherche

> Le client lit les résultats de la phase 1.

1. `types.ts` : `SearchResult` (`id`, `title`, `parentId`, `excerpt: { text: string; match: boolean }[]`).
2. `api.ts`, dans `campaignApi` : `searchPages(query)` → `SearchResult[]` sur `…/notebook/search?q=` (`encodeURIComponent`).

### `2)` Chercher depuis le panneau Pages

> Le champ, la liste et l'ouverture ; le client ne replie rien, il affiche les morceaux reçus.

1. Créer `SearchResults.vue` : props `results`, `query` ; nombre de pages, puis par résultat un bouton (titre ; extrait, chaque morceau `match` en `<mark>`) qui émet `open` ; vide : « Aucune page ne contient « … ». ».
2. `NotebookScreen.vue` : champ `type="search"` sous « Pages » ; 250 ms après la dernière frappe, `await saved()` puis `searchPages` ; si `saved()` rend `false`, la recherche ne part pas (l'erreur d'enregistrement est déjà au bandeau) et repart à la frappe suivante ; un compteur ignore les réponses périmées ; échec → bandeau d'erreur. Champ non vide : `SearchResults` à la place de `PageTree` ; Échap ou « Effacer » vide le champ. `open` inchangé, la recherche reste.
3. Styler selon `DESIGN.md` (champ comme ceux du panneau : étendre la règle `input[type='text']` de `NotebookScreen.vue` à `input[type='search']` ; titre à l'allure d'un lien de page ; extrait en `ink-muted` ; `<mark>` sur fond `brass` atténué).

### `3)` Documenter et clore

> Mémoire, design et backlog suivent.

1. `DESIGN.md` : bloc « Recherche du Carnet ».
2. `architecture.md` : résultats à la place de l'arbre, envoi en attente avant la recherche, réponses périmées ignorées, surlignage reçu du serveur.
3. `codebase-map.md` : `components/SearchResults.vue`.
4. `project-brief.md` : la recherche du Carnet.
5. Story `status: done`, plan `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1    | `npm run build` passe ; l'appel rend la liste du serveur. |
| 2    | Taper « eveque » liste Évêque Aldric puis Session 1 avec extrait surligné ; un clic ou Entrée ouvre la page sans fermer les résultats ; une frappe pas encore enregistrée est trouvée ; « dragon » affiche « Aucune page ne contient « dragon ». » ; Échap rend l'arbre ; une autre campagne ne montre rien de la première. |
| 3    | `DESIGN.md`, `architecture.md`, `codebase-map.md`, `project-brief.md` décrivent la recherche ; story `done`, plan `implemented`. |
