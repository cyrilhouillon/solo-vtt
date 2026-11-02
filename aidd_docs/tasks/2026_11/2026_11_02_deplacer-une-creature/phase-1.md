---
status: done
---

# Instruction: Client : hors grille par défaut, Maj cale sur la case

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/src/
├── App.vue                ✏️ messages de pose : Maj cale sur la case, case occupée = chevauchement
└── map/
    ├── grid.ts            ✏️ occupant : jeton à moins d'une case de centre à centre
    └── SceneCanvas.ts     ✏️ free = Maj non tenue ; aperçu et clic inversés
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081/5174 (`testing.md`), carte 30 × 20 m, Guerrière armée dans le catalogue | Carte vide, pose en cours | browser |
| Happy path   | Clic sans Maj sur un point quelconque | Jeton posé exactement sous le pointeur (au dixième), pas au centre de la case | browser |
| Happy path   | Maj + clic | Jeton posé au centre de la case ; l'aperçu se cale dès l'appui sur Maj, sans bouger la souris | browser |
| Edge case    | Clic à moins d'une case du centre d'un jeton existant, avec ou sans Maj | Anneau `--danger` dans l'aperçu, pose refusée avec « Trop près de … » | browser |
| Edge case    | Clic à exactement une case d'un jeton calé (case voisine, Maj) | Pose acceptée | browser |
| Edge case    | Pose au clavier (flèches, Entrée) | Toujours calée sur la case ; l'annonce dit « occupée par … » si un jeton chevauche | browser |
| Teardown     | Arrêter 8081 puis 5174 | Serveurs de l'utilisateur intacts | system |

## Tasks to do

### `1)` Inverser Maj

> Hors grille par défaut, Maj tenue cale sur le centre de la case.

1. `SceneCanvas` : `free = !shiftKey` au clic du sol, au `mousemove` et dans `onShift` (keydown = calé).
2. Mettre à jour la doc de `SceneCanvasEvents.onBackgroundClick`.

### `2)` Case occupée par chevauchement

> Une position est occupée si un autre jeton est à moins d'une case de centre à centre, calée ou non.

1. `grid.occupant(scene, point, ignoreId?)` : distance euclidienne < `CELL` (moins une tolérance) ; `ignoreId` servira au déplacement (phase 4).
2. `SceneCanvas.drawPreview` et `App.onBackgroundClick` vérifient l'occupation aussi en position libre.

### `3)` Textes

> Les consignes disent le nouveau geste.

1. Barre d'état de pose : « Cliquer pour poser ; Maj + clic cale sur la case, Échap arrête. »
2. Refus : « Trop près de {nom}. Choisir une autre position. »

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1    | Sans Maj, le jeton se pose sous le pointeur ; avec Maj, au centre de la case, aperçu compris. |
| 2    | Une pose à moins d'une case d'un jeton est refusée, calée ou non ; une case voisine est acceptée. |
| 3    | La barre d'état et le message de refus décrivent Maj comme la touche qui cale. |
| 1-3  | `npm run build` passe. |
