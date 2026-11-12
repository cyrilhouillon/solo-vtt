---
status: pending
---

# Instruction: stories des capacités non jouées et mémoire

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
aidd_docs/
├── backlog/
│   ├── stories/
│   │   ├── creer-en-guide-un-guerrier-de-niveau-1.md                ✏️ status: done
│   │   ├── utiliser-second-souffle-en-combat.md                     ✅
│   │   ├── appliquer-les-bottes-d-arme-en-combat.md                 ✅
│   │   └── jouer-les-styles-de-combat-du-guerrier.md                ✅
│   └── tasks/lister-les-capacites-de-niveau-1-que-le-moteur-ne-joue-pas.md  ✏️ liste du Guerrier
└── memory/
    ├── architecture.md      ✏️
    ├── codebase-map.md      ✏️
    └── project-brief.md     ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | relire `rules/` de `dnd-engine` (`StandardRules`, `DuelingStyle`…) | liste de ce que le moteur joue | system |
| Happy path   | lire la tâche de la liste | Second souffle, Bottes d'arme et les quatre styles y figurent, chacun avec sa story ou « joué » | system |
| Edge case    | Défense (+1 CA) | noté « porté par la fiche » : la CA de la fiche le compte, rien à jouer | system |

## Tasks to do

### `1)` Stories des capacités non jouées

> Chaque capacité de niveau 1 du Guerrier que le moteur ne joue pas a sa story.

1. Vérifier dans `dnd-engine/src/main/java/dnd/engine/rules/` ce que le moteur joue.
2. Écrire les stories au format du backlog (front matter `type: story`, `status: proposed`, parent à choisir avec le joueur s'il n'existe pas d'epic des capacités) : Second souffle ; Bottes d'arme (les huit bottes du SRD) ; styles Archerie, Armes à deux mains, Combat à deux armes.
3. Tenir la liste du Guerrier dans la tâche `lister-les-capacites-…`.

### `2)` Mémoire

> La mémoire décrit la construction guidée.

1. `architecture.md` : construction (`Build`, recalcul par différence, `SheetBuilt`, `AbilitiesRolled`, routes `classes`, `build`, `ability-rolls`, dés une fois).
2. `codebase-map.md` : nouveaux types de `dnd.engine.sheet`, `GuidedCreation.vue`.
3. `project-brief.md` : création guidée du Guerrier.
4. Story au statut `done`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Chaque capacité de niveau 1 du Guerrier est jouée, portée par la fiche, ou renvoie à une story |
| 2 | La mémoire nomme chaque nouveau type, route et événement |
