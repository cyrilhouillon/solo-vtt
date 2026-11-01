# Carte du code

```mermaid
flowchart TD
  subgraph dnd-engine
    demo --> attack
    demo --> rules
    catalog --> rules
    catalog --> state
    rules --> attack
    attack --> events
    events --> state
  end
  subgraph vtt-engine
    engine["solovtt.engine"]
    scene["solovtt.scene"]
  end
  subgraph vtt-server
    server["solovtt.server"]
  end
  web["vtt-web"] -- "/api" --> server
  server --> catalog
  server --> scene
  attack --> engine
  rules --> engine
  events --> engine
```

## Zones

- `pom.xml` (racine) : agrégateur Maven `solovtt:solo-vtt`, qui porte les versions communes.
- `vtt-engine/` : le moteur générique (`solovtt.engine` : `Engine`, `Pipeline`, `Listener`, `RuleBook`, `Event`, `Decision`, `Dice`) et la scène (`solovtt.scene` : `Scene`, `Token`, `Position`, `DistanceUnit`). Il ne contient aucune règle ni aucun type de jeu.
- `dnd-engine/` : les règles D&D 2024, qui dépendent de `vtt-engine`.
  - `state/` : l'état du jeu (`GameState`, `Creature`, armes, conditions, types de dégâts).
  - `events/` : `GameEvent` (sealed, `extends Event<GameState>`), tout ce qui arrive et comment ça modifie l'état.
  - `attack/` : l'action « attaque », c'est-à-dire ses phases, son contexte, `AttackRule` et les règles de base 2024.
  - `rules/` : les règles optionnelles et `StandardRules`, qui assemble le `RuleBook`.
  - `catalog/` : `SrdCatalog`, les créatures prêtes à l'emploi du SRD 5.2.1.
  - `demo/` : démonstration console.
- `vtt-server/` : serveur Quarkus (`solovtt.server`). `SceneSession` tient la scène et l'état ; les ressources REST sont sous `/api` (`catalog`, `scene`, `scene/tokens`, `scene/distance`, `creatures/{id}`).
- `vtt-web/` : client Vue 3 + TypeScript. `src/api.ts` appelle le serveur, `src/map/SceneCanvas.ts` dessine la carte avec Konva, `src/components/` porte les panneaux.

## Points d'entrée

- `dnd.engine.demo.Main` : la démo console (`--interactif`, `--seed=N`). Après un `mvn compile` à la racine, elle se lance depuis `dnd-engine/` avec `java -cp "target/classes;../vtt-engine/target/classes" dnd.engine.demo.Main` (le séparateur est `:` hors Windows).
- Pour un appelant : `new Engine<>(state, StandardRules.create(), dice)`, puis `submit` et `answer`.
- L'application : `mvn install -DskipTests` à la racine, puis `mvn quarkus:dev -pl vtt-server` (port 8080) et, dans `vtt-web/`, `npm install` puis `npm run dev` (port 5173, proxy `/api` vers 8080).
