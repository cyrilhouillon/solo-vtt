---
status: pending
---

# Instruction: caler la grille dans le navigateur

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── vtt-web/src/
│   ├── types.ts                         ✏️ SceneView.gridOffsetX/Y
│   ├── api.ts                           ✏️ alignGrid
│   ├── App.vue                          ✏️ tiroir 'align', bouton du rail, aperçu, validation, history vidé
│   ├── map/
│   │   ├── alignment.ts                 ✅ calage en pixels d'image ↔ dimensions et décalages de la carte
│   │   ├── grid.ts                      ✏️ snapToCell suit le décalage ; colonne et ligne d'un point
│   │   ├── SceneCanvas.ts               ✏️ grille décalée ; aperçu du calage ; glisser la carte décale la grille
│   │   └── backdrop.ts                  ✏️ réutilisé par alignment.ts (mapFromImage)
│   └── components/
│       ├── AppIcon.vue                  ✏️ icône 'align'
│       ├── MapView.vue                  ✏️ annonce de case et curseur clavier avec décalage ; relais du glisser de calage
│       └── GridAlignPanel.vue           ✅ décalages, taille de case, Valider, Annuler
├── DESIGN.md                            ✏️ tiroir Caler la grille, icône du rail
└── aidd_docs/
    ├── memory/codebase-map.md           ✏️ scene/alignment, alignment.ts, GridAlignPanel
    ├── memory/project-brief.md          ✏️ fonctionnalité
    └── backlog/stories/realigner-la-grille-sur-l-image.md  ✏️ status: done
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | serveurs de test 8081 / 5174 (`testing.md`), carte sur une image dont la grille est décalée de quelques pixels, grille masquée, deux créatures posées | carte affichée, grille masquée | browser |
| Happy path   | rail « Caler la grille » | le tiroir montre décalages à 0 et taille de case actuelle ; la grille s'affiche par-dessus l'image | browser |
| Happy path   | glisser la carte à la souris, puis ajuster la taille de case aux flèches du champ | la grille suit en direct ; les champs suivent le glisser ; les créatures ne bougent pas | browser |
| Happy path   | Valider | grille masquée de nouveau (choix gardé) ; barre « Carte … × … » aux nouvelles dimensions ; créatures au même endroit de l'image ; Maj cale au centre des nouvelles cases ; la distance entre les deux créatures suit la nouvelle échelle | browser |
| Happy path   | redémarrer le serveur 8081, recharger la page | même calage ; enregistrer puis charger la carte redonne le même calage | browser |
| Edge case    | modifier puis Annuler (ou Échap) | grille et champs reviennent au calage d'avant ; aucun appel au serveur | browser |
| Edge case    | clavier seul | bouton du rail, champs (flèches : 1 px pour les décalages, 0,1 px pour la case), Valider et Annuler atteignables au Tab | browser |
| Edge case    | combat lancé | bouton du rail éteint, avec la raison au survol ; tiroir ouvert avant : lancer le combat est bloqué tant qu'il est ouvert | browser |
| Edge case    | carte sans image | bouton du rail absent | browser |
| Edge case    | décalage saisi plus grand qu'une case | ramené dans la case (modulo) | browser |
| Edge case    | après validation, Ctrl+Z | rien n'est annulé (historique vidé), aucune créature reposée à une position d'avant le calage | browser |
| Edge case    | curseur clavier de pose sur la carte calée | se cale sur les cases décalées ; l'annonce « Colonne, ligne » compte la case partielle du bord | browser |
| Teardown     | arrêter 8081 puis 5174 ; vider le scratchpad | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌──────┬───────────────────────────────┬────────────────────────────
│ (1)  │ (2) Caler la grille            │ (5) Carte
│ Rail │ ───────────────────────────── │  image + grille d'aperçu
│ [≡]  │ (3) Décalage horizontal [ 12 ] px │  (glisser = décaler)
│ [▦]  │     Décalage vertical   [  7 ] px │
│ [★]  │     Taille de case    [32,5] px │
│ [#]  │     30,8 × 20 cases · 46,2 × 30 m │
│      │ ───────────────────────────── │
│      │ (4) [Valider]  [Annuler]       │
└──────┴───────────────────────────────┴────────────────────────────
```

1. Rail : nouveau bouton « Caler la grille », présent avec une image, éteint en combat.
2. Titre et filet, comme les autres tiroirs.
3. Champs numériques en pixels de l'image ; ligne calculée des cases et dimensions qui en résultent.
4. Valider envoie le calage ; Annuler (ou Échap) ferme sans rien envoyer.
5. Carte : grille d'aperçu forcée visible ; un glisser décale la grille ; pas de pose, de déplacement ni de ciblage pendant le calage.

## Tasks to do

### `1)` Géométrie du calage

> La grille du client part du décalage, partout où elle compte.

1. `types.ts` : `gridOffsetX`, `gridOffsetY` (fraction de case) ; `api.alignGrid(width, height, gridOffsetX, gridOffsetY)` → `SceneView`.
2. `grid.ts` : `snapToCell` cale sur les centres `(décalage + k + 0,5) × case`, borné à la carte ; `cellOf(scene, point)` donne colonne et ligne (1 = case partielle du bord quand le décalage n'est pas nul).
3. `MapView.vue` : l'annonce utilise `cellOf` ; le curseur clavier reste sur `snapToCell`.
4. `SceneCanvas.ts` : lignes de grille à `(décalage + k) × case`.
5. `alignment.ts` : `alignmentOf(scene, imageWidth)` → `{ offsetX, offsetY, cellPixels }` en pixels d'image ; `alignedMap(imageWidth, imageHeight, unit, alignment)` → `{ width, height, gridOffsetX, gridOffsetY }`, décalages ramenés dans [0, 1), dimensions par `mapFromImage`.

### `2)` Aperçu sur la carte

> La grille bouge sur l'image pendant le calage, sans toucher aux créatures.

1. `SceneCanvasState.alignment` (pixels d'image) ou `null` : non nul, la grille d'aperçu se dessine même masquée (contourner le `return` sur `gridVisible`, `SceneCanvas.ts:281`), en coordonnées d'image ramenées à l'écran par la largeur naturelle de l'image ; jetons non glissables, clics de fond ignorés.
2. Geste nouveau (aucun glisser de carte aujourd'hui) : pendant le calage seulement, un glisser sur le sol (événements pointeur du stage) signale le déplacement en pixels d'image (`onAlignDrag`), et le `click tap` du sol (`SceneCanvas.ts:253`) ne pose rien ; `MapView` le relaie.

### `3)` Tiroir de calage

> Le joueur ajuste, valide ou annule, à la souris et au clavier.

1. `GridAlignPanel.vue` : trois champs numériques (pas 1, 1 et 0,1 ; case > 0), ligne des cases et dimensions, Valider, Annuler ; Échap annule ; émet l'aperçu à chaque saisie.
2. `App.vue` : tiroir `'align'` et bouton du rail (icône `align`), seulement avec image, éteint en combat (raison en `title`) ; à l'ouverture, taille naturelle de l'image lue (`imageUrl`) et calage actuel en pixels ; pendant le calage, pose désarmée, ciblage annulé, `movable` faux, lancement du combat bloqué.
3. Valider : `api.alignGrid` par `attempt`, scène remplacée par la réponse, `history` vidé, tiroir fermé, annonce « Grille calée. » ; Annuler : tiroir fermé, aperçu jeté.
4. `AppIcon.vue` : icône `align`.

### `4)` Documentation

> Docs et backlog à jour.

1. `DESIGN.md` : tiroir Caler la grille, icône du rail, aperçu forcé.
2. `codebase-map.md` : `scene/alignment`, `map/alignment.ts`, `GridAlignPanel`.
3. `project-brief.md` : la fonctionnalité.
4. Story `status: done` ; plan `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Sur une carte décalée, Maj et le curseur clavier se calent au centre des cases décalées ; à décalage 0, comme avant ; `npm run build` passe. |
| 2 | Pendant le calage, la grille d'aperçu suit chaque saisie et chaque glisser, même masquée ; les créatures ne bougent pas. |
| 3 | Valider rend la carte aux nouvelles dimensions, créatures au même endroit de l'image ; Annuler et Échap rendent le calage d'avant sans appel ; tout se fait au clavier ; bouton éteint en combat, absent sans image. |
| 4 | `DESIGN.md`, `codebase-map.md`, `project-brief.md` et la story décrivent le calage. |
