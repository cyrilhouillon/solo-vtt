---
status: done
---

# Instruction: le cœur garde les données communes d'un module

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── aidd_docs/memory/
│   ├── architecture.md ✏️
│   ├── codebase-map.md ✏️
│   └── testing.md ✏️
├── vtt-module-api/src/main/java/solovtt/module/
│   └── CommonModuleData.java ✅
└── vtt-server/src/
    ├── main/java/solovtt/server/persistence/
    │   ├── FileCommonModuleData.java ✅
    │   └── CommonModuleDataProducer.java ✅
    ├── main/resources/application.properties ✏️
    └── test/java/solovtt/server/persistence/
        └── FileCommonModuleDataTest.java ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `@TempDir` comme racine | Dossier vide | system |
| Happy path   | `write("mythic", json)` puis `read("mythic")` | Même texte ; fichier `<racine>/mythic.json` | system |
| Happy path   | Nouvel adaptateur sur la même racine | Le document se relit (survit au redémarrage) | system |
| Edge case    | `read` d'un module jamais écrit | `Optional.empty()` | system |
| Edge case    | Id `../x` ou `A` | `IllegalArgumentException` | system |
| Edge case    | Profil de test (propriété vide) | Adaptateur en mémoire, aucun fichier | system |

## Tasks to do

### `1)` Le port commun

> Un module garde un document commun à toutes les campagnes.

1. `CommonModuleData` : `Optional<String> read(String moduleId)`, `void write(String moduleId, String json)` ; Javadoc : commun aux campagnes, id en minuscules, chiffres et tirets.

### `2)` L'adaptateur du cœur

> Le cœur le sert sur disque, en mémoire en test.

1. `FileCommonModuleData(Path root)` : `<root>/<id>.json`, id vérifié par `CampaignStore.checkModuleId`, écriture atomique (`.tmp` puis `ATOMIC_MOVE`), `synchronized`, comme `FileCampaignStore.writeModuleData`.
2. `CommonModuleDataProducer` : `@Produces @Singleton`, `solovtt.modules.path` vide → mémoire (`ConcurrentHashMap`, id vérifié).
3. `application.properties` : `solovtt.modules.path=${user.home}/.solo-vtt/modules`, `%test.solovtt.modules.path=`.
4. `FileCommonModuleDataTest` selon le Test Scope.

### `3)` Mémoire

> La doc dit le port et l'option des serveurs de test.

1. `architecture.md` : port `CommonModuleData`, dossier `solovtt.modules.path`.
2. `codebase-map.md` : `vtt-module-api` (`CommonModuleData`), adaptateur et producteur.
3. `testing.md` : ajouter `-Dsolovtt.modules.path=<scratchpad>/modules` à la commande du serveur 8081.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `vtt-module-api` compile sans dépendance ; `ModuleData` inchangé. |
| 2 | `mvn -q test -pl vtt-server -am` vert ; `FileCommonModuleDataTest` couvre écriture, relecture après redémarrage, absence et id invalide. |
| 3 | La mémoire décrit le port et la commande de test complète. |
