# Package

## API publique

- `dnd-engine` (Maven `dnd:dnd-engine`) est la librairie de règles destinée à `solo-vtt`. Elle sera refactorée avant d'être consommée.
- La surface de pilotage est `Engine` : `submit(Resolution)`, `answer(decisionId, option)`, `events()`, `subscribe(observer)`, `pendingDecision()`.
- Elle s'étend par des `Listener` enregistrés dans un `RuleBook` (`StandardRules.create()` donne l'ensemble par défaut).
- Le moteur rend la main avec `StepResult` (`Idle` ou `AwaitingDecision`). L'appelant (UI, IA, réseau) répond quand il veut.

## Consommateurs

- Il n'y a pas encore de consommateur ; seule la démo console utilise le moteur.
- La librairie n'est pas publiée : elle s'installe localement avec `mvn install`. Elle demande Java 21 au minimum.

## Versionnage

- Le projet est en `SNAPSHOT` sans engagement de stabilité : l'API peut casser à chaque refactor tant que `solo-vtt` ne la consomme pas.
- La librairie n'a aucune dépendance runtime, et doit rester ainsi.
