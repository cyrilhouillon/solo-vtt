---
status: pending
---

# Instruction: stories des dons non joués et mémoire

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
aidd_docs/
├── backlog/
│   ├── stories/
│   │   ├── choisir-un-historique-en-creation-guidee.md                     ✏️ status done
│   │   ├── creer-un-personnage-en-saisie-libre-dans-personnages-joueurs.md  ✏️ status done (livrée, restée ready)
│   │   ├── creer-en-guide-les-autres-lanceurs-de-sorts.md                  ✏️ + sorts d'Initié à la magie
│   │   ├── relancer-les-degats-avec-sauvagerie-martiale.md                 ✅
│   │   └── echanger-son-initiative-avec-vigilant.md                        ✅
│   └── tasks/
│       └── lister-les-capacites-de-niveau-1-que-le-moteur-ne-joue-pas.md   ✏️ lignes des 4 dons, Polyvalent
└── memory/
    ├── architecture.md      ✏️ historique dans la construction, Build.abilities, feats, routes
    ├── codebase-map.md      ✏️ nouvelles classes de sheet/
    └── project-brief.md     ✏️ choix d'historique et de Polyvalent
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | phases 1 à 3 livrées | fiche Soldat, Criminel, Acolyte, Sage, Humain Polyvalent construites | system |
| Happy path   | relire la liste de la tâche | Doué et Vigilant (initiative) « portés par la fiche » ; Échange d'Initiative, Sauvagerie martiale, sorts d'Initié à la magie renvoient chacun à une story ; Polyvalent n'est plus « au choix » | system |
| Happy path   | relire la mémoire | `architecture.md`, `codebase-map.md`, `project-brief.md` décrivent l'historique tel que livré | system |
| Edge case    | une capacité de la fiche d'un historique absente de la liste | ajoutée à la liste | system |

## Tasks to do

### `1)` Stories des dons non joués

> Chaque don d'origine que le moteur ne joue pas a sa story.

1. Story « relancer les dégâts avec Sauvagerie martiale » (une fois par tour, deux jets de dés de dégâts d'arme, le joueur garde l'un ; journal et rejeu), parent l'epic, `proposed`, ordre après les stories existantes.
2. Story « échanger son initiative avec Vigilant » (après l'initiative, échange avec un allié volontaire non Neutralisé), `proposed`.
3. `creer-en-guide-les-autres-lanceurs-de-sorts.md` : + choisir les sorts d'Initié à la magie (historique ou Polyvalent).
4. Tâche de liste : lignes Doué, Initié à la magie, Sauvagerie martiale, Vigilant (deux lignes), Polyvalent mis à jour.

### `2)` Mémoire et backlog

> La mémoire décrit ce qui est livré.

1. `architecture.md` : historique, don d'origine et Polyvalent dans la construction, `Build.abilities`, refus des compétences en double, `feats`, routes `backgrounds` et `origin-feats`, retrait de l'historique refusé.
2. `codebase-map.md` : `Background`, `BackgroundChoice`, `BackgroundEquipment`, `GamingSet`, `OriginFeat`, `OriginFeatChoice`, `SpellList`.
3. `project-brief.md` : la création guidée choisit l'historique et le don de l'Humain.
4. Statuts : story historique `done`, story saisie libre `done`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Chaque don d'origine non joué renvoie à une story existante du backlog |
| 2 | La mémoire nomme les nouvelles classes et routes ; les deux stories sont `done` |
