---
status: in-progress
---

# Instruction: Serveur : combat enregistré, entrée du Carnet, reprise

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-server/src/main/java/solovtt/server/
├── SavedGame.java                         ✏️ StartCombat(combatId, rolls)
├── SceneSession.java                      ✏️ combat courant, écriture du combat, reprise
├── CombatStore.java                       ✅ port : la SavedGame de chaque combat
├── CampaignStore.java                     ✏️ combats(id), en mémoire
├── CampaignSessions.java                  ✏️ branche combats et entrée du Carnet
├── Page.java                              ✏️ combatId (null : page ordinaire)
├── NotebookStore.java                     ✏️ createCombatEntry, refus du contenu d'un combat
├── Dtos.java                              ✏️ combatId dans PageSummaryDto, PageDto, SearchResultDto ; CombatEntryDto
├── NotebookResource.java                  ✏️ contenu d'une entrée combat : 400
├── CombatsResource.java                   ✅ GET /combats/{id}, POST /combats/{id}/resume
├── CampaignResource.java                  ✏️ localisateur /{campaignId}/combats
└── persistence/
    ├── FileCampaignStore.java             ✏️ combats/<id>.json
    ├── JsonFileCombatStore.java           ✅ un JsonFileGameStore par combat
    ├── JsonFileGameStore.java             ✏️ (format startCombat inchangé, combatId facultatif)
    └── JsonFileNotebookStore.java         ✏️ combatId dans la fiche (NON_NULL)
