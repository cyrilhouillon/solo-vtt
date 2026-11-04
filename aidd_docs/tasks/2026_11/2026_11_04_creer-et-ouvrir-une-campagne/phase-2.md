---
status: done
---

# Instruction: ouvrir une campagne par l'API

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── vtt-server/src/
│   ├── main/java/solovtt/server/
│   │   ├── SceneSession.java                     ✏️ constructeur CDI (Dice, CampaignStore), open(Campaign), campaign(), maps(), refus sans campagne
│   │   ├── CampaignResource.java                 ✅ /api/campaigns, /current, /{id}/open
│   │   ├── MapResource.java                      ✏️ cartes de la campagne ouverte
│   │   ├── Dtos.java                             ✏️ CampaignDto, CampaignNameRequest
│   │   ├── NoCampaignException.java              ✅ 409 « Aucune campagne ouverte »
│   │   ├── ErrorMapper.java                      ✏️ NoCampaignException → 409
│   │   └── persistence/
│   │       ├── GameStoreProducer.java            ❌ remplacé par CampaignStore.games
│   │       └── MapStoreProducer.java             ❌ remplacé par CampaignStore.maps
│   ├── main/resources/application.properties     ✏️ retire solovtt.save.path et solovtt.maps.path
│   └── test/java/solovtt/server/
│       ├── Campagnes.java                        ✅ aide : créer et ouvrir une campagne neuve
│       ├── CampaignApiTest.java                  ✅
│       ├── AttackApiTest.java                    ✏️ @BeforeEach ouvre une campagne
│       ├── CombatApiTest.java                    ✏️ idem
│       ├── DecisionApiTest.java                  ✏️ idem
│       ├── MapApiTest.java                       ✏️ idem
│       ├── MoveApiTest.java                      ✏️ idem
│       ├── SceneApiTest.java                     ✏️ idem
│       └── SceneWithoutMapTest.java              ✏️ MapResource construit avec la session ouverte
└── aidd_docs/memory/
    ├── architecture.md                           ✏️ campagne ouverte, CampaignStore, pièges
    ├── codebase-map.md                           ✏️ ressources, port, adaptateur
    └── testing.md                                ✏️ -Dsolovtt.campaigns.path
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `@QuarkusTest`, `CampaignStore` en mémoire, `ScriptedDice` | aucune campagne ouverte au démarrage | api |
| Happy path   | `POST /api/campaigns {name:"Phandelver"}` | 201, `{id, name}` ; `GET /api/campaigns` la liste | api |
| Happy path   | `POST /api/campaigns/{id}/open` sur une campagne neuve | 200 ; `GET /api/scene` 409 (aucune carte), `GET /api/maps` vide, `GET /api/campaigns/current` rend la campagne | api |
| Happy path   | campagne A : carte, poses, carte enregistrée, combat, attaque jusqu'à Bouclier ; ouvrir B puis rouvrir A | A : même scène, PV, combat, tour, journal, décision en attente ; ses cartes seulement. B : ni carte ni carte enregistrée | api |
| Happy path   | session neuve sur le même `CampaignStore` (redémarrage simulé, test unitaire de session), ouvrir A | partie de A à l'identique | system |
| Edge case    | nom absent, vide, blanc ou > 80 caractères | 400 | api |
| Edge case    | `open` sur un id inconnu | 404 | api |
| Edge case    | sans campagne ouverte : `POST /api/scene`, `GET /api/maps`, `POST /api/maps` | 409 « Aucune campagne ouverte » | api |
| Edge case    | `GET /api/campaigns/current` sans campagne | 204 | api |
| Edge case    | ouvrir une autre campagne pendant une décision ou un combat | permis ; la décision reste gardée dans la campagne quittée | api |
| Edge case    | partie illisible dans une campagne à l'ouverture | ouverture réussie sans carte ; partie mise de côté (`.illisible`), comme aujourd'hui | system |
| Teardown     | aucune : stores en mémoire, chaque test ouvre sa propre campagne | — | system |

## Tasks to do

### `1)` La session tient la campagne ouverte

> Une seule session, qui change de campagne en rejouant sa partie.

