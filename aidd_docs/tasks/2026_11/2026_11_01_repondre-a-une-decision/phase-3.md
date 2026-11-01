---
status: pending
---

# Instruction: mettre à jour la mémoire projet

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── aidd_docs/
    ├── memory/
    │   ├── architecture.md     ✏️ décision en session, blocage 409, nouvelle carte comme issue ; retirer les pièges sur ShieldReaction absent du serveur
    │   ├── codebase-map.md     ✏️ ressource decisions, DecisionDialog
    │   └── project-brief.md    ✏️ fonctionnalité : répondre à une décision (Bouclier)
    └── backlog/stories/
        └── repondre-a-une-decision-du-moteur.md ✏️ status: done
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | phases 1 et 2 livrées | code final en place | system |
| Happy path   | relire chaque classe, ressource et composant cité | tous existent sous ce nom | system |
| Edge case    | chercher « ShieldReaction » dans `architecture.md` | plus aucun piège ne dit que le serveur ne l'enregistre pas ; le piège « +5 jamais retiré » reste, réaction jamais rendue ajoutée | system |

## Tasks to do

### `1)` Mémoire

> La mémoire décrit le serveur tel qu'il est.

1. `architecture.md` : la session garde la décision, refuse attaque, pose et retrait pendant l'attente (409), « nouvelle carte » reste permise ; retirer les deux pièges sur `ShieldReaction` et `SceneSession.rules` ; ajouter au piège Bouclier que la réaction n'est jamais rendue (pas de tours).
2. `codebase-map.md` : `decisions` dans les ressources, `DecisionDialog` dans les composants.
3. `project-brief.md` : la fonctionnalité dans « Fonctionnalités clés ».
4. Story en `status: done`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Chaque nom cité existe dans le code ; la story est `done` ; plus aucune mention d'un serveur sans Bouclier. |
