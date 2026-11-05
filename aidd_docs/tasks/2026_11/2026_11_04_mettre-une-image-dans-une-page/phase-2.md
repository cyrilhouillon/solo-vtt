---
status: done
---

# Instruction: déposer ou coller une image dans le texte

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md ✏️
├── aidd_docs/
│   ├── backlog/stories/mettre-une-image-dans-une-page-du-carnet.md ✏️
│   ├── memory/architecture.md ✏️
│   └── tasks/2026_11/2026_11_04_mettre-une-image-dans-une-page/plan.md ✏️
└── vtt-web/src/components/
    ├── NotebookScreen.vue ✏️
    └── PageEditor.vue ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081 / 5174, page déverrouillée à trois paragraphes | Page éditable | browser |
| Happy path   | Déposer un PNG de l'explorateur entre le premier et le deuxième paragraphe | L'image s'insère au point de dépôt ; l'application reste à l'écran | browser |
| Happy path   | Copier une image (capture d'écran) et Ctrl+V dans le texte | L'image s'insère au curseur | browser |
| Happy path   | Ctrl+Z après un dépôt | L'image disparaît en un pas | browser |
| Happy path   | Coller du texte ordinaire ; glisser un mot dans le texte | Comportement inchangé | browser |
| Edge case    | Déposer un GIF ou plus de 50 Mo | Raison dans le bandeau, rien d'inséré, l'application reste à l'écran | browser |
| Edge case    | Déposer plusieurs images à la fois | Chacune s'insère, dans l'ordre, au point de dépôt | browser |
| Edge case    | Page verrouillée : déposer ou coller une image | Rien d'inséré, l'application reste à l'écran | browser |
| Edge case    | Lâcher un fichier dans la marge de la page | L'application reste à l'écran, rien d'inséré | browser |
| Teardown     | Arrêter 8081 puis 5174 | Ports libérés ; 8080 et 5173 intacts | system |

## Tasks to do

### `1)` Prendre en main dépôt et collage de fichiers

> Un fichier déposé ou collé sur le texte devient une image de la page, jamais un onglet du navigateur.

1. `PageEditor.vue`, `handleDrop` : si le `dataTransfer` porte des fichiers, empêcher le comportement par défaut ; verrouillée, rien d'autre ; sinon position tirée de `view.posAtCoords`, puis `insertImage` pour chaque fichier à cette position, la raison du premier refus remontée comme au bouton.
2. `handlePaste` : si le presse-papiers porte des fichiers image, même traitement au curseur ; un collage sans fichier garde le comportement actuel.
3. `NotebookScreen.vue` : un fichier lâché hors de l'éditeur, sur l'article de la page, n'ouvre pas le fichier à la place de l'application (`dragover` / `drop` empêchés quand le `dataTransfer` porte des fichiers).

### `2)` Documenter et clore

> La mémoire dit qu'une image se dépose et se colle ; la story est faite.

1. `architecture.md` et `DESIGN.md` : dépôt et collage d'une image en écriture.
2. Story `status: done`, plan `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1    | Une image déposée ou collée s'insère au point visé, se défait en un Ctrl+Z, est refusée avec la raison si elle n'est pas PNG / JPEG / WebP ou dépasse 50 Mo ; verrouillée, rien ne s'insère ; jamais l'application ne cède la place au fichier. |
| 2    | `architecture.md` et `DESIGN.md` décrivent dépôt et collage ; story `done`, plan `implemented`. |
