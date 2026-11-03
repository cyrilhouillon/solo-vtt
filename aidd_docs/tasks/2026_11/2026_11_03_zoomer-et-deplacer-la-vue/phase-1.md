---
status: in-progress
---

# Instruction: zoomer la vue

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── vtt-web/src/
│   ├── App.vue                          ✏️ touches + − 0 (calage compris) ; vue remise à la carte entière à la création et au chargement
│   ├── map/
│   │   ├── view.ts                      ✅ vue (zoom, centre en fraction) : échelle, origine, zoom autour d'un point, bornes, suivi d'un point
│   │   └── SceneCanvas.ts               ✏️ échelle et origine tirées de la vue ; molette et pincement ; API zoomIn/zoomOut/fit/reveal
│   └── components/
│       └── MapView.vue                  ✏️ boutons de zoom sur la carte ; vue suit le curseur clavier ; touch-action: pan-x pan-y
├── DESIGN.md                            ✏️ contrôles de zoom
└── aidd_docs/memory/
    ├── architecture.md                  ✏️ vue client, non gardée
    └── codebase-map.md                  ✏️ map/view.ts
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | serveurs de test 8081 / 5174 (`testing.md`) ; carte avec image, grille visible, trois créatures | carte entière affichée, comme avant | browser |
| Happy path   | molette vers l'avant sur une créature | la carte grossit autour du pointeur, la créature reste sous le pointeur ; la page ne défile pas | browser |
| Happy path   | pincement au pavé tactile (Ctrl + molette) | même zoom ; le navigateur ne zoome pas la page | browser |
| Happy path   | boutons zoom avant / arrière / « Ajuster », puis + − 0 au clavier | zoom par paliers autour du centre ; « Ajuster » et 0 rendent la carte entière | browser |
| Happy path   | zoomé : poser au clic, glisser un jeton, glisser depuis le catalogue, Maj, mesurer, cibler | chaque position et distance est celle sous le pointeur ; mêmes résultats qu'à ×1 | browser |
| Happy path   | zoomé, en combat : déplacer la créature active | cercle de vitesse restante et refus au-delà, à la bonne échelle | browser |
| Edge case    | zoom arrière au-delà de la carte entière ; zoom avant au-delà de ×8 | bloqué aux bornes | browser |
| Edge case    | zoomé, D puis flèches jusqu'au bord de la vue | la vue suit le curseur de case | browser |
| Edge case    | zoomé, caler la grille, valider | même zone de l'image regardée après validation | browser |
| Edge case    | zoomé, « Nouvelle carte » puis charger une carte enregistrée | chaque carte s'affiche entière | browser |
| Edge case    | zoomé, redimensionner la fenêtre | même centre, même zoom | browser |
| Edge case    | molette pendant le glisser d'un jeton | ignorée, le glisser continue | browser |
| Edge case    | deux doigts (écran tactile ou émulation DevTools) | pincer zoome, déplacer les deux doigts déplace la vue | browser |
| Teardown     | arrêter 8081 puis 5174 ; vider le scratchpad | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌───────────────────────────────────────────┐
│ (1) Carte (vue zoomée)              ┌───┐ │
│                                     │(2)│ │
│                                     │ + │ │
│                                     │ − │ │
│                                     │ ⤢ │ │
│                                     └───┘ │
└───────────────────────────────────────────┘
```

1. Carte : la vue zoomée, au-dessus de laquelle roulent toujours les dés.
2. Contrôles de zoom, coin haut droit : zoom avant, zoom arrière, « Ajuster » (carte entière) ; boutons nommés, au Tab.

## Tasks to do

### `1)` Géométrie de la vue

> Une vue pure, sans Vue ni Konva, que la carte applique.

1. `map/view.ts` : `View = { zoom, cx, cy }` (centre en fraction de carte), `FIT_VIEW`, `MAX_ZOOM = 8`.
2. `layout(view, stage, map, margin)` → `{ pixelsPerUnit, origin }` : échelle = échelle d'ajustement actuelle × zoom ; origine qui centre `(cx, cy)`, bornée pour que la carte couvre le cadre sur un axe où elle le dépasse, et centrée sinon.
3. `zoomAt(view, factor, pointPixels, …)` : nouveau zoom borné à [1, 8], le point de carte sous `pointPixels` y reste ; `reveal(view, point, …)` : décale le centre juste assez pour garder un point en vue.

### `2)` Zoom dans la carte

> Molette, pincement et API, sans toucher aux conversions existantes.

1. `SceneCanvas.redraw` (`SceneCanvas.ts:222-228`) tire `pixelsPerUnit` et `origin` de `layout(this.view, …)`.
2. Événement `wheel` du stage : `preventDefault`, ignoré si `Konva.isDragging()`, facteur `exp(-deltaY × k)` (Ctrl + molette compris), `zoomAt` au pointeur ; redessin regroupé par `requestAnimationFrame`.
3. Deux doigts (`touchstart/touchmove` à deux touches) : l'écart zoome autour du milieu, le milieu déplace la vue.
4. Méthodes `zoomIn()`, `zoomOut()` (×1,25 autour du centre), `fit()`, `reveal(point)` ; un changement de vue efface les bulles d'issue (`effects`), posées en pixels.

### `3)` Contrôles et clavier

> Le zoom s'atteint sans molette.

1. `MapView.vue` : trois boutons en surimpression (« Zoom avant », « Zoom arrière », « Ajuster »), désactivés à la borne ; `touch-action: pan-x pan-y` sur `.map` (un doigt fait défiler la page, le pincement reste à la carte) ; expose `zoomIn`, `zoomOut`, `fit`.
2. `MapView.vue` : à chaque déplacement du curseur clavier (`onKeydown`, `startCursor`), `canvas.reveal(cursor)`.
3. `App.vue` `onKey` : `+`/`=`, `-`, `0` sans modificateur et hors saisie, aussi pendant le calage (avant le `return` de `App.vue:984`) ; `mapView.fit()` après `api.createScene` (`:377`) et `api.loadMap` (`:431`).

### `4)` Documentation

> Docs à jour pour le zoom.

1. `DESIGN.md` : contrôles de zoom de la carte (place, boutons, états).
2. `architecture.md` : vue client (zoom, centre), non gardée, remise à la carte entière à la création et au chargement ; `codebase-map.md` : `map/view.ts`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe ; à `FIT_VIEW`, la carte s'affiche exactement comme avant. |
| 2 | La molette et le pincement zooment autour du pointeur, bornés à [×1, ×8], sans faire défiler ni zoomer la page ; poser, glisser, mesurer, cibler et le catalogue visent la position sous le pointeur à tout zoom. |
| 3 | Boutons et touches + − 0 zooment ; « Ajuster » et 0 montrent la carte entière ; le curseur clavier reste en vue ; une nouvelle carte ou une carte chargée s'affiche entière. |
| 4 | `DESIGN.md`, `architecture.md` et `codebase-map.md` décrivent le zoom. |
