---
status: done
---

# Instruction: le cœur garde les données d'un module

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── pom.xml ✏️
├── aidd_docs/memory/
│   ├── architecture.md ✏️
│   ├── codebase-map.md ✏️
│   └── package.md ✏️
├── vtt-module-api/
│   ├── pom.xml ✅
│   └── src/main/java/solovtt/module/ModuleData.java ✅
└── vtt-server/
    ├── pom.xml ✏️
    └── src/
        ├── main/java/solovtt/server/
        │   ├── CampaignStore.java ✏️
        │   ├── ModuleDataAdapter.java ✅
        │   └── persistence/FileCampaignStore.java ✏️
        └── test/java/solovtt/server/
            ├── CampaignStoreTest.java ✏️
            └── persistence/FileCampaignStoreTest.java ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Une campagne créée dans `CampaignStore.inMemory()` et dans `FileCampaignStore` sur `@TempDir` | Aucune donnée de module | system |
| Happy path   | `writeModuleData(id, "mythic", "{\"chaos\":6}")` puis `readModuleData(id, "mythic")` | Rend le même JSON ; fichier `<campagne>/modules/mythic.json` | system |
| Happy path   | Nouvel adaptateur sur le même dossier, puis lecture | Le JSON survit | system |
| Edge case    | Lecture sans écriture préalable | `Optional.empty()` | system |
| Edge case    | Deux campagnes écrivent sous le même module | Chacune relit le sien | system |
| Edge case    | Campagne inconnue : `exists`, lecture, écriture | `false` ; `IllegalArgumentException` | system |
| Edge case    | `moduleId` hors de `[a-z0-9-]+` (`../x`, vide) | `IllegalArgumentException`, rien n'est écrit | system |
| Teardown     | `@TempDir` effacé | Rien sur le disque du joueur | system |

## Tasks to do

### `1)` L'API des modules

> Un module Maven sans dépendance décrit ce que le cœur offre à un module.

1. `vtt-module-api/pom.xml` (`solovtt:vtt-module-api`, aucune dépendance) ; `<module>` dans le `pom.xml` racine, avant `vtt-server`, et entrée de `dependencyManagement`.
2. `solovtt.module.ModuleData` : `exists(campaignId)`, `read(campaignId, moduleId)` → `Optional<String>`, `write(campaignId, moduleId, json)` ; javadoc du contrat (JSON opaque, une donnée par campagne et module, campagne inconnue → `IllegalArgumentException`).

### `2)` Le cœur sert ce port

> Les données d'un module vivent dans le dossier de sa campagne.

1. `CampaignStore` : `readModuleData(id, moduleId)` et `writeModuleData(id, moduleId, json)` ; `inMemory` les garde dans son `Content`.
2. `FileCampaignStore` : `<campagne>/modules/<moduleId>.json`, écriture atomique (`.tmp` puis `ATOMIC_MOVE`), `moduleId` validé `[a-z0-9-]+` avant tout chemin.
3. `ModuleDataAdapter` (`@ApplicationScoped`, implémente `ModuleData`) délègue à `CampaignStore` ; `vtt-server/pom.xml` dépend de `vtt-module-api`.
4. Tests dans `CampaignStoreTest` et `FileCampaignStoreTest`.

### `3)` Mémoire

> Le point d'extension serveur est décrit.

1. `architecture.md` : port `ModuleData`, fichier `modules/<id>.json`, règle « seuls les assemblages nomment un module ».
2. `codebase-map.md` et `package.md` : `vtt-module-api`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `mvn -q compile` vert ; `vtt-module-api` n'a aucune dépendance (`mvn dependency:tree -pl vtt-module-api`). |
| 2 | `mvn -q test -pl vtt-server` vert sur les scénarios ci-dessus ; un `moduleId` malveillant n'écrit rien hors du dossier. |
| 3 | Les trois fichiers de mémoire nomment `vtt-module-api` et `ModuleData`. |
