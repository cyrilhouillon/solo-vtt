---
status: in-progress
---

# Instruction: garder les campagnes

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── vtt-server/src/
    ├── main/java/solovtt/server/
    │   ├── Campaign.java                         ✅ record (id, name)
    │   ├── CampaignStore.java                    ✅ port : list, create, find, games, maps ; inMemory()
    │   ├── GameStore.java                        ✏️ inMemory() à côté de none()
    │   └── persistence/
    │       ├── FileCampaignStore.java            ✅ un dossier par campagne
    │       └── CampaignStoreProducer.java        ✅ propriété solovtt.campaigns.path
    ├── main/resources/application.properties     ✏️ solovtt.campaigns.path (vide en test)
    └── test/java/solovtt/server/
        ├── CampaignStoreTest.java                ✅ inMemory
        └── persistence/FileCampaignStoreTest.java ✅ sur @TempDir
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `FileCampaignStore` sur un `@TempDir` vide | `list()` vide | system |
| Happy path   | `create("Phandelver")` puis `create("Strahd")` | deux campagnes, ids distincts, listées par nom | system |
| Happy path   | sauver une partie par `games(id)` et une carte par `maps(id)`, puis relire avec un nouveau `FileCampaignStore` sur le même dossier | même liste ; la partie et la carte reviennent pour cette campagne seulement | system |
| Edge case    | `maps(autre id)` | vide : les cartes ne fuient pas d'une campagne à l'autre | system |
| Edge case    | `find("inconnu")` | vide | system |
| Edge case    | un dossier sans `campagne.json` ou illisible dans le répertoire | ignoré par `list()`, jamais écrasé | system |
| Edge case    | deux campagnes de même nom | toutes deux listées, ids distincts | system |
| Teardown     | `@TempDir` supprimé par JUnit | aucun fichier hors du dossier temporaire | system |

## Tasks to do

### `1)` Port des campagnes

> La session ne connaît les campagnes que par un port.

1. `Campaign(String id, String name)`.
2. `CampaignStore` : `List<Campaign> list()` (triée par nom, insensible à la casse, comme `MapStore.BY_NAME`), `Campaign create(String name)`, `Optional<Campaign> find(String id)`, `GameStore games(String id)`, `MapStore maps(String id)`.
3. `CampaignStore.inMemory()` : un `GameStore.inMemory()` et un `MapStore.inMemory()` par campagne, gardés le temps du processus.
4. `GameStore.inMemory()` : garde la dernière partie ; `setAside` l'oublie. Il sert le profil de test de l'application ; `InMemoryGameStore` (test, avec ses compteurs et sa partie illisible) reste pour les tests de reprise.

### `2)` Adaptateur fichier

> Chaque campagne vit dans son dossier.

1. `FileCampaignStore(Path root)` : `create` écrit `<root>/<uuid>/campagne.json` (`{id, name}`) par fichier voisin et déplacement atomique.
2. `list` lit chaque `<root>/*/campagne.json` ; un dossier sans fichier lisible est journalisé et ignoré.
3. `games(id)` = `new JsonFileGameStore(<root>/<id>/partie.json)`, `maps(id)` = `new JsonFileMapStore(<root>/<id>/cartes.json)` ; `id` inconnu → `IllegalArgumentException` (aucun chemin construit depuis une saisie non vérifiée).
4. `CampaignStoreProducer` : `solovtt.campaigns.path` → `FileCampaignStore`, vide → `CampaignStore.inMemory()`.
5. `application.properties` : `solovtt.campaigns.path=${user.home}/.solo-vtt/campagnes`, `%test.solovtt.campaigns.path=`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `CampaignStoreTest` : créer, lister par nom, trouver, partie et cartes séparées par campagne. |
| 2 | `FileCampaignStoreTest` : relecture après un nouveau store sur le même dossier, isolation des cartes, dossier illisible ignoré, id inconnu refusé. `mvn -q test` passe. |
