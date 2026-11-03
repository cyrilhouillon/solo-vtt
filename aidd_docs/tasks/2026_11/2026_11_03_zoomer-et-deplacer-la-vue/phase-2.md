---
status: done
---

# Instruction: déplacer la vue en glissant le fond

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── vtt-web/src/
│   ├── App.vue                          ✏️ consigne de calage de la barre d'état (bouton du milieu)
│   ├── map/
│   │   └── SceneCanvas.ts               ✏️ glisser le fond déplace la vue ; clic ignoré après un glisser ; calage au bouton du milieu
│   └── components/
│       ├── MapView.vue                  ✏️ curseurs grab / grabbing
│       └── GridAlignPanel.vue           ✏️ consigne du tiroir (bouton du milieu)
├── DESIGN.md                            ✏️ geste de calage ; glisser le fond
└── aidd_docs/
    ├── memory/architecture.md           ✏️ glisser le fond ; calage au bouton du milieu
    ├── memory/project-brief.md          ✏️ fonctionnalité zoom et vue ; calage
    └── backlog/stories/zoomer-et-deplacer-la-vue-de-la-carte.md  ✏️ status: done
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | serveurs de test 8081 / 5174 ; carte avec image, trois créatures ; zoom ×3 | une partie de la carte en vue | browser |
| Happy path   | glisser une zone vide de la carte à la souris | la vue suit ; la carte ne sort pas du cadre ; à ×1, rien ne bouge | browser |
| Happy path   | au toucher (émulation DevTools) : un doigt sur le fond, puis deux doigts | un doigt fait défiler la page, la vue ne bouge pas ; deux doigts déplacent la vue | browser |
| Happy path   | créature armée au clavier, puis glisser le fond, puis cliquer sans bouger | le glisser ne pose rien ; le clic pose à la position cliquée | browser |
| Happy path   | en ciblage, glisser le fond puis cliquer la cible | la vue bouge, l'attaque part sur la cible cliquée | browser |
| Happy path   | glisser un jeton | le jeton se déplace, la vue ne bouge pas | browser |
| Happy path   | calage : glisser au bouton gauche, puis au bouton du milieu | gauche : la vue bouge, la grille non ; milieu : la grille se décale au pixel entier et les champs suivent, sans défilement automatique | browser |
| Edge case    | glisser depuis le catalogue jusqu'à la carte zoomée | la créature se pose sous le pointeur ; la vue ne bouge pas | browser |
| Edge case    | glisser commencé sur la carte et relâché hors de la carte | la vue s'arrête ; aucun clic ni pose | browser |
| Edge case    | petit tremblement (< seuil) au clic de pose | pose comme avant | browser |
| Teardown     | arrêter 8081 puis 5174 ; vider le scratchpad | 8080 et 5173 intacts | system |

## Tasks to do

### `1)` Glisser le fond

> Un glisser sur le fond déplace la vue, un clic garde son effet.

1. `SceneCanvas.ts` : `mousedown` bouton gauche hors jeton écoutant (cible = sol, image ou stage) note le point ; au toucher, un doigt ne déplace jamais la vue (la page défile, phase 1 : deux doigts) ; au-delà de 4 px, la vue suit le pointeur (centre décalé, borné par `layout`), redessin par `requestAnimationFrame`.
2. Drapeau `panned` : le `click tap` du sol (`SceneCanvas.ts:276`) ne pose rien après un déplacement de vue ; relâché ou sorti du stage, le glisser s'arrête.
3. Le glisser depuis le catalogue (pointeur capturé, `drag` non nul) ne déplace jamais la vue.
4. `MapView.vue` : curseur `grab` sur le fond, `grabbing` pendant le glisser, sans écraser `crosshair` (pose) ni celui des jetons.

### `2)` Calage au bouton du milieu

> Pendant le calage, le bouton gauche bouge la vue, le milieu la grille.

1. `SceneCanvas.ts:132-143` : `gridDrag` ne démarre que sur `button === 1`, avec `preventDefault` ; le bouton gauche déplace la vue comme hors calage.
2. Consignes : barre d'état (`App.vue`, « Calage de la grille : … ») et tiroir (`GridAlignPanel.vue:83`) disent « bouton du milieu » pour décaler la grille et « glisser » pour déplacer la vue ; curseur `move` (`MapView.vue:268`) remplacé par `grab`.

### `3)` Documentation

> Docs et backlog à jour.

1. `DESIGN.md` : glisser le fond déplace la vue ; ligne 281, geste de calage au bouton du milieu.
2. `architecture.md` : geste de vue et de calage ; `project-brief.md` : zoom et vue, calage au bouton du milieu.
3. Story `status: done` ; plan `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Glisser le fond déplace la vue à la souris, et à deux doigts au toucher (un doigt fait défiler la page), sans sortir la carte du cadre ; un clic sans glisser pose, cible ou choisit comme avant ; le catalogue et les jetons gardent leur glisser. |
| 2 | Pendant le calage, le bouton du milieu décale la grille au pixel entier, le bouton gauche déplace la vue ; les deux consignes le disent. |
| 3 | `DESIGN.md`, `architecture.md`, `project-brief.md` et la story décrivent le geste. |
