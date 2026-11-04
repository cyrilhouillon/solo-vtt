---
status: pending
---

# Instruction: arriver sur le Carnet et passer au combat

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md ✏️
├── aidd_docs/memory/
│   ├── architecture.md ✏️
│   ├── codebase-map.md ✏️
│   └── project-brief.md ✏️
└── vtt-web/src/
    ├── App.vue ✏️
    ├── api.ts ✏️
    ├── types.ts ✏️
    └── components/
        ├── AppIcon.vue ✏️
        ├── GameScreen.vue ✏️
        └── NotebookScreen.vue ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test de `testing.md` (8081 avec `-Dsolovtt.campaigns.path=<scratchpad>/campagnes`, Vite sur 5174) ; une campagne neuve | Accueil sur 5174 | system |
| Happy path   | Ouvrir la campagne | Le Carnet s'affiche, pas l'écran de jeu | browser |
| Happy path   | Carnet vide : saisir « Session 1 » et créer | La page « Session 1 » s'ouvre et apparaît dans la liste des pages | browser |
| Happy path   | Créer « Bertrand », puis cliquer « Session 1 » dans la liste | « Session 1 » s'ouvre ; la liste est triée par titre | browser |
| Happy path   | Bouton Combat, puis bouton Carnet dans la barre du jeu | L'écran de jeu s'affiche avec sa carte ; le retour rouvre « Session 1 » | browser |
| Happy path   | Accueil, puis rouvrir la campagne ; puis arrêter et relancer le serveur 8081 et rouvrir | Les deux fois, le Carnet rouvre la dernière page consultée | browser |
| Edge case    | Campagne sans page | « Aucune page » et une invitation à créer la première, champ titre en focus | browser |
| Edge case    | Titre vide ou blanc | « Créer » éteint ; titre de plus de 120 caractères impossible à saisir | browser |
| Edge case    | Serveur arrêté pendant la lecture du Carnet | Bandeau d'erreur « Serveur injoignable… », « Fermer » | browser |
| Edge case    | Fenêtre de 375 px | Liste des pages et page en une colonne, sans défilement horizontal | browser |
| Teardown     | Arrêter les processus sur 8081 puis 5174 (jamais 8080 ni 5173), effacer `<scratchpad>/campagnes` | Rien du joueur touché | system |

## Wireframe

```txt
┌────────────────────────────────────────────────────────────────┐
│ (1) ⌂ Accueil · Solo VTT · Nom de campagne ······ [Combat] ☀/☾ │
├────────────────────────────────────────────────────────────────┤
│ (2) bandeau d'erreur (si erreur)                     [Fermer]  │
├─────────────────┬──────────────────────────────────────────────┤
│ (3) Pages       │ (4) Page ouverte                              │
│  ─────────      │  Titre de la page                             │
│  · Bertrand     │  ────────────                                 │
│  · Session 1  ◀ │  (5) corps de la page                         │
│                 │                                               │
│ (6) Nouvelle    │                                               │
│  [titre    ][+] │                                               │
└─────────────────┴──────────────────────────────────────────────┘
Carnet vide : (3) et (4) cèdent la place à
┌────────────────────────────────────────────────────────────────┐
│ (7) « Aucune page. Créer la première page du Carnet. »         │
│      [titre                               ] [Créer]            │
└────────────────────────────────────────────────────────────────┘
```

1. Barre : la même que l'accueil et le jeu ; Accueil ramène à l'accueil, Combat ouvre l'écran de jeu de la campagne.
2. Erreur : vermillon sous la barre, comme l'accueil.
3. Pages : liste plate triée par titre, la page ouverte marquée (`aria-current`) ; l'arbre la remplacera.
4. Page ouverte : son titre en tête, en Alegreya SC.
5. Corps : vide dans cette phase, l'éditeur de la phase 3 y prend place.
6. Nouvelle page : champ titre et bouton, Entrée crée.
7. Carnet vide : une invitation centrée, qui crée la première page.

## Tasks to do

### `1)` Le Carnet côté client

> Le client lit le Carnet, crée une page, l'ouvre et note la dernière page consultée.

1. `types.ts` : `PageSummary`, `PageView`, `NotebookView`, alignés sur les DTO de la phase 1.
2. `api.ts` (`campaignApi`) : `notebook()`, `createPage(title)`, `page(id)`, `savePageContent(id, content, keepalive?)`, `setLastPage(id)`.

### `2)` L'écran Carnet

> Ouvrir une campagne mène au Carnet, sur sa dernière page ; un Carnet vide invite à créer la première.

1. `NotebookScreen.vue` (prop `campaign`, émet `home` et `combat`) : barre, erreur, liste des pages, page ouverte, formulaire « Nouvelle page » (120 caractères au plus, comme le serveur), état vide, selon le Wireframe.
2. Au montage : `notebook()`, puis ouvrir `lastPageId`, sinon la première page, sinon l'état vide ; ouvrir une page appelle `page(id)` puis `setLastPage(id)`.
3. Créer une page l'ouvre et relit la liste.
4. `AppIcon.vue` : icônes `notebook` et `combat`, même trait (1.75, grille 24).

### `3)` Passer du Carnet au combat

> Carnet et combat sont deux écrans de la campagne ; on va de l'un à l'autre.

1. `App.vue` : `screen: 'notebook' | 'combat'`, `'notebook'` à l'ouverture d'une campagne ; `GameScreen` et `NotebookScreen` montés tour à tour (`v-if`), gardant `:key` = id de campagne.
2. `GameScreen.vue` : bouton Carnet dans la barre, après Accueil, qui émet `notebook`.

### `4)` Mémoire et design

> La doc décrit le Carnet comme écran d'ouverture de la campagne.

1. `DESIGN.md` : écran Carnet (layout, liste des pages, état vide, bouton Combat, bouton Carnet de la barre du jeu, sous 900 px en une colonne).
2. `architecture.md` : ouvrir une campagne mène au Carnet ; Carnet et combat montés tour à tour, chacun relit le serveur.
3. `codebase-map.md` : `NotebookScreen`, `App.vue` à trois écrans ; `project-brief.md` : la fonctionnalité Carnet.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe (`vue-tsc`) ; les appels visent `/api/campaigns/{id}/notebook…`. |
| 2 | Ouvrir une campagne affiche le Carnet ; vide, il invite à créer et la création ouvre la page ; sinon il rouvre la dernière page consultée, aussi après redémarrage du serveur 8081. |
| 3 | Combat ouvre l'écran de jeu avec sa carte ; Carnet y ramène sur la même page. |
| 4 | `DESIGN.md` et la mémoire décrivent l'écran et sa navigation. |
