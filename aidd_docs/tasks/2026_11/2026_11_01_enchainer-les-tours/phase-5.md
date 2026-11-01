---
status: pending
---

# Instruction: Mettre à jour la mémoire projet

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md                                                       ✏️
└── aidd_docs/
    ├── backlog/stories/enchainer-les-tours-selon-l-initiative.md   ✏️
    └── memory/
        ├── architecture.md                                         ✏️
        ├── codebase-map.md                                         ✏️
        └── project-brief.md                                        ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | phases 1 à 4 livrées | code et tests à jour | system |
| Happy path   | relire chaque document modifié contre le code | chaque affirmation nouvelle a son fichier ou son test | system |
| Edge case    | piège « Pas encore de structure de tour » | réécrit : la réaction revient au début du tour ; Bouclier n'expire toujours pas | system |

## Tasks to do

### `1)` Mémoire

> La mémoire décrit le combat tel qu'il est codé.

1. `architecture.md` : `TurnOrder` et `TurnPolicy` génériques ; groupes d'initiative et « Qui joue ? » côté D&D ; attaque réservée à la créature active ; arrivées et départs en combat (créature hors journal) ; piège des tours réécrit.
2. `codebase-map.md` : `TurnOrder` et `TurnPolicy` dans `vtt-engine`, `combat/` dans `dnd-engine`, ressource `combat`, `CombatControl`, `ActionResultDto`.
3. `project-brief.md` : fonctionnalité « lancer le combat et enchaîner les tours ».
4. `DESIGN.md` : contrôle de combat dans la barre, colonne Init., ligne active, « Joué », marque du jeton actif, titre du dialogue de décision.

### `2)` Backlog

> La story est livrée.

1. Story : `status: done`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Aucune affirmation de la mémoire n'est contredite par le code (relecture). |
| 2 | La story porte `status: done`. |
