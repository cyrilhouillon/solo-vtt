---
status: pending
---

# Instruction: Serveur : ressource combat, pose et retrait en combat

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── vtt-server/src/
    ├── main/java/solovtt/server/
    │   ├── ActionRefusedException.java   ✅
    │   ├── AttackResource.java           ✏️
    │   ├── CombatResource.java           ✅
    │   ├── DecisionResource.java         ✏️
    │   ├── Dtos.java                     ✏️
    │   ├── ErrorMapper.java              ✏️
    │   ├── SceneResource.java            ✏️
    │   └── SceneSession.java             ✏️
    └── test/java/solovtt/server/
        └── CombatApiTest.java            ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | nouvelle carte 30 × 20 m, Guerrière, Gobelin guerrier, Squelette posés ; `ScriptedDice` | `GET /api/combat` → 204 | api |
| Happy path   | `POST /api/combat`, d20 10, 15, 4 | 200 : round 1, actif `gobelin-1`, groupes avec initiatives ; entrées sous un même numéro d'action | api |
| Happy path   | `POST /api/combat/end-turn` trois fois | actif `guerriere-1`, `squelette-1`, puis `gobelin-1` au round 2 | api |
| Happy path   | `GET /api/journal` | début du combat, chaque round, chaque tour, dans l'ordre | api |
| Edge case    | ex æquo complets au lancement | 200 avec la décision « Qui joue ? » ; `POST /api/decisions/{id}` avec un nom → 200, cette créature est active, et la cible est absente du résultat | api |
| Edge case    | en combat, attaque d'une créature inactive | 400 « Ce n'est pas le tour de … » | api |
| Edge case    | en combat, pose d'un Loup (d20 scripté) | 201 ; le Loup est dans l'ordre ; son initiative est dans le journal | api |
| Edge case    | en combat, retrait de la créature active | 204 ; elle a quitté l'ordre, et la suivante est active (ou « Qui joue ? » attend) | api |
| Edge case    | sans carte, sans créature, combat déjà lancé ; fin de tour hors combat | 409 avec la raison | api |
| Edge case    | une décision attend, puis lancer, terminer le tour, poser ou retirer | 409 (décision en attente) | api |
| Edge case    | `POST /api/scene` en combat | 201 ; `GET /api/combat` → 204 | api |
| Teardown     | `POST /api/scene` | session vierge pour le test suivant | api |

## Tasks to do

### `1)` Combat dans la session

> La session pilote le combat par le moteur, et y fait entrer ou sortir les créatures.

1. `SceneSession.startCombat()` : refuse pendant une décision (`DecisionPendingException`), sans créature ou combat en cours (`ActionRefusedException`, vérifiés avant de soumettre : une `IllegalStateException` sortirait en 500). Soumet `StartCombat` sous un nouveau numéro d'action et rend son résultat (décision comprise).
2. `SceneSession.endTurn()` : refuse pendant une décision, ou hors combat (`ActionRefusedException`) ; soumet `EndTurn`.
3. `place` en combat : crée la créature (comme aujourd'hui), puis soumet `JoinCombat` sous un nouveau numéro d'action.
4. `remove` en combat : soumet `LeaveCombat` avant de retirer jeton et créature.
5. `create` efface le combat (nouvel état, déjà le cas). Javadoc de la classe : combat, arrivées, départs, refus.

### `2)` Ressources REST

> `/api/combat` lit, lance et termine le tour ; la réponse à une décision n'a pas toujours de cible.

1. `ActionRefusedException` ; `ErrorMapper` la rend en 409 avec son message.
2. `Dtos` : `CombatDto(round, activeId, candidates, groups)`, avec `InitiativeGroupDto(initiative, members)` où chaque membre a `creatureId`, `name`, `played`. `AttackResultDto` devient `ActionResultDto(entries, target, decision, combat)` : `target` (id, PV, PV max) est `null` sans cible, `combat` est `null` hors combat.
3. `CombatResource` : vérifie la carte d'abord (`ClientErrorException` 409, comme `AttackResource`) ; `GET /api/combat` (200 ou 204), `POST /api/combat` (200), `POST /api/combat/end-turn` (200) ; `@APIResponse` pour chaque code.
4. `AttackResource` et `DecisionResource` rendent `ActionResultDto` ; `DecisionResource.answer` ne suppose plus de cible.
5. `SceneResource` : descriptions des codes de pose et retrait mises à jour (entrée et sortie de combat).

### `3)` Tests d'API

> Le parcours et chaque refus, contre l'API réelle.

1. `CombatApiTest` (`@QuarkusTest`, `ScriptedDice`) : les scénarios du Test Scope.
2. `AttackApiTest`, `DecisionApiTest` : chemins JSON adaptés à `ActionResultDto` si la forme de la cible change.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | En combat, une pose ajoute la créature à l'ordre ; un retrait l'en sort ; aucun refus ne sort en 500. |
| 2 | `/q/openapi` décrit les trois opérations de combat et leurs codes ; un 409 a le corps `ErrorDto`. |
| 3 | `mvn -q test` passe à la racine, `SceneApiTest`, `AttackApiTest` et `DecisionApiTest` compris. |
