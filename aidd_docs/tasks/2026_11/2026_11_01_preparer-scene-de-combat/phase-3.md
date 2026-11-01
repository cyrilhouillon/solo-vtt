---
status: pending
---

# Instruction: client Vue — carte, catalogue, feuille

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── .gitignore                        ✏️ node_modules/, dist/
└── vtt-web/
    ├── package.json                  ✅ vue, konva ; dev : vite, typescript, vue-tsc, @vitejs/plugin-vue (versions figées)
    ├── vite.config.ts                ✅ proxy /api → http://localhost:8080
    ├── tsconfig.json                 ✅
    ├── index.html                    ✅ lang="fr"
    └── src/
        ├── main.ts                   ✅
        ├── App.vue                   ✅ disposition : barre, catalogue, carte, feuille
        ├── api.ts                    ✅ appels typés de l'API phase 2, erreurs remontées en message
        ├── types.ts                  ✅ types des DTO
        ├── map/SceneCanvas.ts        ✅ rendu Konva sans Vue : grille, jetons, clic de pose, sélection, ligne de mesure
        └── components/
            ├── NewMapForm.vue        ✅ largeur, hauteur, unité (m / ft)
            ├── CatalogPanel.vue      ✅ personnages et monstres, entrée choisie à poser
            ├── MapView.vue           ✅ monte SceneCanvas, affiche la distance
            └── CreatureSheet.vue     ✅ feuille de la créature sélectionnée
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | lancer `vtt-server` (`quarkus:dev`) et `npm run dev` | page ouverte, formulaire de nouvelle carte | browser |
| Happy path   | créer une carte 30 × 20 m | carte vide quadrillée (case de 1,5 m ou 5 ft), unité affichée | browser |
| Happy path   | choisir « Gobelin guerrier » puis cliquer sur la carte, deux fois ; poser la guerrière | trois jetons nommés aux points cliqués | browser |
| Happy path   | sélectionner deux jetons | ligne entre eux et distance dans l'unité de la carte (ex. « 7,5 m ») | browser |
| Happy path   | sélectionner un jeton | sa feuille s'affiche : nom, CA, PV, vitesse, attaque, résistances, vulnérabilités | browser |
| Edge case    | cliquer sur la carte sans entrée choisie | rien n'est posé | browser |
| Edge case    | serveur arrêté ou erreur 400 | message d'erreur en français, pas de page cassée | browser |
| Edge case    | créer une carte en pieds | distances affichées en ft | browser |
| Teardown     | créer une nouvelle carte | carte vide, feuille fermée | browser |

## Wireframe

```txt
┌──────────────────────────────────────────────────────────────────┐
│ (1) Barre : Nouvelle carte [largeur] [hauteur] [m|ft] [Créer]     │
├───────────────┬──────────────────────────────────┬───────────────┤
│ (2) Catalogue │ (3) Carte (canvas Konva)          │ (5) Feuille   │
│  Personnages  │  ┌────────────────────────────┐  │  Nom          │
│   · Guerrière │  │  ·  ·  ·  ·  ·  ·  ·  ·    │  │  CA · PV      │
│   · Magicienne│  │  ·  (G)────────(g1) ·  ·   │  │  Vitesse      │
│  Monstres     │  │  ·  ·  ·  ·  ·  ·  ·  ·    │  │  Attaque      │
│   · Gobelin   │  │  ·  ·  (g2) ·  ·  ·  ·     │  │  Résist./Vuln.│
│   · Squelette │  └────────────────────────────┘  │               │
│   · Loup      │ (4) Distance : 7,5 m              │               │
└───────────────┴──────────────────────────────────┴───────────────┘
```

1. Barre : créer une carte vide avec ses dimensions et son unité.
2. Catalogue : entrée à poser, mise en évidence quand choisie.
3. Carte : grille, jetons étiquetés, clic pour poser, clic pour sélectionner (deux au plus), ligne de mesure.
4. Distance : entre les deux jetons sélectionnés, dans l'unité de la carte.
5. Feuille : créature du dernier jeton sélectionné.

## Tasks to do

### `1)` Socle du client

> Un client Vue 3 + TypeScript se construit et parle au serveur.

1. Créer `vtt-web` (Vite, Vue 3, TS) et le proxy `/api`.
2. Écrire `types.ts` et `api.ts` d'après les DTO de la phase 2.

### `2)` Carte Konva isolée

> Le rendu de la carte ne dépend pas de Vue.

1. `SceneCanvas` : constructeur (conteneur, scène), `render(scene)`, rappels `onPlace(x, y)` et `onSelect(ids)` ; conversion pixels ↔ unité de carte en un seul endroit.
2. `MapView` le monte et le détruit avec le composant.

### `3)` Panneaux et parcours

> Le parcours de la story se fait au clic, sans saisie.

1. `NewMapForm`, `CatalogPanel`, `CreatureSheet`, `App` selon le wireframe.
2. Distance demandée au serveur (`/api/scene/distance`), jamais recalculée côté client.
3. Textes de l'interface en français.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` (avec `vue-tsc`) passe sans erreur dans `vtt-web` |
| 2 | `SceneCanvas.ts` n'importe pas `vue` ; les jetons apparaissent aux coordonnées cliquées |
| 3 | le parcours du Test Scope se fait dans le navigateur : carte créée, 3 créatures posées, distance et feuille affichées |
