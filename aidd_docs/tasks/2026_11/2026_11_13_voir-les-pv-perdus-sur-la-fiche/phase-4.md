---
status: done
---

# Instruction: mémoire et backlog

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
aidd_docs/
├── memory/
│   ├── architecture.md      ✏️
│   ├── codebase-map.md      ✏️
│   └── project-brief.md     ✏️
└── backlog/stories/
    └── voir-sur-la-fiche-les-pv-perdus-en-combat.md ✏️ status: done
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | phases 1 à 3 faites | code et parcours vérifiés | system |
| Happy path   | relire la mémoire contre le code | chaque nom cité existe (`PlayerCharacters.sustained`, `HitPointsLost`, `lost`, `CharacterSheetFollower`, `ActionResultDto.warnings`) | system |
| Edge case    | le brief dit « retoucher la fiche ne change pas la créature posée » | phrase gardée, complétée : les coups reçus baissent la fiche | system |

## Tasks to do

### `1)` Décrire le suivi de la fiche

> La mémoire dit ce que fait le code.

1. `architecture.md` : puce du catalogue de préparation complétée (suivi des coups, delta `lost`, envoi après la garde et hors rejeu, avertissement, tout coup compté) ; pièges : arrêt entre les deux écritures (coup perdu pour la fiche), saisie d'un onglet périmé qui efface les PV perdus, PJ posé deux fois, combat repris avec la copie d'avant une retouche.
2. `codebase-map.md` : `PlayerCharacters.sustained`, `CharacterSheetFollower`.
3. `project-brief.md` : les PV d'un personnage posé suivent sa fiche.
4. Story au statut `done`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | La mémoire nomme le port, l'événement et ses limites ; la story est `done` |
