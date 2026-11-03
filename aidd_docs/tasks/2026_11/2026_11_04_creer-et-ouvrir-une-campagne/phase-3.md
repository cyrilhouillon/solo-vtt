---
status: pending
---

# Instruction: accueil des campagnes dans le navigateur

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── vtt-web/src/
│   ├── App.vue                         ✏️ réduit : accueil ou écran de jeu (:key = id de campagne)
│   ├── api.ts                          ✏️ campaigns, createCampaign, openCampaign, currentCampaign
│   ├── types.ts                        ✏️ CampaignView
│   └── components/
│       ├── GameScreen.vue              ✅ l'ancien App.vue (git mv), prop campaign, émet home
│       ├── HomeScreen.vue              ✅ liste, création, ouverture
│       └── AppIcon.vue                 ✏️ icône 'home'
├── DESIGN.md                           ✏️ écran d'accueil, bouton Accueil
└── aidd_docs/
    ├── memory/project-brief.md         ✏️ fonctionnalité, chemins ~/.solo-vtt/campagnes
    ├── memory/codebase-map.md          ✏️ HomeScreen, GameScreen
    └── backlog/stories/creer-et-ouvrir-une-campagne-depuis-l-accueil.md  ✏️ status: done
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | serveurs de test 8081 / 5174 (`testing.md`, avec `solovtt.campaigns.path` dans le scratchpad) | accueil « Aucune campagne. » | browser |
| Happy path   | saisir « Phandelver », Créer | elle apparaît dans la liste | browser |
| Happy path   | Ouvrir « Phandelver » | écran de jeu, la barre affiche « Phandelver » ; tiroir Nouvelle carte ouvert, aucune carte enregistrée | browser |
| Happy path   | carte, poses, enregistrer « Crypte », combat, attaque jusqu'à Bouclier ; Accueil ; créer et ouvrir « Strahd » | « Strahd » : aucune carte, tiroir Cartes enregistrées vide | browser |
| Happy path   | Accueil, rouvrir « Phandelver » | même carte, positions, PV, combat, tour, journal ; dialogue Bouclier rouvert ; « Crypte » listée | browser |
| Happy path   | redémarrer 8081, recharger la page | accueil avec les deux campagnes ; rouvrir « Phandelver » la retrouve à l'identique | browser |
| Edge case    | nom vide ou blanc | Créer désactivé | browser |
| Edge case    | 8081 redémarré pendant l'écran de jeu, puis une action | retour à l'accueil avec un message « Campagne fermée par le serveur (redémarrage ?) : la rouvrir. » | browser |
| Edge case    | serveur injoignable à l'accueil | message d'erreur dans l'accueil, sans liste | browser |
| Edge case    | clavier seul | champ, Créer, chaque Ouvrir et le bouton Accueil atteignables au Tab | browser |
| Edge case    | thème clair et largeur téléphone | accueil lisible, sans défilement horizontal | browser |
| Teardown     | arrêter 8081 puis 5174 ; vider le scratchpad | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌───────────────────────────────────────────────┐
│ (1) Solo VTT                      [☀] [?]      │
├───────────────────────────────────────────────┤
│ (2) Campagnes                                  │
│     ┌───────────────────────────────────────┐ │
│     │ (3) Phandelver              [Ouvrir]  │ │
│     │     Strahd                  [Ouvrir]  │ │
│     └───────────────────────────────────────┘ │
│ (4) Nouvelle campagne                          │
│     Nom [____________________]  [Créer]        │
│ (5) message d'erreur                           │
└───────────────────────────────────────────────┘
```

1. Barre : même barre que l'écran de jeu, sans carte ni combat ; thème et raccourcis gardés.
2. Titre de l'accueil.
3. Liste : une ligne par campagne, son nom et Ouvrir ; « Aucune campagne. » si vide.
4. Création : champ nom requis (80 caractères au plus), Créer ; Entrée crée.
5. Erreur du serveur (injoignable, nom refusé), dans la barre d'état comme sur l'écran de jeu.

Écran de jeu : la barre gagne, avant `map-info`, le nom de la campagne et un bouton icône « Accueil ».

## Tasks to do

### `1)` Appels et types

> Le client parle aux routes des campagnes.

1. `CampaignView { id, name }`.
2. `api.campaigns()`, `api.createCampaign(name)`, `api.openCampaign(id)`, `api.currentCampaign()` (204 → `null`).

### `2)` Écran de jeu séparé

> L'écran actuel devient un composant remontable.

1. `git mv src/App.vue src/components/GameScreen.vue` ; imports relatifs corrigés ; prop `campaign: CampaignView`, émet `home`.
2. Barre : nom de la campagne et bouton Accueil (icône `home`, `AppIcon`) ; le bouton émet `home`.
3. `resync` (`App.vue:320`) relit `api.currentCampaign()` : aucune ou autre campagne → émet `home` avec le message de fermeture ; sinon comportement actuel.

### `3)` Accueil

> Choisir ou créer une campagne.

1. `HomeScreen.vue` : liste relue au montage, création (le serveur rend la campagne, la liste est relue), Ouvrir → `api.openCampaign` puis émet `open(campaign)`.
2. `App.vue` réduit : `campaign` à `null` au chargement (l'accueil est toujours l'écran d'ouverture) ; `HomeScreen` si `null`, sinon `<GameScreen :key="campaign.id" :campaign @home>` ; un message de retour passé à l'accueil s'y affiche.
3. Styles du `DESIGN.md` (jetons de `style.css`, thèmes sombre et clair).

### `4)` Documentation et clôture

> Décrire l'écran et clore la story.

1. `DESIGN.md` : écran d'accueil, bouton Accueil de la barre.
2. `project-brief.md` : campagnes dans les fonctionnalités clés ; chemins `~/.solo-vtt/campagnes/<id>/` à la place de `partie.json` et `cartes.json`.
3. `codebase-map.md` : `HomeScreen`, `GameScreen`, `App.vue` réduit.
4. Story `status: done` ; plan `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe (`vue-tsc`). |
| 2 | Écran de jeu inchangé hors barre ; le nom de la campagne s'affiche ; Accueil ramène à la liste ; un redémarrage du serveur ramène à l'accueil avec le message. |
| 3 | Parcours navigateur sur 5174 : créer deux campagnes, aller-retour avec combat et décision en attente, cartes enregistrées séparées, retrouvées après redémarrage de 8081. |
| 4 | Docs à jour ; story et plan clos. |