vtt-server/src/test/java/solovtt/server/
├── CombatsApiTest.java                    ✅
├── ResumeGameTest.java                    ✏️ partie d'avant sans combatId
├── NotebookStoreTest.java                 ✏️ entrée combat
└── persistence/JsonFileNotebookStoreTest.java ✏️ combatId relu
```

## Test Scope

| Scenario | Action | Observable outcome | Actor |
| -------- | ------ | ------------------ | ----- |
| Setup | Campagne neuve, carte, deux créatures posées, dés scriptés | Partie prête, aucun combat | api |
| Happy path | `POST …/combat` | Une page de `combatId` C, titre « Combat 1 », sous la racine de section `combats` « Combats » ; `combats/C.json` contient les commandes jusqu'au lancement | api |
| Happy path | Attaque puis `POST …/combat/end` | Le fichier du combat se termine par `endCombat` ; `GET …/combats/{id}` rend `ended: true` et la scène finale | api |
| Happy path | Combat A lancé, `POST …/scene` (nouvelle carte), puis `POST …/combats/A/resume` | La partie courante redevient A, en cours, round et PV d'avant ; `GET …/combats/A` rend `current: true` | api |
| Edge case | Second combat sur la même carte après la fin du premier | « Combat 2 », son fichier contient tout depuis la création de la carte ; le fichier de A ne change plus | api |
| Edge case | `resume` d'un combat terminé, ou illisible | 409, partie courante inchangée | api |
| Edge case | « Nouvelle carte » puis « Charger une carte » pendant un combat | Le fichier du combat garde toutes ses commandes ; l'entrée reste « en cours » | api |
| Edge case | Lancer refusé (combat déjà lancé) | 409 ; les commandes suivantes vont toujours au fichier du combat courant | api |
| Edge case | Après `combat/end` | `GET …/scene` rend la carte du combat, PV et positions gardés, sans combat ; une action suivante ne change plus le fichier du combat | api |
| Edge case | Créer ou déplacer une page sous une entrée combat | 400 | api |
| Edge case | `PUT …/notebook/pages/{idCombat}/content` | 400, rien n'est écrit | api |
| Edge case | `partie.json` d'avant, `startCombat` sans `combatId` | La partie se reprend ; aucune entrée créée | system |
| Edge case | Entrée combat déplacée, renommée, supprimée puis restaurée | Mêmes réponses qu'une page ; la racine « Combats » refuse déplacement et suppression | api |
| Edge case | Combat inconnu `GET …/combats/xyz` | 404 | api |
| Teardown | Campagne de test jetée (mémoire) | Rien sur disque | system |

## Tasks to do

### `1)` Garder chaque combat

> Le combat lancé a un id, gardé dans sa commande, et son propre fichier écrit à chaque commande jusqu'à sa fin.

1. `SavedCommand.StartCombat(String combatId, List<Integer> rolls)` ; `combatId` `null` pour une partie d'avant.
2. Port `CombatStore` : `GameStore game(String combatId)`, `List<String> ids()` ; `inMemory()`.
3. `CampaignStore.combats(id)` ; `FileCampaignStore` → `JsonFileCombatStore` sur `<campagne>/combats/<id>.json` (id UUID vérifié, jamais un chemin).
4. `SceneSession` : champ `combatId` ; `startCombat` tire un UUID (ou reprend celui de la commande au rejeu) dans une variable locale, et ne l'affecte qu'une fois le lancement réussi, avant `record` : un lancement refusé ne touche pas le combat courant.
5. `record` écrit aussi `combats.game(combatId)` tant qu'un combat est en cours, hors rejeu ; `endCombat` l'écrit puis remet `combatId` à `null`.
6. `create` (donc « Nouvelle carte » et « Charger une carte ») et `reset` remettent `combatId` à `null` **avant** `commands.clear()` et `record(Create)` : le fichier du combat en cours reste tel quel.
7. Après un lancement réussi hors rejeu, appeler le rappel `combatStarted(combatId)` fourni par `CampaignSessions`.

### `2)` L'entrée du Carnet

> Un lancement crée « Combat N » dans la section du cœur « Combats ».

1. `Page.combatId` (`null` : page ordinaire), constructeurs existants inchangés ; `isCombatEntry()` ; `withTitle`, `withParent`, `withContent` le reportent ; `PageFile` de `JsonFileNotebookStore` le lit et l'écrit (`NON_NULL`) à chaque lecture (`stored`, `read`) et écriture.
2. `NotebookStore.createCombatEntry(combatId, title)`, dans `inMemory` et `JsonFileNotebookStore` : crée la racine de section `combats` « Combats » si aucune n'est vivante, puis la page qui porte `combatId`, avec son propre id (UUID), sans contenu, sous elle.
3. Refus serveur (400) : `saveContent` d'une entrée combat ; `create` et `move` (`checkMove`) sous un parent entrée combat.
4. `CampaignSessions` passe à `SceneSession` le `CombatStore` et un rappel qui nomme `"Combat " + combats.ids().size()` et crée l'entrée.
5. `combatId` dans `PageSummaryDto`, `PageDto`, `SearchResultDto`.

### `3)` Lire et reprendre un combat

> Le Carnet lit l'état d'un combat ; un combat en cours se reprend.

1. `SceneSession.replayed(SavedGame)` (fabrique statique, sans store ni rappel) : rejoue et lève si le rejeu échoue.
2. `CombatsResource` sous `/api/campaigns/{id}/combats` : `GET /{combatId}` → `CombatEntryDto { id, ended, current, scene }` (scène finale par `replayed`) ; 404 inconnu, 409 illisible.
3. `POST /{combatId}/resume` : valide d'abord le combat par `replayed` (409 si illisible ou terminé, la partie courante intacte), puis `SceneSession.resume(SavedGame)` remplace la partie courante (reset, rejeu, sauvegarde de `partie.json`) ; sans effet s'il est déjà courant.
4. `@APIResponse` sur chaque route.

## Test acceptance criteria

| Task | Acceptance criteria |
| ---- | ------------------- |
| 1 | Après lancement, attaque et fin, `combats/<id>.json` rejoué redonne la scène et les PV finals ; une partie d'avant sans `combatId` se reprend |
| 2 | Le lancement crée « Combat N » portant `combatId` sous « Combats » ; écrire son contenu rend 400 ; renommer, déplacer, supprimer et restaurer marchent comme pour une page |
| 3 | `GET …/combats/{id}` rend état et scène finale ; `resume` d'un combat en cours le remet en partie courante, 409 s'il est terminé, 404 s'il est inconnu |
