---
status: pending
---

# Instruction: serveur — décision en attente, réponse et blocage

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── vtt-server/src/
    ├── main/java/solovtt/server/
    │   ├── SceneSession.java       ✏️ StandardRules.create() ; décision en attente et cible de l'action suspendue ; answer(...) ; refus pendant l'attente
    │   ├── DecisionPendingException.java ✅ refus d'une action pendant une décision (IllegalStateException)
    │   ├── DecisionResource.java   ✅ GET /api/decisions/pending ; POST /api/decisions/{id} {option}
    │   ├── AttackResource.java     ✏️ rend la décision posée par l'attaque
    │   ├── ErrorMapper.java        ✏️ DecisionPendingException → 409 avec la raison
    │   └── Dtos.java               ✏️ DecisionDto, AnswerRequest ; AttackResultDto.decision
    └── test/java/solovtt/server/
        └── DecisionApiTest.java    ✅ Bouclier lancé, refusé, blocage, reprise, erreurs
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | carte 30 × 20 m, Gobelin en (12 ; 1,5), Magicienne au contact en (12 ; 3), Guerrière en (1,5 ; 1,5) ; dés scriptés | scène prête | api |
| Happy path   | Gobelin, Cimeterre → Magicienne, d20 10 (14 contre CA 12, Bouclier ferait rater à 17) | 200 ; `decision` porte un id, `chooserId` `magicienne-1`, la question et les options « Lancer Bouclier » / « Ne rien faire » ; aucune entrée « subit » ; `targetHitPoints` 7 | api |
| Happy path   | `GET /api/decisions/pending` pendant l'attente | 200, la même décision | api |
| Happy path   | réponse « Lancer Bouclier » | 200 ; entrées « Magicienne utilise sa réaction : Bouclier » et « CA de Magicienne : +5 (Bouclier) → 17 », attaque ratée, `decision` null, PV 7/7 ; entrées au même numéro d'action que l'attaque | api |
| Happy path   | réponse « Ne rien faire », d20 10 puis d6 3 | 200 ; « Magicienne subit 5 dégâts tranchants (PV 2/7) », `targetHitPoints` 2 | api |
| Happy path   | `GET /api/decisions/pending` après la réponse | 204 | api |
| Edge case    | pendant l'attente : attaque, pose, retrait d'un jeton | 409 avec la raison ; journal et scène inchangés | api |
| Edge case    | pendant l'attente : nouvelle carte | 201 ; `GET /api/decisions/pending` rend 204, journal vide | api |
| Edge case    | réponse à un id inconnu, ou sans décision en attente | 404 | api |
| Edge case    | réponse avec une option absente | 400 avec la raison ; la décision reste en attente | api |
| Edge case    | Gobelin → Magicienne, d20 15 (19 : Bouclier inutile) | 200, `decision` null (`laMagicienneToucheeNePoseAucuneDecision` inchangé) | api |
| Teardown     | `@BeforeEach` recrée la carte | session de base, sans décision | api |

## Tasks to do

### `1)` Décision en session

> La session garde la question posée et la reprend à la réponse.

1. `SceneSession.create` construit le moteur avec `StandardRules.create()` ; supprimer `rules()` et son commentaire.
2. `attack` garde le `StepResult` de `engine.submit` et la cible de l'action ; il rend les entrées ajoutées et la décision éventuelle.
3. `answer(decisionId, option)` : si aucune décision n'a cet id, `NotFoundException` côté ressource ; sinon `engine.answer`, entrées ajoutées sous le même numéro d'action, décision suivante éventuelle.
4. `pendingDecision()` rend `engine.pendingDecision()`.
5. `attack`, `place` et `remove` lèvent `DecisionPendingException` (« Une décision attend une réponse : … ») tant qu'une décision attend ; `create` reste permis.

### `2)` API

> Le client voit la question, répond, et reçoit la suite de l'action.

1. `Dtos.DecisionDto(id, chooserId, chooserName, prompt, options)` ; `AttackResultDto` gagne `decision` (null si l'action est finie).
2. `DecisionResource` : `GET /api/decisions/pending` (200 ou 204) ; `POST /api/decisions/{id}` avec `{option}` rend un `AttackResultDto` (cible de l'action suspendue, PV après coup).
3. `ErrorMapper` : `DecisionPendingException` → 409 avec la raison ; option invalide → 400 (l'`IllegalArgumentException` du moteur).

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Une attaque que Bouclier ferait rater s'arrête sans dégâts ; après « Lancer Bouclier » elle rate et la Magicienne garde 7 PV ; après « Ne rien faire » elle perd 5 PV. Pose, retrait et attaque sont refusés pendant l'attente ; une nouvelle carte efface la décision. |
| 2 | Les scénarios de `DecisionApiTest` passent ; `AttackApiTest`, `SceneApiTest` et `SceneWithoutMapTest` passent inchangés ; `mvn -q test` passe. |
