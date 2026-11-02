---
status: done
---

# Instruction: Serveur : ressource de déplacement et vitesse restante

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-server/src/
├── main/java/solovtt/server/
│   ├── SceneSession.java     ✏️ move(tokenId, x, y) : Move pour l'active en combat, sinon libre
│   ├── SceneResource.java    ✏️ POST /api/scene/tokens/{id}/move
│   └── Dtos.java             ✏️ MoveTokenRequest ; CombatDto.movementLeft (unité de la carte)
└── test/java/solovtt/server/
    └── MoveApiTest.java      ✅ déplacement par l'API
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `@QuarkusTest`, `ScriptedDice` ; carte 30 × 20 m, Guerrière, Gobelin guerrier, Squelette posés (comme `CombatApiTest`) | Scène prête | api |
| Happy path   | Hors combat, déplacer le Squelette de 20 m | 200, entrées vides, `GET /api/scene` donne la nouvelle position | api |
| Happy path   | En combat, déplacer l'active de 6 m | 200, une entrée « … se déplace de 6 m (reste 3 m) », `combat.movementLeft` = 3 | api |
| Happy path   | En combat, déplacer une créature non active de 20 m | 200, entrées vides, position changée, `movementLeft` de l'active inchangé | api |
| Edge case    | L'active au-delà de sa vitesse restante | 400 avec la raison, position inchangée, journal inchangé | api |
| Edge case    | Position hors de la carte | 400, position inchangée, aucun événement | api |
| Edge case    | Jeton inconnu | 404 | api |
| Edge case    | Décision en attente (« Qui joue ? ») | 409 | api |
| Edge case    | Fin du tour puis retour de la créature | `movementLeft` égal à sa vitesse | api |
| Edge case    | Carte en pieds | `movementLeft` en ft (30 pour 9 m) | api |

## Tasks to do

### `1)` Déplacer dans la session

> La session applique la règle D&D à l'active et repositionne librement les autres.

1. `SceneSession.move(tokenId, x, y)` : `requireNoDecision` ; distance = départ → arrivée dans l'unité de la carte.
2. Si `Move.limits` : `Move.of` (refus sans effet), vérifier les bornes, `scene.move`, puis `run(move, null)` ; sinon `scene.move` seul et un `ActionOutcome` vide.
3. Ordre garanti : aucune émission ni déplacement si l'un des refus tombe.

### `2)` Exposer la route

> `POST /api/scene/tokens/{id}/move` rend un `ActionResultDto`.

1. `Dtos.MoveTokenRequest(x, y)` ; requête incomplète → 400 ; jeton inconnu vérifié par `scene.contains` avant la session → 404 (comme `remove`), sinon `Scene` lèverait un 400.
2. `@APIResponse` 200, 400 (règles, bornes), 404 (jeton), 409 (aucune carte, décision).

### `3)` Vitesse restante dans le combat

> Le client lit la vitesse restante de l'active sans calcul.

1. `CombatDto.movementLeft` : `Move.remaining` converti dans l'unité de la carte ; `null` veut dire « aucune limite » (pas de créature active, ou système sans vitesse limitée), jamais 0.

### `4)` Tests d'API

> `MoveApiTest` couvre le Test Scope.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1    | Hors combat ou créature non active : position changée, journal inchangé ; active : événement dans le journal. |
| 2    | Les codes 200, 400, 404 et 409 suivent le Test Scope ; un refus laisse la position. |
| 3    | `movementLeft` baisse avec chaque déplacement de l'active et revient plein à son tour suivant, dans l'unité de la carte. |
| 4    | `mvn -q test` passe. |
