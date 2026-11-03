---
status: done
---

# Instruction: Mettre à jour le design, la mémoire projet et la story

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md                                                          ✏️
└── aidd_docs/
    ├── memory/architecture.md                                         ✏️
    ├── memory/codebase-map.md                                         ✏️
    ├── memory/package.md                                              ✏️
    ├── memory/project-brief.md                                        ✏️
    ├── backlog/stories/voir-les-des-rouler-et-le-resultat-sur-la-carte.md ✏️
    └── tasks/2026_11/2026_11_03_des-3d-sur-la-carte/plan.md           ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Happy path   | Relire chaque document contre le code livré | chaque phrase décrit le comportement observé | system |
| Edge case    | Phrase qui décrit encore les faces de passage de la piste | réécrite ou retirée | system |

## Tasks to do

### `1)` Design

> `DESIGN.md` décrit ce qui est livré.

1. North Star et Don't : la séquence devient dés 3D sur la carte → verdict → bulle ; plus de « seul mouvement : le verdict ».
2. Piste à dés : retirer faces de passage et balancement ; verdict, dés posés et cible apparaissent à l'arrêt des dés 3D ; corriger le double espace « atténuée  ; ».
3. Carte : couche des dés (lancés du bas, matériaux, couleurs par rôle, d20 écarté atténué, effacement, sans pointeur, absente sous `prefers-reduced-motion`).

### `2)` Mémoire

> La mémoire suit le code.

1. `architecture.md` : module `src/dice/`, simulation hors écran puis échange des numéros de faces, valeurs toujours celles du serveur ; repli sans WebGL.
2. `codebase-map.md` : `src/dice/` dans la zone `vtt-web`.
3. `package.md` : three et cannon-es figés, chargés à la demande ; cannon-es non maintenu.
4. `project-brief.md` : « Dés à la table » dit que les dés 3D roulent sur la carte.

### `3)` Story et plan

> Le backlog dit ce qui est livré.

1. Story : le premier critère dit que les dés roulent sur la carte ; note d'amendement datée (choix de l'utilisateur, dés 3D).
2. Plan en `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `DESIGN.md` ne mentionne plus de faces de passage sur la piste ; les dés 3D y figurent avec leur repli `prefers-reduced-motion`. |
| 2 | `architecture.md`, `codebase-map.md`, `package.md` et `project-brief.md` nomment `src/dice/` ou les dés 3D et leurs dépendances. |
| 3 | Story amendée ; plan `implemented`. |
