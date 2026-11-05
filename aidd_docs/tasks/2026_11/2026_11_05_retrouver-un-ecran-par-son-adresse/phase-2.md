---
status: done
---

# Instruction: le clic du milieu ouvre une page dans un nouvel onglet

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── aidd_docs/
│   ├── backlog/stories/retrouver-un-ecran-par-son-adresse.md ✏️
│   ├── memory/architecture.md ✏️
│   └── tasks/2026_11/2026_11_05_retrouver-un-ecran-par-son-adresse/plan.md ✏️
└── vtt-web/src/
    ├── notebook/
    │   ├── context.ts ✏️
    │   └── pageLink.ts ✏️
    └── components/
        ├── NotebookScreen.vue ✏️
        ├── PageTree.vue ✏️
        ├── PageBranch.vue ✏️
        ├── PageEditor.vue ✏️
        └── PageLinkView.vue ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs 8081 / 5174 ; campagne avec Auberge (lien `[[Bertrand]]`), PNJ > Bertrand | Carnet ouvert sur Auberge | browser |
| Happy path   | Clic du milieu sur Bertrand dans l'arbre | Nouvel onglet du navigateur sur `/campagnes/<id>/carnet/<bertrand>` ; l'onglet d'origine reste sur Auberge | browser |
| Happy path   | Clic du milieu sur le lien Bertrand, page verrouillée puis déverrouillée | Nouvel onglet sur Bertrand ; rien ne change dans l'onglet d'origine (ni sélection, ni collage, ni page) | browser |
| Happy path   | Clic simple sur une ligne, sur un lien verrouillé ; Ctrl+clic sur un lien en écriture | Ouverture dans l'application, comme avant | browser |
| Edge case    | Glisser une ligne de l'arbre sur une autre | Déplacement par événements pointeur, pas de glisser natif de lien | browser |
| Edge case    | Clic simple sur un lien en écriture ; Entrée sur une ligne de l'arbre au clavier | Lien sélectionné, bulle ouverte ; la page s'ouvre | browser |
| Edge case    | Clic du milieu sur un lien mort | Rien ne s'ouvre | browser |
| Teardown     | Tuer 8081 puis 5174 | 8080 / 5173 intacts | system |

## Tasks to do

### `1)` L'arbre porte l'adresse de chaque page

> Une ligne de l'arbre est un lien que le navigateur sait ouvrir ailleurs.

1. `NotebookScreen` passe à `PageTree` l'adresse d'une page (`router.resolve(...).href`) ; `context.ts` la partage (`pageHref`).
2. `PageBranch` : le titre devient `<a :href draggable="false" class="page-link">` ; tout `click` (bouton principal, avec ou sans Ctrl / Maj) : `preventDefault` puis `tree.open`, comme aujourd'hui ; seul le clic du milieu (`auxclick`, non intercepté) est laissé au navigateur. Même allure qu'avant (pas de soulignement, focus visible).

### `2)` Le lien de page porte l'adresse de sa cible

> Le clic du milieu sur un lien ouvre sa cible dans un nouvel onglet.

1. `PageLinkOptions.pageHref` (fonction, comme `pages`) ; `PageEditor` la reçoit en prop et la passe ; `PageLinkView` remplace `#page-<id>` par elle ; lien mort sans `href`.
2. Vérifier que ProseMirror ne prend pas le bouton du milieu en écriture (sélection, collage) ; sinon arrêter `mousedown` au bouton 1 sur le lien.

### `3)` Clôture

> Dire le geste, fermer la story.

1. `architecture.md` : clic du milieu sur l'arbre et les liens ; story `status: done` ; plan `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Clic du milieu sur une ligne : nouvel onglet sur la page ; clic simple, clavier et glisser inchangés ; `npm run build` passe |
| 2 | Clic du milieu sur un lien vivant, verrouillé ou non : nouvel onglet, la page d'origine inchangée ; lien mort : rien |
| 3 | Mémoire, story et plan à jour |
