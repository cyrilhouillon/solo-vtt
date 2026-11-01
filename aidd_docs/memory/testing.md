# Tests

## Stratégie

- Des tests de règles unitaires : chacun décrit une situation de table exacte et vérifie les événements émis et l'état final.
- La rejouabilité est testée : rejouer le journal sur une copie de l'état initial doit redonner la même `GameState.signature()`.

## Outils

- JUnit Jupiter, avec les assertions JUnit. Pas de librairie de mock.

## Conventions

- Les tests sont dans `dnd-engine/src/test/java/`.
- Les dés sont toujours scriptés (`Dice.scripted`), dans l'ordre de consommation : d20 (un ou deux), dés bonus au jet, puis dés de dégâts. Il n'y a jamais de hasard réel dans un test.
- Les noms de méthodes sont en français et décrivent la règle (`avantageEtDesavantageSAnnulent_unSeulD20`).
- Toute nouvelle règle arrive avec le test de la situation de table qu'elle change.

## Lancer

- `mvn test` depuis `dnd-engine/`.