1. Constructeur CDI `@Inject SceneSession(Dice, CampaignStore)` : aucune campagne ouverte, aucun rejeu.
2. `open(Campaign)` : `reset()`, prend `campaigns.games(id)` comme store, garde la campagne, puis `restore()` (rejeu, ou mise de côté si illisible).
3. Les constructeurs `SceneSession(Dice)` et `SceneSession(Dice, GameStore)` (tests, `ResumeGameTest.java:175`, `:240`, `:247`, `SceneWithoutMapTest.java:17`) restent : ils ouvrent directement une campagne sans nom sur `GameStore.none()` ou `store`, donc `create` y reste permis.
4. `campaign()` rend la campagne ouverte (`Optional`) ; `maps()` rend le `MapStore` de la campagne ouverte (`MapStore.inMemory()` pour la campagne sans nom des tests) ; `create`, `load` et `maps()` sans campagne ouverte lèvent `NoCampaignException` (409) ; les autres méthodes refusent déjà sans carte.

### `2)` Ressource des campagnes

> Lister, créer, ouvrir, savoir laquelle est ouverte.

1. `GET /api/campaigns` → `CampaignDto[]` triées par nom.
2. `POST /api/campaigns {name}` → 201 ; nom nettoyé (`strip`), vide → 400, plus de 80 caractères → 400 (mêmes règles que `MapResource.nameOf`, `:74`, et `save`, `:45`).
3. `POST /api/campaigns/{id}/open` → 200 `CampaignDto` ; inconnu → 404 ; appelle `session.open(campaign)`.
4. `GET /api/campaigns/current` → 200 ou 204.
5. `@APIResponse` sur chaque route.

### `3)` Cartes enregistrées par campagne

> Chaque campagne ne voit que ses cartes.

1. `MapResource(SceneSession)` prend `session.maps()` à chaque appel ; sans campagne → 409.
2. Supprimer `GameStoreProducer`, `MapStoreProducer` et les propriétés `solovtt.save.path`, `solovtt.maps.path` (la story suivante relira les anciens fichiers).

### `4)` Tests existants et nouveaux

> Le filet existant repasse, la story est couverte.

1. Aide `Campagnes.ouvrirUneNeuve()` (RestAssured : créer puis ouvrir) appelée en tête de chaque `@BeforeEach` des tests d'API qui créent une carte.
2. `SceneWithoutMapTest` : `MapResource` construit avec la seule session ; la carte « Marais » est enregistrée dans `session.maps()`.
3. `CampaignApiTest` : scénarios de la Test Scope côté API.
4. Test de session : deux campagnes sur un même `CampaignStore.inMemory()`, aller-retour, puis session neuve qui rouvre A.

### `5)` Mémoire du projet

> La doc suit l'architecture.

1. `architecture.md` : décision « campagne ouverte » (session unique, `open` rejoue, refus 409 sans campagne, dossier par campagne, images communes) ; piège : après redémarrage, aucune campagne n'est ouverte.
2. `codebase-map.md` : `Campaign`, `CampaignStore`, `CampaignResource`, `FileCampaignStore`, `CampaignStoreProducer`, routes `campaigns`, `campaigns/current`, `campaigns/{id}/open` ; retirer les deux producteurs.
3. `testing.md` : la commande du serveur de test passe `-Dsolovtt.campaigns.path=<scratchpad>/campagnes` à la place de `solovtt.save.path` et `solovtt.maps.path`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Deux campagnes sur la même session : l'aller-retour redonne scène, journal, combat et décision de A à l'identique ; session neuve qui rouvre A, idem. `ResumeGameTest` et `CombatDeBoutEnBoutTest` passent sans changement ; une session CDI sans campagne refuse `create` (409). |
| 2 | `CampaignApiTest` : 201, 400, 404, 204 et liste triée. |
| 3 | Une carte enregistrée dans A n'apparaît pas dans B ; sans campagne, `/api/maps` rend 409. |
| 4 | `mvn -q test` passe en entier. |
| 5 | Mémoire à jour, sans référence aux producteurs supprimés. |
