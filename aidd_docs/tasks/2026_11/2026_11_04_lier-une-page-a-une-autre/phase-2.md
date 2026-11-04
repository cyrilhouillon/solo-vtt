---
status: done
---

# Instruction: changer le texte d'un lien depuis sa bulle

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md ✏️
├── aidd_docs/
│   ├── backlog/stories/
│   │   └── lier-une-page-du-carnet-a-une-autre.md ✏️
│   ├── memory/
│   │   ├── architecture.md ✏️
│   │   ├── codebase-map.md ✏️
│   │   └── project-brief.md ✏️
│   └── tasks/2026_11/2026_11_04_lier-une-page-a-une-autre/
│       └── plan.md ✏️
└── vtt-web/src/components/
    ├── PageEditor.vue ✏️
    ├── PageLinkBubble.vue ✅
    └── PageLinkView.vue (créé en phase 1, modifié ici)
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081 / 5174 ; Session 5 déverrouillée avec un lien vers Bertrand | Lien « Bertrand » dans le texte | browser |
| Happy path   | Cliquer le lien | Le lien est sélectionné ; la bulle montre « Ouvrir » et « Texte affiché » (vide, « Bertrand » en indication) | browser |
| Happy path   | Saisir « l'aubergiste », Entrée | Le lien affiche « l'aubergiste », le focus revient au texte, la page s'enregistre ; verrouillée et rechargée, elle l'affiche encore | browser |
| Happy path   | Renommer Bertrand | Le lien garde « l'aubergiste » et ouvre toujours Bertrand | browser |
| Happy path   | Vider le champ, Entrée | Le lien reprend le titre de la cible | browser |
| Happy path   | « Ouvrir » dans la bulle ; Ctrl+clic sur un lien | Bertrand s'ouvre (enregistrement attendu d'abord) | browser |
| Happy path   | Au clavier : flèche jusqu'au lien (sélection du nœud) | La bulle s'affiche ; Tab atteint « Ouvrir » et le champ | browser |
| Edge case    | Échap dans le champ | Texte d'avant gardé, bulle fermée, focus au texte | browser |
| Edge case    | Texte de plus de 120 caractères ou blanc | Tronqué à 120 (`maxlength`) ; blanc vaut vide (titre) | browser |
| Edge case    | Lien mort sélectionné | La bulle n'offre pas « Ouvrir » ; le texte affiché reste modifiable | browser |
| Edge case    | Ctrl+Z après un changement de texte | Le texte d'avant revient | browser |
| Teardown     | Arrêter 8081 puis 5174 ; effacer le dossier scratchpad | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌─────────────────────────────────────────────────────────────┐
│ (1) … puis il rejoint [Bertrand] à l'auberge                 │
│           ┌(2)────────────────────────────────┐             │
│           │ Ouvrir · Texte affiché [Bertrand ] │             │
│           └────────────────────────────────────┘            │
└─────────────────────────────────────────────────────────────┘
```

1. Lien sélectionné (page déverrouillée) : marqué comme une sélection.
2. Bulle : « Ouvrir » (absent pour un lien mort) et le champ du texte affiché, vide pour le titre de la cible, le titre en indication.

## Tasks to do

### `1)` La bulle du lien

> Page déverrouillée, le lien sélectionné offre d'ouvrir sa cible et de changer son texte.

1. `PageLinkBubble.vue` : `BubbleMenu` (`@tiptap/vue-3/menus`), `shouldShow` sur une `NodeSelection` de `pageLink` et un éditeur éditable ; bouton « Ouvrir » (cible présente), champ « Texte affiché » (`maxlength` 120, indication = titre) ; Entrée ou sortie du champ pose `label` (blanc → `null`) en une transaction annulable, Échap rend l'ancien ; le focus revient à l'éditeur.
2. `PageEditor.vue` : monte la bulle.

### `2)` Ouvrir en écriture

> Ctrl+clic suit le lien sans passer par la bulle.

1. `PageLinkView.vue` : éditeur éditable, un clic sélectionne le nœud (comportement de ProseMirror) ; Ctrl+clic (Cmd sur macOS) appelle `onOpen`, sauf lien mort.

### `3)` Clore la story

> Documentation et statuts à jour.

1. `DESIGN.md` : bulle du lien, sélection, texte choisi.
2. `architecture.md`, `codebase-map.md` (`PageLinkBubble.vue`), `project-brief.md` (liens du Carnet).
3. Story : `status: done` ; plan : `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Sur une page déverrouillée, un lien sélectionné (clic ou clavier) montre la bulle ; le texte saisi remplace le titre à l'affichage, survit au renommage de la cible et au rechargement ; vide, le titre revient ; Ctrl+Z défait. |
| 2 | Ctrl+clic et « Ouvrir » ouvrent la cible après l'enregistrement ; un lien mort n'ouvre rien. |
| 3 | Mémoire, `DESIGN.md` et statuts à jour ; `npm run build` passe. |
