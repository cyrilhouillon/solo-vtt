# Package

## API publique

- `vtt-engine` (Maven `solovtt:vtt-engine`) est le moteur générique et la scène ; `dnd-engine` (Maven `dnd:dnd-engine`) est la librairie de règles D&D 2024 qui s'appuie dessus. Toutes deux sont destinées à `solo-vtt`. `vtt-module-api` (Maven `solovtt:vtt-module-api`) porte les interfaces qu'un module injecte et que le cœur implémente (`ModuleData`).
- `vtt-server` (Maven `solovtt:vtt-server`, Quarkus) expose les moteurs en REST JSON sous `/api` ; `vtt-web` (npm, privé) est le client.
- La surface de pilotage est `Engine<S, E>` : `submit(Resolution)`, `answer(decisionId, option)`, `events()`, `subscribe(observer)`, `pendingDecision()`.
- Elle s'étend par des `Listener` enregistrés dans un `RuleBook` (`StandardRules.create()` donne l'ensemble par défaut).
- Le moteur rend la main avec `StepResult` (`Idle` ou `AwaitingDecision`). L'appelant (UI, IA, réseau) répond quand il veut.

## Consommateurs

- `vtt-server` est le premier consommateur des librairies ; la démo console les utilise aussi.
- Les librairies ne sont pas publiées : elles s'installent localement avec `mvn install` à la racine. Elles demandent Java 21 au minimum.

## Versionnage

- Le projet est en `SNAPSHOT` sans engagement de stabilité : l'API peut casser à chaque refactor tant que `solo-vtt` ne la consomme pas.
- Aucune de ces librairies n'a de dépendance runtime (hormis `dnd-engine` vers `vtt-engine`), `mythic-engine` (Maven `mythic:mythic-engine`, oracle Mythic GME 2e) seulement vers `vtt-engine`, `vtt-module-api` aucune, et cela doit rester ainsi. Les dépendances (Quarkus, Jackson, Konva, Vue, vue-router, three, cannon-es, Tiptap) restent confinées à `vtt-server` et `vtt-web`, aux versions figées. three et cannon-es ne sont chargés qu'au premier lancer de dés (chunk séparé) ; cannon-es n'est plus maintenu depuis 2022, mais n'a aucune dépendance. `@tiptap/suggestion` (liste `[[`) et `@floating-ui/dom` (sa position, pair de `@tiptap/vue-3`) suivent la version de Tiptap.
- `vue-router` est en 5.x : ses dépendances servent au routage par fichiers (plugin de build), non utilisé ; les routes sont déclarées à la main dans `src/router.ts`.
- `vtt-web` reste en TypeScript 6.x : TypeScript 7 (portage natif) risque de casser `vue-tsc`. Monter de version seulement quand `vue-tsc` le prend en charge.
