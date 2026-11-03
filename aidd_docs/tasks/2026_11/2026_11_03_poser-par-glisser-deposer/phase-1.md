---
status: done
---

# Instruction: glisser une créature du catalogue et la déposer sur la carte

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/src/
├── App.vue                      ✏️ pose partagée (clic armé, dépôt), réception du glisser
├── components/
│   ├── CatalogPanel.vue         ✏️ le glisser part d'une entrée (événements pointeur)
│   └── MapView.vue              ✏️ relaie survol et dépôt à la carte
└── map/
    └── SceneCanvas.ts           ✏️ position d'écran → position de carte, fantôme du dépôt
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081/5174 (`testing.md`), carte 20 × 20 m | Carte vide, catalogue ouvert | browser |
| Happy path   | Glisser « Guerrier » du catalogue et relâcher au milieu de la carte | Fantôme sous le pointeur pendant le glisser ; jeton posé à l'endroit relâché ; « Guerrier posé. Ctrl+Z pour annuler. » | browser |
| Happy path   | Même geste, Maj tenue | Jeton calé au centre de la case | browser |
| Edge case    | Relâcher à moins d'une case et quart d'un jeton | Jeton collé à son contact (aimant) | browser |
| Edge case    | Relâcher sur un jeton (Maj tenue, même case) | Anneau rouge pendant le survol ; rien n'est posé ; « Trop près de … » | browser |
| Edge case    | Relâcher hors de la carte, ou Échap pendant le glisser | Rien n'est posé, fantôme effacé ; Échap ne vide pas la sélection | browser |
| Edge case    | Combat lancé, déposer une créature | Elle entre dans l'ordre d'initiative ; Ctrl+Z annule la pose | browser |
| Edge case    | Glisser au doigt (émulation tactile, `pointerType: 'touch'`) | Même pose ; la page ne défile pas | browser |
| Edge case    | Fenêtre de moins de 900 px de large, carte sous le pli : glisser une créature vers le bas de la fenêtre, à la souris puis au doigt (émulé) | La page défile ; le fantôme suit le pointeur sur la carte ; le dépôt pose à l'endroit visé | browser |
| Edge case    | Pose au clic armé et au clavier (flèches, Entrée) | Inchangées | browser |
| Teardown     | Tuer les processus sur 8081 et 5174 | 8080 et 5173 intacts | system |

## Tasks to do

### `1)` Convertir une position d'écran en position de carte

> La carte sait viser et dessiner un dépôt depuis `clientX/clientY`, Konva ne recevant pas le pointeur capturé.

1. Dans `SceneCanvas`, factoriser `pointerTarget` : une fonction prend une position de stage et `free`, et rend la position aimantée ou calée, ou `null` hors carte.
2. Paramétrer `drawPreview` et `clearPreview` par le type de créature au lieu de lire `state.placing` (gardes `:640`, `:660`).
3. Ajouter un état de dépôt dans `SceneCanvas` (type, dernière position d'écran) : `dragOver(kind, clientX, clientY)` le met à jour et dessine le fantôme (via `container.getBoundingClientRect()`), `dropTarget(clientX, clientY)` rend la position visée, `endDrag()` l'efface.
4. `redraw` (`:172`) et `onShift` (`:93`) redessinent le fantôme quand un dépôt est en cours : un rendu ou Maj pendant le glisser ne le perdent pas, et Maj change l'aperçu sans attendre un mouvement.
5. Les exposer par `MapView` (`defineExpose`).

### `2)` Faire partir le glisser du catalogue

> Une entrée du catalogue se glisse à la souris comme au doigt.

1. Sur chaque entrée : `pointerdown` (bouton principal, entrée activée) capture le pointeur ; un seuil de quelques pixels distingue le glisser d'un clic.
2. `pointermove`, `pointerup`, `pointercancel` émettent `dragMove(entryId, clientX, clientY)`, `drop(entryId, clientX, clientY)`, `dragCancel`.
3. Le panneau détient l'état du glisser. Pendant un glisser, il écoute Échap sur `window` en capture, l'arrête (`stopImmediatePropagation`, `App.onKey` ne le voit pas), libère la capture, émet `dragCancel` et ignore le `pointerup` suivant. Il expose `cancelDrag()`.
4. `touch-action: none` sur les entrées ; curseur `grabbing` pendant le glisser.
5. Défilement au bord : pendant un glisser, un pointeur (souris ou doigt) près du haut ou du bas de la fenêtre fait défiler la page (`requestAnimationFrame`, vitesse croissante vers le bord), puisque la capture et `touch-action: none` empêchent le défilement natif. À chaque pas, le panneau réémet `dragMove` avec la dernière position : la carte glisse sous un pointeur immobile et le fantôme suit.

### `3)` Poser au dépôt

> Le dépôt pose comme le clic armé, par le même chemin.

1. Extraire de `onBackgroundClick` une fonction `placeEntry(entry, x, y)` (refus « Trop près », jeton en attente, historique, `afterRosterChange`, message).
2. `App` relaie `dragMove` à `mapView.dragOver`, `drop` à `dropTarget` puis `placeEntry` (hors carte, rien), et `drop` comme `dragCancel` à `endDrag`. Maj se lit à la carte (`shiftHeld`), pas dans l'événement.
3. Au début du glisser, terminer ciblage et déplacement au clavier comme `arm` ; refuser le glisser sans carte ou pendant une décision.
4. Si une décision s'ouvre pendant un glisser, `App` appelle `catalog.cancelDrag()`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Pendant le survol, le fantôme suit le pointeur, aimanté ou calé (Maj, même sans bouger), rouge sur une position prise ; il disparaît hors carte et survit à un rendu de la carte. |
| 2 | Le glisser part d'une entrée à la souris et au doigt (émulé) ; un clic sans mouvement n'en lance pas ; près du bord de la fenêtre, la page défile et le fantôme suit ; Échap l'annule sans vider la sélection, et le relâcher qui suit ne pose rien. |
| 3 | Relâcher sur la carte pose la créature avec les mêmes refus, message et Ctrl+Z que la pose au clic ; relâcher hors carte ou Échap ne pose rien ; en combat, la créature entre dans l'ordre. |
