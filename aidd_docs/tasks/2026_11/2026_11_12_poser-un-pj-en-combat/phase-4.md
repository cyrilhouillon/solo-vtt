---
status: done
---

# Instruction: mémoire du projet et backlog

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
aidd_docs/
├── memory/
│   ├── architecture.md      ✏️
│   ├── codebase-map.md      ✏️
│   ├── project-brief.md     ✏️
│   └── testing.md           ✏️
└── backlog/stories/poser-un-pj-en-combat-depuis-le-catalogue.md ✏️ status: done
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Phases 1 à 3 livrées | Code fusionnable | system |
| Happy path   | Relire la mémoire contre le code | Routes, port, `Place`, attaques structurées décrits tels qu'implémentés | system |
| Edge case    | Chercher `/api/catalog`, « Guerrière », « Magicienne » dans `aidd_docs/memory` | Plus aucune mention périmée | cli |

## Tasks to do

### `1)` Mémoire

> La mémoire décrit le code livré.

1. `architecture.md` : catalogue par campagne et port `PlayerCharacters` ; `Place` garde nom et profil ; carte enregistrée qui recharge la fiche ; attaques de fiche structurées, fiches d'avant illisibles ; Bouclier sans PJ qui le connaisse (piège).
2. `codebase-map.md` : `CreatureProfile`, `PlayerCharacters`, `SheetCharacters`, `CampaignCatalog` ; route `catalog` déplacée sous la campagne.
3. `project-brief.md` : poser ses PJ ; fin des prétirés.
4. `testing.md` : `TestCharacters` sert les prétirés aux tests d'API.

### `2)` Backlog

> La story est marquée livrée.

1. `status: done` dans la story ; `status: implemented` dans `plan.md`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Aucune mention de `/api/catalog` commun ni des prétirés au catalogue dans `aidd_docs/memory` |
| 2 | Story `done`, plan `implemented` |
