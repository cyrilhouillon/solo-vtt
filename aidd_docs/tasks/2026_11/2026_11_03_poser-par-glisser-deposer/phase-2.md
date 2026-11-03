---
status: done
---

# Instruction: réserver la pose armée au clavier, textes et mémoire

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── aidd_docs/
│   ├── backlog/stories/poser-une-creature-en-la-glissant-du-catalogue-sur-la-carte.md  ✏️ status: done
│   ├── memory/architecture.md                                                            ✏️ pose par glisser, pose armée au clavier
│   ├── memory/project-brief.md                                                           ✏️ « posées au clic » → glissées
│   └── tasks/2026_11/2026_11_03_poser-par-glisser-deposer/plan.md                        ✏️ status: implemented
├── DESIGN.md                    ✏️ fantôme du dépôt, curseur du catalogue, armement au clavier
└── vtt-web/src/
    ├── App.vue                  ✏️ barre d'état de la pose armée
    └── components/
        ├── CatalogPanel.vue     ✏️ clic souris inerte, indication
        └── TokenBoard.vue       ✏️ texte de la carte vide
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081/5174, carte vide | Catalogue ouvert | browser |
| Happy path   | Tab jusqu'à une entrée, Entrée, flèches, Entrée | Créature armée (`aria-pressed`), puis posée sur la case du curseur | browser |
| Happy path   | Espace sur une entrée armée | Pose désarmée | browser |
| Edge case    | Clic souris sur une entrée | Rien ne change : pas d'armement, pas de message | browser |
| Edge case    | Toucher une entrée sans glisser (émulé) | Rien ne change | browser |
| Edge case    | Pose armée au clavier, puis clic sur la carte | Pose au clic (inchangée) | browser |
| Teardown     | Tuer les processus sur 8081 et 5174 | 8080 et 5173 intacts | system |

## Tasks to do

### `1)` Clic souris inerte

> Seule une activation clavier ou lecteur d'écran arme la pose.

1. Dans `CatalogPanel`, n'émettre `arm` que si `event.detail === 0`.
2. Ne pas armer à la fin d'un glisser (le `click` qui suit un `pointerup` a `detail >= 1`).

### `2)` Textes

> Le joueur sait qu'il faut glisser, et le clavier sait comment poser.

1. Indication du catalogue : glisser une créature sur la carte ; au clavier, Entrée sur une créature puis flèches et Entrée.
2. Barre d'état de la pose armée (`App.vue:940`) : flèches, Entrée pour poser, Échap arrête.
3. Tableau vide (`TokenBoard.vue:205`) : glisser une créature du catalogue sur la carte.

### `3)` Mémoire et backlog

> La documentation décrit la pose telle qu'elle est.

1. `architecture.md` : la pose se fait par glisser (événements pointeur, souris et doigt) ; la pose armée est réservée au clavier.
2. `project-brief.md` : remplacer « posées au clic ».
3. `DESIGN.md` : fantôme du dépôt sur la carte, curseur `grabbing` depuis le catalogue, entrée armée réservée au clavier.
4. Story `status: done`, plan `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Un clic souris ou un toucher sur une entrée ne fait rien ; Entrée ou Espace arme et désarme ; un glisser n'arme pas. |
| 2 | L'indication du catalogue et le tableau vide invitent à glisser ; le catalogue décrit la pose au clavier ; la barre d'état de la pose armée ne parle plus de clic. |
| 3 | `architecture.md`, `project-brief.md` et `DESIGN.md` décrivent la pose par glisser ; story et plan portent leur statut final. |
