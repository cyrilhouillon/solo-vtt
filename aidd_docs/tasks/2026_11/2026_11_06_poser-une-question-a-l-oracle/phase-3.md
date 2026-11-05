---
status: pending
---

# Instruction: le serveur Mythic tient le chaos et pose la question

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── pom.xml ✏️
├── aidd_docs/memory/
│   ├── architecture.md ✏️
│   ├── codebase-map.md ✏️
│   └── testing.md ✏️
├── mythic-server/
│   ├── pom.xml ✅
│   └── src/
│       ├── main/
│       │   ├── java/mythic/server/
│       │   │   ├── MythicResource.java ✅
│       │   │   └── MythicDtos.java ✅
│       │   └── resources/META-INF/beans.xml ✅
│       └── test/java/mythic/server/MythicResourceTest.java ✅
└── vtt-server/pom.xml ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `MythicResource` sur un `ModuleData` en mémoire et `Dice.scripted` ; serveur de test sur 8081, campagnes dans le scratchpad | Une campagne neuve | system |
| Happy path   | `GET …/mythic/chaos` sur une campagne neuve | `{ "chaos": 5 }` | api |
| Happy path   | `PUT …/mythic/chaos` `{ "chaos": 7 }`, puis `GET` | 7 ; `<campagne>/modules/mythic.json` écrit | api |
| Happy path   | `POST …/mythic/questions` `{ "odds": "LIKELY" }`, d100 scripté | `roll`, `answer` (code et libellé), `odds` (libellé), `chaos`, `randomEvent` `null` | api |
| Happy path   | Jet double ≤ chaos | `randomEvent.focus` (code et libellé) | api |
| Happy path   | Redémarrer le serveur 8081, `GET …/mythic/chaos` | 7 | api |
| Edge case    | Deux campagnes | Chaque chaos est le sien | api |
| Edge case    | Chaos 0 ou 10 ; `odds` absent ou inconnu | 400 avec la raison, rien n'est écrit | api |
| Edge case    | Campagne inconnue | 404 | api |
| Edge case    | `mythic-server` retiré de `vtt-server/pom.xml` | `…/mythic/chaos` 404, tests `vtt-server` verts | system |
| Edge case    | `grep -ri mythic vtt-server/src vtt-web/src` | Aucun résultat | system |
| Teardown     | Arrêter le processus sur 8081 (jamais 8080) ; scratchpad | Données du joueur intactes | system |

## Tasks to do

### `1)` Le jar Mythic du serveur

> Un jar découvert par Quarkus sert l'oracle sous la campagne.

1. `mythic-server/pom.xml` (`mythic:mythic-server`) : `vtt-module-api`, `mythic-engine`, `quarkus-rest-jackson` et `quarkus-arc` du BOM Quarkus 3.40.1.1 (`provided`), JUnit 6 comme `vtt-server` ; `META-INF/beans.xml` vide.
2. `<module>` dans le `pom.xml` racine (avant `vtt-server`) et dépendance de `vtt-server/pom.xml` : les deux seuls endroits qui nomment le module.

### `2)` Chaos et question

> Le chaos de chaque campagne se garde ; la question rend le résultat complet.

1. `MythicResource` `@Path("/api/campaigns/{campaignId}/mythic")`, injecte `ModuleData` et `Dice` : `GET /chaos`, `PUT /chaos`, `POST /questions` (`odds`, `question` facultatif renvoyé tel quel) ; chaos lu dans le JSON du module (`{ "chaos": n }`, 5 sans donnée) ; campagne inconnue `NotFoundException` ; chaos ou probabilité invalides `IllegalArgumentException` (400) ; `@APIResponse` sur chaque route.
2. `MythicDtos` : `ChaosDto`, `QuestionRequest`, `AnswerDto` (`roll`, `odds` et libellé, `answer` et libellé, `chaos`, `randomEvent` facultatif avec focus et libellé) ; `GET /odds` rend les probabilités dans l'ordre, avec libellés, pour que le client ne les recopie pas.
3. `MythicResourceTest` : JUnit sans Quarkus sur la ressource, `ModuleData` en mémoire, dés scriptés.

### `3)` Vérifier l'assemblage

> La ressource du jar est servie par l'application, sans que le cœur la nomme.

1. `mvn -q install -DskipTests`, serveur de test sur 8081 (options de `testing.md`) ; scénarios API ci-dessus par `curl`, dont le redémarrage.
2. `architecture.md` (module serveur Mythic, route, fichier `modules/mythic.json`), `codebase-map.md` (`mythic-server`, route `mythic`), `testing.md` (tests de `mythic-server`).

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `mvn -q verify` vert ; `mythic` n'apparaît pas dans `vtt-server/src`. |
| 2 | `mvn -q test -pl mythic-server -am` vert sur chaos par défaut, modifié, borné, question, événement, campagne inconnue. |
| 3 | Sur 8081 : `GET …/mythic/chaos` 200, `POST …/mythic/questions` 200, chaos retrouvé après redémarrage, 404 sur campagne inconnue. |
