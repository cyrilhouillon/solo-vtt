---
status: pending
---

# Instruction: insérer un lien de page et le suivre

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md ✏️
├── aidd_docs/memory/
│   ├── architecture.md ✏️
│   ├── codebase-map.md ✏️
│   └── package.md ✏️
└── vtt-web/
    ├── package.json ✏️
    ├── package-lock.json ✏️
    └── src/
        ├── notebook/
        │   ├── pageLink.ts ✅
        │   └── tree.ts ✏️
        └── components/
            ├── NotebookScreen.vue ✏️
            ├── PageEditor.vue ✏️
            ├── PageLinkList.vue ✅
            └── PageLinkView.vue ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081 / 5174 (`testing.md`) ; une campagne avec PNJ > Bertrand, Session 1, Session 5 | Carnet ouvert sur Session 5, déverrouillée | browser |
| Happy path   | Taper « Il croise [[ber », flèche bas si besoin, Entrée | Liste filtrée sans casse ni accents (Bertrand) ; « [[ber » remplacé par un lien « Bertrand », curseur après | browser |
| Happy path   | Taper « [[session 5 » (espace dans la requête) | Session 5 reste proposée | browser |
| Happy path   | Renommer Bertrand en « Bertrand le Rouge » depuis l'arbre, puis le déplacer à la racine | Le lien de Session 5 affiche « Bertrand le Rouge » sans recharger ; le déplacement ne change rien au lien | browser |
| Happy path   | Verrouiller Session 5 ; cliquer le lien ; puis Tab jusqu'au lien et Entrée depuis une autre page qui le porte | Bertrand s'ouvre, notée dernière page consultée | browser |
| Happy path   | Recharger l'onglet, puis redémarrer le serveur de test | Le lien est toujours là, au titre actuel | browser |
| Edge case    | `[[` puis Échap ; `[[zzz` (aucune page) | La liste se ferme, le texte tapé reste ; « Aucune page » sans insertion à Entrée | browser |
| Edge case    | Ctrl+Z juste après l'insertion | Le lien redevient le texte « [[ber » tapé | browser |
| Edge case    | Coller dans une page un lien dont la cible n'existe pas (`<a data-page-link="inconnu">`, ou copié d'une autre campagne) | Lien mort : « page introuvable » barré, éteint ; un clic n'ouvre rien | browser |
| Edge case    | Copier-coller un lien d'une page à une autre du même Carnet | Le lien collé suit la même cible | browser |
| Edge case    | Page déverrouillée : clic sur un lien | Rien ne s'ouvre (la bulle vient en phase 2) | browser |
| Teardown     | Arrêter 8081 puis 5174 ; effacer le dossier scratchpad | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌─────────────────────────────────────────────────────────────┐
│ (1) Titre de la page                                      🔒│
│ ─────────────────                                           │
│ (2) Il croise [[ber▌                                        │
│        ┌(3)──────────────┐                                  │
│        │ ▸ Bertrand  PNJ  │                                  │
│        │   Berthe    Lore │                                  │
│        └──────────────────┘                                 │
│ (4) … puis il rejoint Bertrand à l'auberge (5) page introuvable │
└─────────────────────────────────────────────────────────────┘
```

1. Titre et verrou : inchangés.
2. Corps en écriture : `[[` suivi de la requête.
3. Liste des pages : titres filtrés, page parente en `ink-muted` pour distinguer deux homonymes ; option active marquée ; posée sous le curseur, au-dessus si la fenêtre manque de place.
4. Lien : titre actuel de la cible, même police que le texte, souligné.
5. Lien mort : barré, éteint, sans effet au clic.

## Tasks to do

### `1)` Le nœud lien de page

> Un lien est un nœud en ligne qui ne garde que l'id de sa cible et, plus tard, un texte choisi.

1. `vtt-web` : ajouter `@tiptap/suggestion` `3.32.2` et figer `@floating-ui/dom` `1.8.0` (sans `^`).
2. `notebook/pageLink.ts` : `Node.create` `pageLink` (`inline`, `group: 'inline'`, `atom`, `selectable`), attributs `pageId` et `label` (`null` par défaut) ; `parseHTML` sur `a[data-page-link]`, `renderHTML` en `<a data-page-link data-label>`, `renderText` au texte affiché ; options `pages` (`Ref` de la liste des pages du Carnet) et `onOpen(pageId)`.
3. `PageLinkView.vue` (`VueNodeViewRenderer`) : un `<a href>` (atteignable au Tab quand la page est verrouillée), texte = `label` ?? titre de la cible lu dans `pages` ; cible absente : lien mort (`label` ?? « page introuvable », sans `href`, `aria-disabled`, titre « Cette page n'existe plus »). Éditeur non éditable : clic et Entrée (activation native du lien) appellent `onOpen`, `preventDefault`. Éditeur éditable : pas d'ouverture, et aucun `stopEvent` sur la souris, pour que ProseMirror sélectionne le nœud.
4. `pages` suit la liste remplacée à chaque relecture du Carnet : `PageEditor` la passe en `toRef(props, 'pages')`, jamais le tableau figé à la création.

### `2)` Choisir la cible avec `[[`

> Taper `[[` propose les pages par leur titre.

1. `pageLink.ts` : `Suggestion` sur `char: '[['`, `allowSpaces: true`, `allowedPrefixes: null` ; `items` garde les pages dont le titre contient la requête, sans casse ni accents (comparaison sur `normalize('NFD')` sans diacritiques, en minuscules), triées par `byTitle` de `tree.ts` (à exporter), au plus 8 ; `command` remplace la plage par `pageLink { pageId }` suivi d'une espace, en une seule commande (`insertContentAt`), donc un seul pas d'annulation.
2. `PageLinkList.vue` : liste `role="listbox"`, option active `aria-selected`, flèches haut et bas, Entrée et Tab insèrent, Échap ferme sans toucher au texte ; « Aucune page » quand rien ne correspond ; page parente en `ink-muted` ; position fixe tirée de `clientRect`, au-dessus quand le bas manque.
3. `PageEditor.vue` : props `pages`, émet `open` ; enregistre `pageLink` ; le commentaire « Pas de liens dans cette story » disparaît (`link` de StarterKit reste coupé : les liens web ne sont pas demandés).
4. `NotebookScreen.vue` : passe `notebook.pages` à `PageEditor` et branche `@open` sur `open`.

### `3)` Documentation

> La mémoire et le design décrivent le lien de page.

1. `DESIGN.md` : lien, lien mort, liste `[[`.
2. `architecture.md`, `codebase-map.md`, `package.md` : nœud `pageLink`, titre lu à l'affichage, contrat pour les liens entrants, nouvelles dépendances. Corriger au passage « ses `editorProps` avalent touches, collage et dépôt » : non éditable, ProseMirror n'exécute déjà aucun de ces gestionnaires.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Un lien affiche le titre actuel de sa cible, suit renommage et déplacement sans réécrire la page, s'affiche mort si la cible manque, ouvre la cible au clic et à Entrée sur une page verrouillée ; il survit au rechargement et au redémarrage. |
| 2 | `[[` puis une partie du titre propose les pages correspondantes ; Entrée insère le lien à la place de `[[…` ; Échap rend le texte tapé ; Ctrl+Z défait l'insertion. |
| 3 | `DESIGN.md` et la mémoire décrivent le nœud, son format et les dépendances ; `npm run build` passe. |
