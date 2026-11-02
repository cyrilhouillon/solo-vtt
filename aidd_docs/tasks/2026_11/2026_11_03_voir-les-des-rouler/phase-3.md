---
status: pending
---

# Instruction: Carte : jauge de PV sur chaque jeton et issue sur la cible

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/src/
├── map/SceneCanvas.ts            ✏️
├── components/MapView.vue        ✏️
└── App.vue                       ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081/5174, carte, trois créatures posées | chaque jeton a sa jauge pleine | browser |
| Happy path   | Attaque qui touche pour 11 | à l'arrêt des dés, « −11 » apparaît au-dessus de la cible puis s'efface seul ; sa jauge baisse et passe en `hp-low` sous la moitié | browser |
| Edge case    | Raté | « Raté » au-dessus de la cible, puis effacé | browser |
| Edge case    | Clic, survol et glisser pendant la bulle | la bulle ne capte rien : la carte réagit comme sans elle | browser |
| Edge case    | Redessin pendant la bulle (survol, changement de tour) | la bulle reste jusqu'à son effacement | browser |
| Edge case    | Créature à 0 PV | pas de jauge : jeton gris et croix, comme aujourd'hui | browser |
| Edge case    | Jeton en attente du serveur | pas de jauge tant que ses PV sont inconnus | browser |
| Edge case    | `prefers-reduced-motion: reduce` | la bulle apparaît et disparaît sans glisser | browser |
| Edge case    | Thème clair puis sombre | jauge et bulle lisibles dans les deux | browser |
| Teardown     | Arrêter 8081 puis 5174 | ports libres, 8080/5173 intacts | system |

## Wireframe

```txt
┌─ carte ──────────────────────────────┐
│          (1) [ −11 ]                 │
│               ╭───╮                  │
│               │(2)│                  │
│               ╰───╯                  │
│               (3) ▓▓░░               │
│               (4) [Squelette]        │
└──────────────────────────────────────┘
```

1. Bulle d'issue : « −11 » ou « Raté », s'élève puis s'efface.
2. Jeton : inchangé.
3. Jauge de PV : largeur du jeton, `hp` ou `hp-low`.
4. Nom : sous la jauge (sélection, survol, tour).

## Tasks to do

### `1)` Jauge de PV

> Les PV se lisent sur la carte sans ouvrir la feuille.

1. Dans `drawToken`, sous chaque jeton aux PV connus et non nuls, une jauge fine (largeur du jeton) en `--hp`, `--hp-low` à la moitié ou moins (seuil de `TokenBoard`) ; lire ces couleurs dans `readColors`.
2. Décaler le nom sous la jauge.

### `2)` Bulle d'issue

> L'issue se lit là où le regard se pose après le clic.

1. Ajouter à `SceneCanvas` une couche d'effets sans écoute, que `redraw` ne vide pas, et `showImpact(tokenId, text)` : une bulle au-dessus du jeton (même pavé que les autres bulles), qui s'élève et s'efface seule ; sans glissement sous `prefers-reduced-motion`.
2. `MapView` l'expose (`defineExpose`).
3. Dans `App.vue`, sur `landed` de la piste pour un résultat `live` : « −N » (somme des dégâts) si touché, « Raté » sinon, sur le jeton de la cible.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Chaque jeton vivant porte une jauge proportionnelle à ses PV, mise à jour après l'attaque. |
| 2 | La bulle apparaît à l'arrêt des dés, disparaît seule, ne bloque aucun clic, et n'apparaît pas au rechargement. |
