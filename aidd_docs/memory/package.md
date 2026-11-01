# Package

## API publique

- `vtt-engine` (Maven `solovtt:vtt-engine`) est le moteur générique ; `dnd-engine` (Maven `dnd:dnd-engine`) est la librairie de règles D&D 2024 qui s'appuie dessus. Toutes deux sont destinées à `solo-vtt`.
- La surface de pilotage est `Engine<S, E>` : `submit(Resolution)`, `answer(decisionId, option)`, `events()`, `subscribe(observer)`, `pendingDecision()`.
- Elle s'étend par des `Listener` enregistrés dans un `RuleBook` (`StandardRules.create()` donne l'ensemble par défaut).
- Le moteur rend la main avec `StepResult` (`Idle` ou `AwaitingDecision`). L'appelant (UI, IA, réseau) répond quand il veut.

## Consommateurs

- Il n'y a pas encore de consommateur ; seule la démo console utilise le moteur.
- Les librairies ne sont pas publiées : elles s'installent localement avec `mvn install` à la racine. Elles demandent Java 21 au minimum.

## Versionnage

- Le projet est en `SNAPSHOT` sans engagement de stabilité : l'API peut casser à chaque refactor tant que `solo-vtt` ne la consomme pas.
- Aucune des deux librairies n'a de dépendance runtime (hormis `dnd-engine` vers `vtt-engine`), et cela doit rester ainsi.
