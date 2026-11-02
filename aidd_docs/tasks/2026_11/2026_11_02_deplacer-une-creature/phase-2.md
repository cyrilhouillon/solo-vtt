---
status: pending
---

# Instruction: Moteur : déplacer un jeton, dépenser la vitesse de la créature active

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-engine/src/
├── main/java/solovtt/scene/Scene.java          ✏️ move(tokenId, position), bornes de la carte
└── test/java/solovtt/scene/SceneTest.java      ✏️ déplacer, hors carte, jeton inconnu
dnd-engine/src/
├── main/java/dnd/engine/
│   ├── combat/Move.java                        ✅ résolution : refus puis CreatureMoved
│   ├── events/GameEvent.java                   ✏️ CreatureMoved(creatureId, metres, unit)
│   └── state/Combat.java                       ✏️ déplacement dépensé du tour, remis à zéro à startTurn
└── test/java/dnd/engine/
    ├── MoveTest.java                           ✅ situations de table du déplacement
    └── CombatTest.java                         ✏️ le rejeu couvre le déplacement dépensé
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `CombatTest.table()` et dés scriptés ; combat lancé, Gobelin guerrier actif (vitesse 9 m) | Combat en cours | system |
| Happy path   | `Move.of(gobelin, 6, METRE)` soumis | `CreatureMoved` émis ; reste 3 m | system |
| Happy path   | 4,5 m puis 4,5 m dans le même tour | Les deux acceptés ; reste 0 | system |
| Edge case    | 3 m demandés avec 2,9 m restants | `IllegalArgumentException` avec la vitesse restante dans l'unité donnée, aucun événement | system |
| Edge case    | Exactement la vitesse restante (tolérance d'arrondi) | Accepté | system |
| Edge case    | Carte en pieds : 30 ft avec 9 m de vitesse | Accepté (30 ft = 9 m) ; la raison d'un refus parle en ft | system |
| Edge case    | Créature active à 0 PV, ou entravée | Refus « vitesse de 0 », aucun événement | system |
| Edge case    | Fin du tour puis tour suivant de la même créature | Vitesse restante pleine | system |
| Edge case    | `Move.of` pour une créature non active, ou hors combat | `IllegalStateException`, aucun événement | system |
| Edge case    | `Scene.move` hors carte, ou jeton inconnu | `IllegalArgumentException`, position inchangée | system |
| Edge case    | Rejouer le journal sur une copie de l'état initial | Même `signature()`, déplacement dépensé compris | system |

## Tasks to do

### `1)` Déplacer un jeton sur la scène

> `Scene` sait bouger un jeton dans ses bornes, sans règle.

1. Extraire la vérification de bornes de `place` ; `move(tokenId, Position)` remplace le jeton par une copie à la nouvelle position.
2. Tests dans `SceneTest`.

### `2)` Le déplacement dépensé du tour

> `Combat` compte les mètres dépensés par la créature active.

1. Champ `movementUsed` (mètres), `spendMovement(metres)`, remis à 0 dans `startTurn` ; inclus dans `copy()` et `signature()`.
2. `GameEvent.CreatureMoved(creatureId, metres, unit)` : `apply` dépense ; `describe` « {nom} se déplace de {d} (reste {r}) » dans `unit`.

### `3)` La résolution `Move`

> Seule la créature active se déplace selon les règles, dans sa vitesse restante.

1. `Move.of(creatureId, distance, unit, state)` : `IllegalStateException` hors combat ou si elle n'est pas active ; `IllegalArgumentException` si vitesse 0 (0 PV, `RESTRAINED`) ou distance > restante + tolérance, raison dans `unit`.
2. `Move.remaining(state)` : vitesse restante (mètres) de la créature active, vide sinon ; `Move.limits(creatureId, state)` : vrai si la créature est l'active d'un combat.
3. Étend `CombatStep` et émet `CreatureMoved`, sans décision.

### `4)` Tests de table

> Chaque règle a sa situation.

1. `MoveTest` : scénarios du Test Scope.
2. `CombatTest.rejouerLeJournalDuCombatRedonneLEtatFinal` : un déplacement avant les fins de tour.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1    | Un jeton déplacé dans la carte change de position ; hors carte ou inconnu, refus sans changement. |
| 2    | Le déplacement dépensé apparaît dans la signature et revient à 0 au début du tour de la créature. |
| 3    | Au-delà de la vitesse restante, à 0 PV ou entravée, refus avec la raison et aucun événement ; dans la limite, `CreatureMoved`. |
| 4    | `mvn -q test` passe ; le rejeu redonne la même signature. |
