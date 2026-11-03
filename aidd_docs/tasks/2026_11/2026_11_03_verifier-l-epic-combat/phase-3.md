---
status: done
---

# Instruction: clore l'epic ou reporter ses écarts

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── aidd_docs/
    ├── backlog/
    │   ├── epics/jouer-un-combat-dnd-2024-en-local.md ✏️
    │   └── stories/ ou defects/<un item par écart>.md ✅
    └── tasks/2026_11/2026_11_03_verifier-l-epic-combat/
        ├── plan.md ✏️
        └── phase-1.md, phase-2.md, phase-3.md ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Happy path   | `verification.md` sans écart bloquant ni gênant | epic `status: done` | system |
| Edge case    | Un écart bloquant ou gênant | un item par écart (`/aidd-pm:09-defect` pour un défaut, `/aidd-pm:02-user-stories` pour un besoin manquant), parent l'epic ; epic `ready` | system |

## Tasks to do

### `1)` Trancher

> Donner à l'epic l'état que la vérification prouve.

1. Sans écart bloquant ni gênant : passer l'epic à `status: done`.
2. Sinon : créer un item par écart, ordonné, parent l'epic ; l'epic reste `ready`.
3. Écarts cosmétiques : les laisser listés dans `verification.md`, sans item, sauf demande de l'utilisateur.

### `2)` Fermer le plan

> Marquer le plan comme fait.

1. Passer `plan.md` et ses phases à `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Le statut de l'epic correspond à `verification.md` ; chaque écart bloquant ou gênant a un item dans `aidd_docs/backlog/`. |
| 2 | `plan.md` et ses phases portent `status: implemented`. |
