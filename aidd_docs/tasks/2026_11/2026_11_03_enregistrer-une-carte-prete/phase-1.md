---
status: done
---

# Instruction: garder les cartes enregistrées

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── vtt-server/src/main/java/solovtt/server/
│   ├── SavedMap.java                          ✅ nom, largeur, hauteur, unité, imageId, gridVisible, jetons (catalogId, x, y)
│   ├── MapStore.java                          ✅ port : list, find(nom), save ; inMemory()
│   └── persistence/
│       ├── JsonFileMapStore.java              ✅ toutes les cartes dans un fichier JSON, écriture atomique
│       └── MapStoreProducer.java              ✅ solovtt.maps.path, mémoire si vide
├── vtt-server/src/main/resources/application.properties   ✏️ solovtt.maps.path, vide en %test
├── vtt-server/src/test/java/solovtt/server/persistence/JsonFileMapStoreTest.java  ✅
└── aidd_docs/memory/testing.md                ✏️ option -Dsolovtt.maps.path du serveur de test
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `JsonFileMapStore` sur un `@TempDir` | aucun fichier, `list()` vide | system |
| Happy path   | `save` de deux cartes puis nouvel adaptateur sur le même fichier | `list()` rend les deux, triées par nom ; `find` rend chacune à l'identique | system |
| Edge case    | `save` d'une carte au nom déjà pris | une seule carte de ce nom, la dernière | system |
| Edge case    | carte sans image (`imageId` null) et grille masquée | relue à l'identique | system |
| Edge case    | fichier présent mais illisible | `list` et `find` lèvent une exception ; `save` ne l'écrase pas | system |
| Edge case    | dossier parent absent | `save` le crée | system |

## Tasks to do

### `1)` Modèle et port

> Décrire une carte enregistrée et son stockage, sans support.

1. `SavedMap(String name, double width, double height, DistanceUnit unit, String imageId, boolean gridVisible, List<SavedToken> tokens)`, `SavedToken(String catalogId, double x, double y)` ; listes copiées.
2. `MapStore` : `List<SavedMap> list()` (triée par nom), `Optional<SavedMap> find(String name)`, `void save(SavedMap map)` (remplace le même nom) ; `static MapStore inMemory()`.

### `2)` Adaptateur fichier

> Garder les cartes entre deux lancements.

1. `JsonFileMapStore(Path file)` : lit la liste entière, écrit la liste entière par fichier voisin `.tmp` puis `ATOMIC_MOVE`, comme `JsonFileGameStore`.
2. Fichier illisible : `UncheckedIOException` avec le chemin, à la lecture comme avant une écriture (jamais écrasé).
3. `MapStoreProducer` : `solovtt.maps.path` non vide → fichier, sinon `inMemory()`.
4. `application.properties` : `solovtt.maps.path=${user.home}/.solo-vtt/cartes.json`, `%test.solovtt.maps.path=`.

### `3)` Documentation

> Le serveur de test n'écrit pas les cartes du joueur.

1. `testing.md` : ajouter `-Dsolovtt.maps.path=<scratchpad>/cartes.json` à la commande du serveur de test.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `MapStore.inMemory()` remplace une carte de même nom et liste par nom. |
| 2 | `JsonFileMapStoreTest` passe : aller-retour, remplacement, illisible jamais écrasé, dossier créé. |
| 3 | `testing.md` donne la commande avec les trois chemins du scratchpad. |
