---
status: done
---

# Instruction: stories des traits non joués et mémoire

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
aidd_docs/
├── backlog/
│   ├── stories/
│   │   ├── choisir-une-espece-en-creation-guidee.md                  ✏️ status: done
│   │   ├── appliquer-les-resistances-d-espece-en-combat.md           ✅ Nain, Drakéide, Tieffelin
│   │   ├── utiliser-le-souffle-du-drakeide-en-combat.md              ✅
│   │   ├── utiliser-l-ascendance-gigante-du-goliath-en-combat.md     ✅
│   │   ├── relancer-un-1-avec-la-chance-du-halfelin.md               ✅
│   │   ├── jouer-acharnement-et-poussee-d-adrenaline-de-l-orc.md     ✅
│   │   └── avoir-l-avantage-aux-sauvegardes-d-espece.md              ✅ Charmé, Effrayé, Empoisonné, Ruse gnome
│   └── tasks/lister-les-capacites-de-niveau-1-que-le-moteur-ne-joue-pas.md  ✏️ traits des 9 espèces
└── memory/
    ├── architecture.md      ✏️
    ├── codebase-map.md      ✏️
    └── project-brief.md     ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | relire `dnd-engine/src/main/java/dnd/engine/rules/` et `CreatureProfile` | liste de ce que le moteur joue (résistance dans l'attaque, pas de sauvegarde, pas d'éclairage) | system |
| Happy path   | lire la tâche de la liste | chaque trait des 9 espèces et chaque option y figure, avec sa story, « porté par la fiche » ou « hors combat » | system |
| Edge case    | sorts mineurs des lignages et héritages | renvoyés aux stories des lanceurs de sorts | system |
| Edge case    | Polyvalent (don d'origine de l'Humain) | renvoyé à la story Historique | system |

## Tasks to do

### `1)` Stories des traits non joués

> Chaque trait d'espèce que le moteur ne joue pas a sa story.

1. Vérifier dans `rules/` et `CreatureProfile` ce que le moteur joue.
2. Écrire les stories au format du backlog (`type: story`, `status: proposed`, parent : l'epic des personnages), groupées par comportement commun.
3. Tenir la liste des espèces dans la tâche `lister-les-capacites-…` : Vision dans le noir, Transe, Connaissance de la pierre, Agilité et Discrétion halfelines, Forte carrure, Ingénieux, Vol draconique et Forme de géant (niveau 5, hors champ) notés sans story, avec la raison.

### `2)` Mémoire

> La mémoire décrit l'espèce dans la création guidée.

1. `architecture.md` : `SpeciesChoice` dans `Build`, champs dérivés `null` gardés, `size` et `speciesTraits`, route `species`, refus d'une compétence déjà maîtrisée.
2. `codebase-map.md` : `CreatureSize`, `Species`, `SpeciesOption`, `SpeciesChoice`.
3. `project-brief.md` : choix de l'espèce en création guidée.
4. Story au statut `done`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Chaque trait de niveau 1 des 9 espèces est joué, porté par la fiche, hors combat, ou renvoie à une story |
| 2 | La mémoire nomme chaque nouveau type, champ et route |
