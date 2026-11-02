---
status: done
---

# Instruction: Serveur : `POST /api/combat/end`

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-server/src/
├── main/java/solovtt/server/
│   ├── SceneSession.java                ✏️
│   └── CombatResource.java              ✏️
└── test/java/solovtt/server/CombatApiTest.java ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Nouvelle carte, créatures posées, `ScriptedDice`, combat lancé | 200, combat en cours | api |
| Happy path   | `POST /api/combat/end` | 200 ; `combat` null ; `entries` contient « Le combat se termine » ; `GET /api/combat` rend 204 ; jetons et PV inchangés dans `GET /api/scene` | api |
| Edge case    | `POST /api/combat/end` hors combat | 409 « Aucun combat en cours » | api |
| Edge case    | `POST /api/combat/end` pendant « Qui joue ? » | 409 « Une décision attend une réponse » | api |
| Edge case    | Sans carte | 409 | api |
| Edge case    | Après la fin, `POST /api/combat` | 200, nouveau combat | api |

## Tasks to do

### `1)` Session

> Terminer le combat passe par le moteur, comme la fin de tour.

1. Ajouter `SceneSession.endCombat()` sur le modèle d'`endTurn()` : `requireNoDecision()`, puis `run(refusable(() -> EndCombat.of(state)), null)`.
2. Ajouter la fin du combat à la Javadoc de classe, parmi les actions passées par le moteur et bloquées par une décision.

### `2)` Ressource

> Une route documentée comme les autres.

1. Ajouter `@POST @Path("/end")` dans `CombatResource`, avec `requireScene()` et les `@APIResponse` 200 et 409 (aucune carte, aucun combat, décision en attente).

### `3)` Tests API

> La story vue de l'API.

1. Ajouter dans `CombatApiTest` : `terminerLeCombat_plusDeCombat_lesCreaturesRestentAvecLeursPv`, et les refus (hors combat, décision en attente) dans `refus_sansCreature_dejaLance_horsCombat` et `uneDecisionEnAttente_bloqueLeCombatLaPoseEtLeRetrait`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | En combat, `endCombat()` rend l'entrée « Le combat se termine » ; ensuite `combat()` est vide. |
| 2 | `POST /api/combat/end` rend 200 avec `combat: null`, ou 409 avec la raison ; la route figure dans `/q/openapi`. |
| 3 | `mvn -q test` vert, nouveaux tests compris. |
