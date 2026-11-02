---
status: pending
---

# Instruction: Mettre à jour la mémoire projet et le design

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md                                                        ✏️ aperçu de glisser, vitesse restante
└── aidd_docs/
    ├── backlog/stories/deplacer-une-creature-dans-la-limite-de-sa-vitesse.md  ✏️ status: done
    ├── memory/
    │   ├── architecture.md                                          ✏️ Maj, règles du déplacement, piège des positions
    │   ├── codebase-map.md                                          ✏️ Move, Scene.move, route move
    │   └── project-brief.md                                         ✏️ fonctionnalité
    └── tasks/2026_11/2026_11_02_deplacer-une-creature/plan.md       ✏️ status: implemented
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Phases 1 à 4 livrées | Code final lisible | system |
| Happy path   | Relire chaque document contre le code | Chaque affirmation se retrouve dans le code | system |
| Edge case    | Ancienne ligne « Maj + clic pose hors grille » | Plus présente nulle part | system |

## Tasks to do

### `1)` Mémoire

> La mémoire décrit le déplacement tel qu'il est codé.

1. `architecture.md` : ligne Maj réécrite (hors grille par défaut, Maj cale, chevauchement refusé par le client) ; décision « seule l'active est limitée, son déplacement est un événement ; le reste est libre, hors journal ; `movementLeft` null = illimité, le client ne code aucune limite » ; piège « le journal ne rejoue pas les positions ».
2. `codebase-map.md` : `Scene.move`, `dnd.engine.combat.Move`, route `scene/tokens/{id}/move`.
3. `project-brief.md` : « Déplacement : glisser ou clavier, limité par la vitesse de la créature active ».

### `2)` Design

> `DESIGN.md` décrit l'aperçu de glisser.

1. Fantôme, trait, cercle laiton de vitesse restante, bulle, refus `--danger` ; vitesse restante sur la feuille.

### `3)` Backlog

> Story livrée, plan implémenté.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1    | `architecture.md`, `codebase-map.md` et `project-brief.md` décrivent le déplacement et Maj comme le code. |
| 2    | `DESIGN.md` décrit chaque élément visuel ajouté en phase 4, dans les deux thèmes. |
| 3    | Story `status: done`, plan `status: implemented`. |
