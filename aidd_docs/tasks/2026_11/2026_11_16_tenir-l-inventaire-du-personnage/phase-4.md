---
status: pending
---

# Instruction: règles non jouées, mémoire et backlog

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
aidd_docs/
├── memory/architecture.md        ✏️ fiche calculée (CA, attaques), inventaire, classe obligatoire, maîtrise d'arme, formation, catalogue d'équipement, fiches d'avant illisibles
├── memory/codebase-map.md        ✏️ Item, Inventory, CombatStats ; InventoryPanel.vue
├── memory/project-brief.md       ✏️ inventaire de la fiche, CA et attaques calculées
└── backlog/
    ├── epics/tenir-ses-personnages-dnd-2024-dans-le-carnet.md  ✏️ CA et attaques calculées, avec ajustements, au lieu de retouchées
    ├── stories/tenir-l-inventaire-du-personnage.md             ✏️ status: done
    ├── stories/subir-les-penalites-d-armure.md                 ✅ armure sans formation (Désavantage For / Dex, pas de sorts), Force requise (−3 m), Discrétion
    └── tasks/lister-les-capacites-de-niveau-1-que-le-moteur-ne-joue-pas.md  ✏️ formation aux armures, Force des armures lourdes, Discrétion, mains (Deux mains, Polyvalente)
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | phases 1 à 3 livrées | tests et build verts | cli |
| Happy path   | relire la mémoire contre le code | chaque nouvelle classe, route et règle décrite ; rien de périmé (CA, attaques ou équipement gardés, `rebuild` de CA et d'attaques, classe en texte) | system |
| Edge case    | une règle d'équipement non jouée sans story | aucune : chaque ligne « non » de la liste renvoie à une story | system |

## Tasks to do

### `1)` Règles rencontrées

> Chaque règle d'équipement que le moteur ne joue pas a sa story.

1. Story « subir les pénalités d'armure », rattachée à l'epic D&D.
2. Lignes de la liste de la tâche : formation aux armures, Force requise, Discrétion → cette story ; Deux mains et Polyvalente → [jouer les styles de combat du Guerrier](../../../backlog/stories/jouer-les-styles-de-combat-du-guerrier.md) si elle les couvre, sinon une story à part.

### `2)` Mémoire

> La mémoire dit ce que fait le code.

1. `architecture.md`, `codebase-map.md`, `project-brief.md`, epic.
2. Story `done`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | La liste porte chaque règle rencontrée, chacune vers une story existante |
| 2 | La mémoire décrit la fiche telle que livrée ; la story est `done` |
