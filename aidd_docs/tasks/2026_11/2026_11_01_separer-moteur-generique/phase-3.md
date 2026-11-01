---
status: done
---

# Instruction: mettre à jour la mémoire projet

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
aidd_docs/memory/
├── architecture.md        ✏️
├── codebase-map.md        ✏️
├── coding-assertions.md   ✏️
├── package.md             ✏️
├── project-brief.md       ✏️
├── testing.md             ✏️
└── vcs.md                 ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | aucun | état issu de la phase 2 | system |
| Happy path   | lancer les commandes de `coding-assertions.md` telles qu'écrites | elles réussissent depuis l'emplacement indiqué | cli |
| Edge case    | chercher `dnd.engine.core` et « depuis `dnd-engine/` » dans `aidd_docs/memory/` | aucun résultat obsolète | cli |

## Tasks to do

### `1)` Décrire les deux modules

> La mémoire reflète le code.

1. `architecture.md` : moteur générique paramétré par état et événement, D&D en module dépendant ; handler de décision sans moteur.
2. `codebase-map.md` : modules `vtt-engine` et `dnd-engine`, diagramme et points d'entrée à jour.
3. `package.md` : deux artefacts, surface `Engine<S, E>`, installation par `mvn install` à la racine.
4. `coding-assertions.md` et `testing.md` : commandes lancées à la racine, emplacement des tests des deux modules.
5. `project-brief.md` : le dépôt contient le moteur générique et dnd-engine.
6. `vcs.md` : scopes de commit `engine` (vtt-engine) au lieu de `core`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | les commandes documentées réussissent telles qu'écrites et aucun chemin ou package obsolète ne reste |
