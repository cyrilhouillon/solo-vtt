---
status: pending
---

# Instruction: catalogue de campagne et pose d'un PJ

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
dnd-engine/src/
├── main/java/dnd/engine/catalog/SrdCatalog.java         ✏️ sans Guerrière ni Magicienne
└── test/java/dnd/engine/catalog/SrdCatalogTest.java     ✏️
dnd-server/src/
├── main/java/dnd/server/SheetCharacters.java            ✅ PlayerCharacters des fiches
├── main/java/dnd/server/SheetStore.java                 ✏️ ids des fiches gardées
└── test/java/dnd/server/SheetCharactersTest.java        ✅
vtt-server/src/
├── main/java/solovtt/server/
│   ├── CampaignCatalog.java      ✅ monstres + PJ des modules actifs
│   ├── CatalogResource.java      ✏️ sous-ressource de campagne
│   ├── CampaignResource.java     ✏️ localisateur catalog, catalogue passé à scene et maps
│   ├── SceneResource.java        ✏️ pose par CampaignCatalog
│   ├── MapResource.java          ✏️ chargement par CampaignCatalog
│   ├── SceneSession.java         ✏️ Place garde nom et profil ; rejeu sans fiche ; load(map, catalogue)
│   ├── SavedGame.java            ✏️ Place(catalogId, name, profile, x, y, rolls)
│   └── persistence/JsonFileGameStore.java ✏️ format de CreatureProfile et Weapon
└── test/java/solovtt/server/
    ├── TestCharacters.java       ✅ Guerrière et Magicienne servies en PJ de test
    ├── PlayerCharacterApiTest.java ✅
    ├── persistence/JsonFileGameStoreTest.java ✏️ fixtures avec profil
    ├── ResumeGameTest.java       ✏️ fixtures avec profil, TestCharacters.entry
    ├── CombatsEnregistresTest.java ✏️ Place avec profil, TestCharacters.entry
    ├── CampaignSessionsTest.java ✏️ TestCharacters.entry
    ├── CombatReplayTest.java     ✏️ TestCharacters.entry
    ├── CombatDeBoutEnBoutTest.java ✏️ TestCharacters.entry
    └── SceneApiTest.java         ✏️ catalogue de la campagne
vtt-web/src/
├── api.ts                        ✏️ catalog dans campaignApi
├── components/GameScreen.vue     ✏️ catalogue de la campagne
└── components/CatalogPanel.vue   ✏️ titre « Catalogue »
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Campagne avec D&D, fiche « Aria » guidée ; serveurs 8081 / 5174 | Fiche lisible | api, browser |
| Happy path   | `GET …/catalog` | Aria (Personnage) et les monstres ; ni Guerrière ni Magicienne | api |
| Happy path   | Poser Aria, lancer le combat, la déplacer, attaquer un Gobelin | Initiative tirée, déplacement limité à sa vitesse, attaque à son bonus et ses dégâts | api, browser |
| Happy path   | Après la pose : retoucher sa CA, la renommer, la mettre à la corbeille ; redémarrer ; rejouer le combat terminé | Créature posée inchangée (nom, CA, PV) en reprise et en rejeu | api |
| Edge case    | Module D&D inactif | Catalogue sans PJ ; poser son id : 404 ; un PJ déjà posé se rejoue | api |
| Edge case    | Poser une fiche à 0 PV max | 400 avec la raison | api |
| Edge case    | Carte enregistrée avec Aria, puis fiche supprimée, puis chargement | 409, carte en cours inchangée | api |
| Edge case    | Partie gardée qui pose `guerriere` sans profil | Rejeu en échec, partie mise de côté (accepté) | system |
| Teardown     | Arrêter 8081 puis 5174 | Ports libres | system |

## Tasks to do

### `1)` PJ des fiches

> Le module D&D sert ses fiches au catalogue.

1. `SheetStore` : ids des fiches gardées.
2. `SheetCharacters` (`@ApplicationScoped`, `PlayerCharacters`, module `dnd`) : `list` garde les entrées vivantes (`NotebookPages.isEntry`), titre par `pageTitle`, triées par nom ; `take` rejoue l'histoire et rend titre et `profile()`.
3. `SheetCharactersTest` : liste, entrée à la corbeille absente, copie aux valeurs du moment, fiche vierge refusée.

### `2)` Catalogue de campagne

> Le catalogue dépend de la campagne.

1. `CampaignCatalog` : monstres de `SrdCatalog` puis PJ des `PlayerCharacters` dont le module est actif (`InstalledModules.activeIn`) ; `entry(campaignId, id)` rend un monstre ou la copie du PJ.
2. `GET /api/campaigns/{id}/catalog` (localisateur), suppression de `/api/catalog` ; `@APIResponse` à jour.
3. `SceneResource.place` et `MapResource.load` résolvent par `CampaignCatalog` (404 inconnu, 400 fiche qui ne joue pas, 409 carte dont une créature manque).

### `3)` Copie gardée dans la partie

> Reprise et rejeu ne relisent jamais la fiche.

1. `SavedCommand.Place` gagne `name` et `profile` (`null` pour un monstre) ; `SceneSession.place` les écrit pour un PJ.
2. Rejeu : `profile` présent → `CatalogEntry.character` ; sinon `SrdCatalog`. `SceneSession.load` reçoit le résolveur du catalogue ; `catalogEntry` statique disparaît.
3. `JsonFileGameStore` : sérialiser `CreatureProfile` et `Weapon` sans propriété dérivée (`melee`) ; test aller-retour.

### `4)` Retrait des prétirés

> Seuls les monstres restent au catalogue SRD.

1. Retirer `guerriere` et `magicienne` de `SrdCatalog` ; adapter `SrdCatalogTest`.
2. `TestCharacters` (test de `vtt-server`, module `dnd`) sert ces deux prétirés en PJ sous les mêmes ids et profils (Duel, Bouclier, emplacements) : les tests d'API existants gardent leurs ids. Il expose aussi `entry(id)` en statique, pour les tests qui construisent `SceneSession` sans CDI et appelaient `SceneSession.catalogEntry` : `CampaignSessionsTest:24`, `CombatReplayTest:27`, `CombatDeBoutEnBoutTest:97`, `CombatsEnregistresTest:38`, `ResumeGameTest:51`.
3. Fixtures de partie : ajouter nom et profil aux `place` de PJ (`JsonFileGameStoreTest`, `ResumeGameTest`, `CombatsEnregistresTest:194`) ; `SceneApiTest:42` lit `…/catalog` de sa campagne.
4. `PlayerCharacterApiTest` : scénarios de la Test Scope côté API (copie figée après retouche, renommage et corbeille ; reprise ; rejeu ; module inactif ; carte enregistrée).

### `5)` Client

> L'écran de combat lit le catalogue de sa campagne.

1. `api.ts` : `catalog` passe dans `campaignApi` (`${base}/catalog`).
2. `GameScreen.vue` l'appelle ; `CatalogPanel.vue` titre « Catalogue ».

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `mvn -q test -pl dnd-server` passe ; la liste suit la corbeille ; la copie suit la fiche du moment |
| 2 | `GET …/catalog` rend les PJ seulement si D&D est actif ; `/api/catalog` rend 404 |
| 3 | Après retouche, renommage et suppression de la fiche, la reprise et le rejeu montrent le PJ tel que posé |
| 4 | `mvn -q verify` passe ; le catalogue SRD n'a plus que des monstres |
| 5 | `npm run build` passe ; dans le navigateur (5174), le PJ se glisse sur la carte, entre en combat, se déplace et attaque |
