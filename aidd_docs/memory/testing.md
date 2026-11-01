# Tests

## Stratégie

- Des tests de règles unitaires : chacun décrit une situation de table exacte et vérifie les événements émis et l'état final.
- La rejouabilité est testée : rejouer le journal sur une copie de l'état initial doit redonner la même `GameState.signature()`.

## Outils

- JUnit Jupiter, avec les assertions JUnit. Pas de librairie de mock.
- `vtt-server` : `@QuarkusTest` et RestAssured contre l'API réelle ; RestAssured lit les nombres JSON en `float` (`equalTo(5.0f)`).
- `vtt-web` : pas de tests automatisés ; `vue-tsc` vérifie les types et le parcours se valide dans le navigateur.

## Conventions

- Les tests sont dans `vtt-engine/src/test/java/` (axiomes du moteur sur un système jouet, scène), `dnd-engine/src/test/java/` (règles D&D, catalogue) et `vtt-server/src/test/java/` (API).
- Les dés sont toujours scriptés (`Dice.scripted`), dans l'ordre de consommation : d20 (un ou deux), dés bonus au jet, puis dés de dégâts. Il n'y a jamais de hasard réel dans un test.
- Les noms de méthodes sont en français et décrivent la règle (`avantageEtDesavantageSAnnulent_unSeulD20`).
- Toute nouvelle règle arrive avec le test de la situation de table qu'elle change.

## Lancer

- `mvn test` depuis la racine.
- Le serveur (8080) et le client (5173) de développement sont lancés par l'utilisateur depuis les lanceurs IntelliJ. Ne jamais les arrêter : valider le parcours dans son Chrome via l'extension Claude in Chrome sur ces ports, ou lancer ses propres serveurs sur d'autres ports (par exemple `-Dquarkus.http.port=8081` et `vite --port 5174`, en redirigeant `/api` vers 8081). Les tests navigateur modifient la session en mémoire du serveur visé.
