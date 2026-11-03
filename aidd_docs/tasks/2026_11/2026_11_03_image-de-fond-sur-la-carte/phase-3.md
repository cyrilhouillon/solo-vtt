---
status: done
---

# Instruction: nouvelle carte depuis une image, fond et bascule de grille dans le navigateur

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/src/
├── api.ts                          ✏️ uploadImage, createScene(…, imageId), showGrid ; message du 413
├── types.ts                        ✏️ SceneView.imageId / gridVisible
├── App.vue                         ✏️ création avec image, bouton grille dans la barre
├── components/
│   ├── NewMapForm.vue              ✏️ choix d'image, cases en largeur ↔ taille de case en pixels, dimensions calculées
│   ├── MapView.vue                 ✏️ relaie image et grille au canevas
│   └── AppIcon.vue                 ✏️ icône « grid »
└── map/
    ├── SceneCanvas.ts              ✏️ image de fond sur la carte, grille affichable
    └── backdrop.ts                 ✅ dimensions de la carte depuis l'image et la grille (pur, sans Vue)
DESIGN.md                           ✏️ champ image du tiroir, bouton grille, grille sur image
aidd_docs/memory/project-brief.md   ✏️ fonctionnalité
aidd_docs/backlog/stories/mettre-une-image-de-fond-sur-la-carte.md  ✏️ status: done
aidd_docs/tasks/2026_11/2026_11_03_image-de-fond-sur-la-carte/plan.md ✏️ status: implemented
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081/5174 (`testing.md`, avec `solovtt.save.path` et `solovtt.images.path` dans le scratchpad) ; image `legendary-armour-shop-v0-7n3tqdwsuczh1.webp` (975 × 975 px, racine du dépôt, ignorée par git) | Application sans carte | browser |
| Happy path   | Nouvelle carte, mètres, choisir l'image, saisir 30 cases en largeur | Taille de case 32,5 px affichée ; carte 30 × 30 cases, 45 × 45 m | browser |
| Happy path   | Créer la carte | L'image remplit la carte ; les lignes de la grille tombent sur celles de l'image ; barre « Carte 45 m × 45 m » | browser |
| Happy path   | Passer en pieds avant de créer | 150 × 150 ft, toujours 30 cases | browser |
| Happy path   | Saisir 32,5 px de case au lieu des cases | 30 cases calculées et affichées | browser |
| Happy path   | Bouton grille de la barre, puis le rebasculer | Grille masquée puis affichée, sur l'image comme sur une carte vide ; `aria-pressed` suit | browser |
| Edge case    | Grille masquée : poser (glisser, Maj), mesurer, cibler, déplacer en combat | Même comportement qu'avec la grille : calage Maj au centre des cases de l'image, distances et portées inchangées | browser |
| Edge case    | Recharger la page, puis redémarrer le serveur de test (8081) | Image et choix de grille reviennent | browser |
| Edge case    | Nouvelle carte sans image | Largeur et hauteur saisies comme aujourd'hui ; aucun fond | browser |
| Edge case    | Choisir une image puis la retirer | Retour aux champs largeur / hauteur | browser |
| Edge case    | Fichier non image, ou image illisible | Message dans le tiroir ; rien n'est envoyé | browser |
| Edge case    | Image de plus de 50 Mo (413) | Message « Image trop lourde (50 Mo au plus) » ; la carte en cours reste | browser |
| Edge case    | Thèmes clair et sombre ; clavier seul dans le tiroir (Tab, Espace sur le choix de fichier, saisie) | Grille lisible sur l'image ; tout le formulaire est atteignable | browser |
| Teardown     | Tuer les processus sur 8081 et 5174 | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌───────────────────────────────────────────────────────────────────┐
│ (1) Solo VTT · Carte 45 m × 45 m · 3 créatures   [combat] [#] [☾] │
├────┬───────────────────────┬──────────────────────────────────────┤
│(2) │ (3) Nouvelle carte    │ (6) Carte                            │
│rail│ Unité ( ) m  ( ) ft   │  ┌────────────────────────────────┐  │
│    │ (4) Image             │  │ image de fond, grille par-     │  │
│    │ [Choisir une image…]  │  │ dessus si affichée, jetons     │  │
│    │ shop.webp 975×975 px  │  │                                │  │
│    │ [Retirer l'image]     │  │                                │  │
│    │ (5) Grille de l'image │  └────────────────────────────────┘  │
│    │ Cases en largeur [30] │                                      │
│    │ Case [32,5] px        │                                      │
│    │ = 30 × 30 cases       │                                      │
│    │   45 × 45 m           │                                      │
│    │ [Créer la carte]      │                                      │
└────┴───────────────────────┴──────────────────────────────────────┘
```

1. Barre : ajoute le bouton grille (`#`, bascule, avant le thème), présent dès qu'une carte existe.
2. Rail : inchangé.
3. Tiroir « Nouvelle carte » : unité, puis image, puis grille de l'image ou largeur / hauteur.
4. Image : choix de fichier (PNG, JPEG, WebP), nom et taille en pixels une fois lue, bouton pour revenir à une carte sans image.
5. Grille de l'image (avec image seulement) : cases en largeur et taille de case en pixels, liées ; dimensions en cases et dans l'unité, calculées. Sans image, les champs largeur et hauteur d'aujourd'hui à cette place.
6. Carte : image étirée sur toute la carte, sous la grille et les jetons.

## Tasks to do

### `1)` API et types

> Le client envoie une image et crée une carte qui la référence.

1. `types.ts` : `SceneView.imageId: string | null`, `gridVisible: boolean`.
2. `api.ts` : `uploadImage(file: File)` envoie le `File` en corps brut avec `Content-Type: file.type` (le `request` actuel force `application/json` quand il y a un corps : le rendre surchargeable) ; `createScene(width, height, unit, imageId?)` ; `showGrid(visible)` (`PUT /api/scene/grid`) ; `imageUrl(id)` ; un 413 donne « Image trop lourde (50 Mo au plus) ».

### `2)` Dimensions depuis l'image

> Cases en largeur et taille de case se déduisent l'une de l'autre, et donnent la carte.

1. `map/backdrop.ts` : `mapFromImage(pixelWidth, pixelHeight, cellPixels, unit)` rend cases en largeur et hauteur et dimensions dans l'unité (`cases × CELL[unit]`) ; 975 × 975 px à 32,5 px → 30 × 30 cases, 45 × 45 m, 150 × 150 ft.
2. `NewMapForm` : `<input type="file" accept="image/png,image/jpeg,image/webp">` ; lecture par `URL.createObjectURL` et `HTMLImageElement` (`naturalWidth`, `naturalHeight`), révoquée ensuite ; erreur de lecture → message dans le tiroir.
3. Avec image : champs liés « Cases en largeur » et « Case (px) » (saisir l'un recalcule l'autre, `step="any"`), dimensions affichées ; largeur et hauteur masquées ; « Retirer l'image » les rend. Le changement d'unité garde le nombre de cases.
4. `emit('create', width, height, unit, file | null)` ; `App.createScene` envoie l'image puis crée la carte ; l'annonce dit les dimensions comme aujourd'hui.

### `3)` Fond et grille sur la carte

> L'image remplit la carte, la grille se montre ou se cache.

1. `SceneCanvas` : `SceneCanvasState` (via `scene`) porte `imageId` et `gridVisible` ; un `HTMLImageElement` gardé par URL (pas rechargé à chaque `redraw`), qui relance `redraw` à son chargement ; `drawGround` ajoute un `Konva.Image` sans écoute aux dimensions de la carte, entre le sol et la grille, et ne trace la grille que si `gridVisible`. Image en erreur (404) : sol seul.
2. Lisibilité de la grille sur une image : vérifier dans les deux thèmes ; si `--map-grid` se perd, un trait doublé (sombre et clair) sur image seulement, décrit dans `DESIGN.md`.
3. `AppIcon` : icône `grid` (trait 1.75, grille 24, comme les autres). `App.vue` : bouton bascule dans la barre (`aria-pressed`, libellé « Masquer la grille » / « Afficher la grille »), qui appelle `api.showGrid` et remplace la scène ; erreur via `attempt`.

### `4)` Documentation et clôture

> La mémoire et le design disent ce qui existe.

1. `DESIGN.md` : tiroir Nouvelle carte (image, grille de l'image), bouton grille de la barre, image de fond sous la grille.
2. `project-brief.md` : carte depuis une image de battlemap, grille affichable.
3. Story `status: done` ; `plan.md` `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe ; une image envoyée puis une carte créée apparaissent dans les requêtes réseau (`/api/images` puis `/api/scene` avec `imageId`). |
| 2 | Avec l'image 975 × 975 px, saisir 30 cases affiche 32,5 px et 45 × 45 m (150 × 150 ft en pieds), et saisir 32,5 px affiche 30 cases ; sans image, le formulaire d'aujourd'hui. |
| 3 | L'image remplit la carte et la grille tombe sur la sienne ; le bouton masque et rend la grille ; Maj, aimant, mesures et portées sont identiques grille masquée ; image et grille reviennent après rechargement et redémarrage du serveur de test. |
| 4 | `DESIGN.md`, `project-brief.md`, la story et le plan décrivent l'état livré. |
