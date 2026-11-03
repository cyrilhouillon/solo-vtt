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
    ├── memory/project-brief.md                                        ✏️
    ├── backlog/stories/voir-les-des-rouler-et-le-resultat-sur-la-carte.md ✏️
    └── tasks/2026_11/2026_11_03_voir-les-des-rouler/plan.md           ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Happy path   | Relire chaque document contre le code livré | chaque phrase décrit le comportement observé | system |
| Edge case    | Critère de la story non tenu | signalé, story laissée hors `done` | system |

## Tasks to do

### `1)` Design

> `DESIGN.md` décrit ce qui est livré.

1. Piste à dés : les dés (jet d'attaque, filet, dégâts ; d20 écarté atténué et barré), le roulement puis le verdict ; réécrire « seul mouvement » (Creative North Star et Piste à dés) en une séquence dés → verdict → bulle.
2. Carte : jauge de PV sous les jetons, nom sous la jauge, bulle d'issue sur la cible.

### `2)` Mémoire

> La mémoire suit le code.

1. `architecture.md` : le fait `dice` des entrées du journal (faces, valeurs, valeur gardée), rangé avant ou après l'issue par le client.
2. `project-brief.md` : dés sur la piste, jauge des jetons, issue sur la cible.

### `3)` Story et plan

> Le backlog dit ce qui est fait.

1. Vérifier dans le navigateur (8081/5174) les critères tenus avant cette story : à terre sur la carte et dans le tableau (`SceneCanvas.ts`, `TokenBoard.vue`), issue d'abord et détail replié au journal (`JournalPanel.vue`), journal complet.
2. Amender les critères de la story (dés sur la piste, pas de surimpression centrale, décision de l'utilisateur), `status: done`.
3. Plan en `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `DESIGN.md` ne dit plus que le verdict est le seul mouvement ; jauge et bulle y figurent. |
| 2 | `architecture.md` et `project-brief.md` nomment le fait `dice` et les fonctionnalités livrées. |
| 3 | Story `done` avec ses critères amendés ; plan `implemented`. |
