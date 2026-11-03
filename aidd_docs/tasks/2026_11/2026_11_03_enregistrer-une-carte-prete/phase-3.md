---
status: pending
---

# Instruction: tiroir « Cartes enregistrées » dans le navigateur

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── vtt-web/src/
│   ├── api.ts                          ✏️ savedMaps, saveMap, loadMap
│   ├── types.ts                        ✏️ SavedMapView
│   ├── App.vue                         ✏️ tiroir 'maps', bouton du rail, chargement
│   ├── replaceWarning.ts               ✅ describeLoss, sorti de NewMapForm
│   └── components/
│       ├── AppIcon.vue                 ✏️ icône 'saved'
│       ├── NewMapForm.vue              ✏️ importe describeLoss
│       └── SavedMapsPanel.vue          ✅ enregistrer sous un nom, liste, charger
├── DESIGN.md                           ✏️ tiroir Cartes enregistrées
└── aidd_docs/
    ├── memory/project-brief.md         ✏️ fonctionnalité
    └── backlog/stories/enregistrer-puis-charger-une-carte-prete.md  ✏️ status: done
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | serveurs de test 8081 / 5174 (`testing.md`, avec `solovtt.maps.path`), carte sur image, grille masquée, trois adversaires posés | carte affichée | browser |
| Happy path   | rail « Cartes enregistrées », saisir « Crypte », Entrée | « Crypte » apparaît dans la liste ; annonce « Carte « Crypte » enregistrée. » | browser |
| Happy path   | attaquer, lancer le combat, puis « Charger » sur « Crypte » et confirmer | même image, grille masquée, trois créatures aux positions, PV pleins ; combat, journal et piste vides ; la barre affiche « Crypte » ; le tiroir Catalogue s'ouvre | browser |
| Happy path   | poser un personnage, lancer le combat | initiative tirée pour les quatre créatures | browser |
| Happy path   | redémarrer le serveur 8081, rouvrir le tiroir | « Crypte » toujours listée | browser |
| Edge case    | saisir un nom déjà pris | avertissement « Remplace la carte enregistrée « Crypte » » avant d'enregistrer | browser |
| Edge case    | aucune carte en cours | champ et bouton Enregistrer désactivés, la liste reste chargeable | browser |
| Edge case    | charger une carte dont une créature n'est plus au catalogue (fichier `cartes.json` du scratchpad modifié à la main) | message du serveur nommant l'entrée ; carte en cours intacte | browser |
| Edge case    | aucune carte enregistrée | « Aucune carte enregistrée. » | browser |
| Edge case    | clavier seul | rail, champ, Enregistrer, Charger et confirmation atteignables au Tab ; Échap ferme le tiroir | browser |
| Teardown     | arrêter 8081 puis 5174 ; vider le scratchpad | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌──────┬───────────────────────────────┬──────────────
│ (1)  │ (2) Cartes enregistrées        │  table…
│ Rail │ ───────────────────────────── │
│ [≡]  │ (3) Nom [______________]       │
│ [▦]  │     [Enregistrer]              │
│ [★]  │     Remplace « Crypte ».       │
│      │ ───────────────────────────── │
│      │ (4) Crypte                     │
│      │     30 × 20 m · 3 créatures    │
│      │     [Charger]                  │
│      │     Grotte                     │
│      │     100 × 65 ft · 5 créatures  │
│      │     [Charger]                  │
│      │ (5) Remplace la carte actuelle,│
│      │     ses 4 créatures et son     │
│      │     journal. [Charger] [Annuler]│
└──────┴───────────────────────────────┴──────────────
```

1. Rail : troisième bouton, « Cartes enregistrées », après Catalogue et Nouvelle carte.
2. Tiroir : titre et filet effilé, comme Nouvelle carte.
3. Enregistrer : champ nom (requis), bouton ; avertissement si le nom existe déjà ; désactivé sans carte.
4. Liste : une entrée par carte, nom, dimensions et nombre de créatures, bouton Charger.
5. Confirmation sous l'entrée choisie quand une carte est en cours, même formule et bouton `danger` que Nouvelle carte ; sans carte en cours, Charger agit d'emblée.

## Tasks to do

### `1)` Appels et types

> Le client parle aux nouvelles routes.

1. `SavedMapView { name, width, height, unitSymbol, imageId, creatureCount }` ; `SceneView.mapName: string | null`.
2. `api.savedMaps()`, `api.saveMap(name)`, `api.loadMap(name)` (rend `SceneView`).

### `2)` Panneau

> Enregistrer et charger depuis un tiroir.

1. `SavedMapsPanel.vue` : props `maps`, `tokenCount` (null sans carte) ; émet `save(name)`, `load(name)`, `cancel` ; Échap émet `cancel` ; formule de perte partagée : `describeLoss` sort de `NewMapForm.vue:148` vers `replaceWarning.ts`, importée par les deux.
2. `AppIcon` : `saved` ajouté à `IconName` (`AppIcon.vue:3`), dessiné dans la même grille 24, trait 1.75.

### `3)` Branchement dans `App.vue`

> Le chargement remet le client à zéro comme une nouvelle carte.

1. `drawer` accepte `'maps'` (`App.vue:42`) ; `NewMapForm` passe en `v-else-if="drawer === 'map'"` (`App.vue:982`) ; bouton du rail ; liste relue à l'ouverture du tiroir et après chaque enregistrement.
2. Enregistrer et charger hors de `attempt` (qui remplace tout 404/409 par le message de `resync`, `App.vue:260`) : `error` = message du serveur (nom inconnu, entrée absente du catalogue, aucune carte).
3. Enregistrer réussi : `tell`.
4. Barre : avec `mapName`, `map-info` (`App.vue:904`) commence par « Crypte · » avant les dimensions.
5. Charger réussi : même remise à zéro que `createScene` (`scene` = réponse), `drawer = 'catalog'`, `tell("Carte « X » chargée : N créatures.")`.

### `4)` Documentation et clôture

> Décrire l'écran et clore la story.

1. `DESIGN.md` : tiroir Cartes enregistrées, icône du rail, layout (trois tiroirs).
2. `project-brief.md` : cartes enregistrées dans les fonctionnalités clés.
3. Story `status: done` ; plan `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe (`vue-tsc`). |
| 2 | Le tiroir montre le champ, la liste, l'avertissement de nom pris et la confirmation de remplacement. |
| 3 | Un refus du serveur s'affiche avec son propre message. Parcours navigateur sur 5174 : enregistrer, charger après un combat, poser un personnage et lancer le combat, retrouver la carte après redémarrage du serveur. |
| 4 | Docs à jour ; story et plan clos. |
