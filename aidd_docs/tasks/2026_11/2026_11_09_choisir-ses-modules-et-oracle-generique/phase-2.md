---
status: pending
---

# Instruction: activer ou désactiver un module d'une campagne existante

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── vtt-server/src/
│   ├── main/java/solovtt/server/
│   │   ├── CampaignStore.java ✏️
│   │   ├── CampaignResource.java ✏️
│   │   ├── Dtos.java ✏️
│   │   └── persistence/FileCampaignStore.java ✏️
│   └── test/java/solovtt/server/
│       ├── CampaignApiTest.java ✏️
│       └── persistence/FileCampaignStoreTest.java ✏️
└── vtt-web/src/
    ├── api.ts ✏️
    └── components/
        ├── CampaignModulesDialog.vue ✅
        └── NotebookScreen.vue ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Deux campagnes A et B avec Mythic, chaos de A réglé à 7 | Prêt | api |
| Happy path   | `PUT /api/campaigns/A/modules {modules: []}` | 200 `[]` ; `…/mythic/chaos` de A 404, de B 200 | api |
| Happy path   | `PUT …/A/modules {modules: ["mythic"]}` | Chaos de A revient à 7 ; Fils / Personnages gardés | api |
| Happy path   | Carnet de A : Modules, décocher Mythic, Enregistrer | Panneau et section Mythic disparaissent sans recharger | browser |
| Happy path   | Recocher Mythic, Enregistrer | Panneau et section reviennent, mêmes pages et contenu | browser |
| Edge case    | `PUT` module inconnu, ou corps sans `modules` | 400, rien ne change | api |
| Edge case    | `PUT` campagne inconnue | 404 | api |
| Edge case    | Lien `[[Fils]]` dans une page ordinaire, Mythic désactivé | Le lien reste vivant et ouvre la page | browser |
| Edge case    | Échec du `PUT` | Erreur dans le dialogue, modules inchangés | browser |
| Teardown     | Arrêter 8081 puis 5174 | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌───────────────────────────────────────────────┐
│ (1) Barre : Accueil · Solo VTT · Nom          │
│              [Modules] [Combat] [thème]       │
├───────────────────────────────────────────────┤
│        ┌──────────────────────────────┐       │
│        │ (2) Modules de la campagne    │       │
│        │   [x] Mythic                  │       │
│        │   [ ] Oracle                  │       │
│        │ (3) message d'erreur          │       │
│        │        [Annuler] [Enregistrer]│       │
│        └──────────────────────────────┘       │
└───────────────────────────────────────────────┘
```

1. Barre du Carnet : un bouton « Modules » avant « Combat ».
2. Dialogue modal (`<dialog>`, comme `TrashDialog`) : une case par module du client, cochée s'il est actif.
3. Raison d'un refus du serveur, le dialogue reste ouvert.

## Tasks to do

### `1)` Le serveur change les modules d'une campagne

> Remplacer la liste des modules actifs, sans toucher leurs données.

1. `CampaignStore.setModules(id, modules)` (`inMemory`, `FileCampaignStore` : réécrit `campagne.json` atomiquement).
2. `PUT /api/campaigns/{id}/modules` (`CampaignModulesDto`) : `InstalledModules.check`, 400 sans liste, 404 campagne inconnue ; rend la liste gardée.
3. Tests : lignes api du Test Scope ; `FileCampaignStoreTest` relit la liste changée.

### `2)` Le Carnet change ses modules

> Un dialogue depuis la barre du Carnet.

1. `api.ts` : `campaignApi(id).setModules(modules)`.
2. `CampaignModulesDialog.vue` : cases, Annuler, Enregistrer ; émet la liste gardée ; erreur affichée dans le dialogue.
3. `NotebookScreen.vue` : bouton « Modules » ; à l'enregistrement, attend `saved()`, remplace les modules actifs, lance `prepare` des modules nouvellement actifs, relit le Carnet.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Les lignes api du Test Scope passent ; les données d'un module désactivé puis réactivé sont identiques. |
| 2 | Sur 5174 : désactiver puis réactiver Mythic suit les lignes browser du Test Scope ; `npm run build` vert. |
