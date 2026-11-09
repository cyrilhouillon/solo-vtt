---
status: pending
---

# Instruction: choisir les modules d'une campagne à sa création

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── vtt-module-api/src/main/java/solovtt/module/
│   └── InstalledModule.java ✅
├── mythic-server/src/main/java/mythic/server/
│   └── MythicModule.java ✅
├── vtt-server/src/
│   ├── main/java/solovtt/server/
│   │   ├── Campaign.java ✏️
│   │   ├── CampaignStore.java ✏️
│   │   ├── CampaignResource.java ✏️
│   │   ├── Dtos.java ✏️
│   │   ├── InstalledModules.java ✅
│   │   ├── InactiveModuleFilter.java ✅
│   │   └── persistence/FileCampaignStore.java ✏️
│   └── test/java/solovtt/server/
│       ├── CampaignApiTest.java ✏️
│       ├── CampaignStoreTest.java ✏️
│       ├── CampaignSessionsTest.java ✏️
│       ├── CarnetDeBoutEnBoutTest.java ✏️
│       └── persistence/
│           └── FileCampaignStoreTest.java ✏️
└── vtt-web/src/
    ├── api.ts ✏️
    ├── types.ts ✏️
    ├── components/
    │   ├── HomeScreen.vue ✏️
    │   ├── NotebookScreen.vue ✏️
    │   └── PageTree.vue ✏️
    └── notebook/tree.ts ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveur de test 8081 (scratchpad), Vite 5174 ; `@QuarkusTest` avec Mythic installé | Accueil vide | system |
| Happy path   | `POST /api/campaigns {name, modules: ["mythic"]}` | 201, `modules: ["mythic"]` ; `GET …/modules` le rend | api |
| Happy path   | `POST /api/campaigns {name, modules: []}` puis `GET …/mythic/chaos` | 404 « Module inactif dans cette campagne » | api |
| Happy path   | `POST /api/campaigns {name}` (sans liste) | Tous les modules installés actifs | api |
| Happy path   | Accueil : décocher Mythic, créer, ouvrir | Carnet sans colonne de modules ni section Mythic | browser |
| Happy path   | Accueil : Mythic coché, créer, ouvrir | Panneau Mythic, section Fils / Personnages | browser |
| Edge case    | `modules: ["inconnu"]` | 400 « Module inconnu : inconnu » | api |
| Edge case    | `modules: ["Mythic/.."]` | 400, id invalide | api |
| Edge case    | `campagne.json` d'avant, sans `modules` | Lue avec tous les modules installés actifs | system |
| Edge case    | Fichier relu après redémarrage (`FileCampaignStore` neuf sur le même dossier) | Mêmes modules | system |
| Edge case    | `GET /api/mythic/meaning-tables` (route commune) | Pas filtrée | api |
| Teardown     | Arrêter 8081 puis 5174 | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌─────────────────────────────────────┐
│ (1) Barre : Solo VTT · thème         │
├─────────────────────────────────────┤
│ (2) Campagnes                        │
│     Nom ............... [Ouvrir]     │
├─────────────────────────────────────┤
│ (3) Nouvelle campagne                │
│     Nom [__________________]         │
│     (4) Modules                      │
│       [x] Mythic                     │
│       [x] Oracle                     │
│                          [Créer]     │
└─────────────────────────────────────┘
```

1. Barre d'application, inchangée.
2. Liste des campagnes, inchangée.
3. Formulaire de création : le nom, puis les modules, puis Créer.
4. Groupe de cases (`fieldset`, légende « Modules »), une par module du client, cochée par défaut ; absent s'il n'y a aucun module.

## Tasks to do

### `1)` Le serveur connaît ses modules

> Le cœur sait quels modules sont installés sans en nommer aucun.

1. `vtt-module-api` : interface `InstalledModule { String id(); }`, javadoc : un module la déclare en bean.
2. `mythic-server` : `MythicModule` (`@ApplicationScoped`, `id()` = `MythicResource.MODULE_ID`).
3. `vtt-server` : `InstalledModules` (`@ApplicationScoped`, `Instance<InstalledModule>`) : `ids()` triés, `check(List<String>)` (400 si inconnu, doublons retirés).

### `2)` La campagne garde ses modules

> La fiche porte la liste des modules actifs.

1. `Campaign(id, name, modules)` ; `modules` `null` : tous les modules installés.
2. `CampaignStore.create(name, modules)`, et `create(name)` par défaut (`modules` `null`) ; `inMemory` et `FileCampaignStore` l'écrivent (`campagne.json`, `NON_NULL`) ; une fiche d'avant se lit avec `modules` `null`.
3. Tests des stores (`CampaignStoreTest`, `FileCampaignStoreTest`, `CampaignSessionsTest`, `CarnetDeBoutEnBoutTest`) compilent ; nouveaux cas de modules.

### `3)` L'API choisit et refuse

> Créer avec des modules, les lire, refuser les routes d'un module inactif.

1. `CampaignNameRequest` → `CreateCampaignRequest(name, modules)` ; `CampaignResource.create` valide par `InstalledModules.check` ; absent : `null`.
2. `CampaignDto` gagne `modules` (résolus : `null` → `InstalledModules.ids()`).
3. `GET /api/campaigns/{id}/modules` → `CampaignModulesDto(modules)` ; 404 campagne inconnue.
4. `InactiveModuleFilter` (`@ServerRequestFilter`) : chemin `/api/campaigns/{id}/{segment}/…`, segment installé mais inactif → lève `NotFoundException("Module inactif dans cette campagne")`, rendue par `ErrorMapper` (corps `ErrorDto`, vérifié par le test).
5. `CampaignApiTest` : chaque ligne api du Test Scope.

### `4)` Le client choisit et n'affiche que les modules actifs

> Accueil et Carnet suivent le choix.

1. `types.ts` : `CampaignView.modules` ; `api.ts` : `createCampaign(name, modules)`, `campaignApi(id).modules()`.
2. `HomeScreen.vue` : `fieldset` de cases (`notebookModules`, toutes cochées par défaut), envoyées à la création.
3. `NotebookScreen.vue` : lit `modules()` au montage avant `prepareModules` ; `activeModules` et `activePanelModules` remplacent `notebookModules` / `panelModules` ; échec de lecture : bandeau, aucun module.
4. `tree.ts` : `visiblePages(pages, activeIds)` retire les pages dont la racine porte une section inactive ; `NotebookScreen` la passe à `PageTree` et au dialogue Déplacer, les liens gardent toutes les pages.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `InstalledModules.ids()` rend `["mythic"]` sous `@QuarkusTest` ; `grep -rn mythic vtt-server/src/main` vide. |
| 2 | Une fiche sans `modules` se relit ; une fiche avec `modules` les relit après un nouveau `FileCampaignStore`. |
| 3 | Les lignes api du Test Scope passent dans `CampaignApiTest` ; `mvn -q test` vert. |
| 4 | Sur 5174 : campagne sans Mythic sans panneau ni section ; avec Mythic, comme avant ; `npm run build` vert. |
