---
status: done
---

# Instruction: l'oracle générique répond oui/non et lance un d100

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── pom.xml ✏️
├── oracle-server/ ✅
│   ├── pom.xml ✅
│   └── src/
│       ├── main/
│       │   ├── java/oracle/server/
│       │   │   ├── OracleModule.java ✅
│       │   │   ├── OracleResource.java ✅
│       │   │   └── OracleDtos.java ✅
│       │   └── resources/META-INF/beans.xml ✅
│       └── test/java/oracle/server/
│           └── OracleResourceTest.java ✅
├── vtt-server/
│   ├── pom.xml ✏️
│   └── src/test/java/solovtt/server/CampaignApiTest.java ✏️
└── vtt-web/
    └── modules/oracle/ ✅
        ├── index.ts ✅
        ├── api.ts ✅
        ├── result.ts ✅
        └── OraclePanel.vue ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `OracleResource` sur `ModuleData` en mémoire, `Dice.scripted` | Aucun hasard réel | system |
| Happy path   | `yesNo("Le pont tient ?")`, d100 = 50 | Oui, jet 50, question gardée | system |
| Happy path   | `yesNo(null)`, d100 = 51 | Non, jet 51, question `null` | system |
| Happy path   | `d100()`, d100 = 100 | Jet 100 | system |
| Happy path   | Carnet, page ouverte : « Le pont tient ? » puis Oui/non | Citation « Oracle : Le pont tient ? » / « d100 : 37 → Oui » au curseur | browser |
| Happy path   | Lancer un d100 | Citation « d100 : 82 » | browser |
| Edge case    | Question de plus de 500 caractères | 400 | system |
| Edge case    | Campagne inconnue | 404 | system |
| Edge case    | Aucune page ouverte | Refus avec la raison dans le panneau, aucun appel | browser |
| Edge case    | Page verrouillée | Citation à la fin, page toujours verrouillée | browser |
| Edge case    | Campagne sans le module Oracle | Pas de panneau ; `POST …/oracle/yes-no` 404 | api |
| Teardown     | Arrêter 8081 puis 5174 | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌──────────────────────────────┐
│ (1) Oracle                    │
│ (2) Question                  │
│   [_________________________] │
│   [Oui/non]   [d100]          │
│ (3) Dernier résultat          │
│   Le pont tient ? → Oui (37)  │
│ (4) message d'erreur          │
└──────────────────────────────┘
```

1. Titre du panneau, dans la colonne des modules, sous ou sur Mythic selon l'ordre des ids.
2. Question facultative (500 caractères), et les deux jets.
3. Rappel du dernier jet, comme le panneau Mythic.
4. Raison d'un refus ou d'un échec du serveur.

## Tasks to do

### `1)` Le module Maven

> `oracle-server`, construit comme `mythic-server`, embarqué par le serveur.

1. `oracle-server/pom.xml` (groupId `oracle`, dépendances `vtt-module-api`, `vtt-engine`, Quarkus et Jackson `provided`, JUnit) ; `beans.xml` vide.
2. `pom.xml` racine : `<module>` avant `vtt-server`, `dependencyManagement` ; `vtt-server/pom.xml` : dépendance.
3. `OracleModule` (`InstalledModule`, id `oracle`).

### `2)` Les jets

> Oui/non à 50/50 et d100, tirés au serveur.

1. `OracleResource` (`/api/campaigns/{campaignId}/oracle`) : `POST yes-no {question}` → `{question, roll, yes}` (oui si `roll ≤ 50`) ; `POST d100 {question}` → `{question, roll}` ; 404 campagne inconnue (`ModuleData.exists`), 400 question trop longue.
2. `OracleResourceTest` : lignes system du Test Scope ; `CampaignApiTest` : route 404 si Oracle inactif.

### `3)` Le panneau

> Module client `oracle`, écrit dans la page ouverte.

1. `vtt-web/modules/oracle/index.ts` (`id: 'oracle'`, `title: 'Oracle'`, `panel`), `api.ts` (`request`), `result.ts` (citation en nœuds standard).
2. `OraclePanel.vue` : refus sans page avant tout appel (`context.hasOpenPage`), écrit par `writeToOpenPage`, dernier résultat, erreur dans le panneau.
3. `tsconfig.app.json` inclut déjà `modules/**` : rien à changer.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `mvn -q verify` construit `oracle-server` ; `InstalledModules.ids()` rend `["mythic", "oracle"]`. |
| 2 | Les lignes system et api du Test Scope passent. |
| 3 | Sur 5174 : les lignes browser du Test Scope ; `npm run build` vert. |
