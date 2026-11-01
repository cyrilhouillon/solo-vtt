---
status: pending
---

# Instruction: mettre à jour la mémoire projet

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── PRODUCT.md                             ✏️ transport tranché (REST JSON, Quarkus), Konva choisi
└── aidd_docs/
    ├── memory/
    │   ├── architecture.md                ✏️ vtt-server, vtt-web, scène générique, Konva isolé
    │   ├── codebase-map.md                ✏️ zones et points d'entrée (quarkus:dev, npm run dev)
    │   ├── package.md                     ✏️ vtt-server et vtt-web, premier consommateur
    │   ├── coding-assertions.md           ✏️ npm run build dans vtt-web
    │   ├── testing.md                     ✏️ @QuarkusTest, emplacements des tests
    │   └── project-brief.md               ✏️ contenu du dépôt, fonctionnalités
    └── backlog/stories/
        └── preparer-une-scene-de-combat.md ✏️ status: done
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | relire chaque fichier mémoire | écarts listés | system |
| Happy path   | suivre `codebase-map.md` pour lancer serveur et client | l'application démarre | cli |
| Edge case    | chercher « transport » indécis et « aucune interface » | plus aucune mention obsolète | system |

## Tasks to do

### `1)` Mémoire

> Une session future comprend serveur, client et scène sans relire le code.

1. Mettre à jour les fichiers listés, sans paraphraser le code.
2. Passer la story à `status: done`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | les commandes de `codebase-map.md` et `coding-assertions.md` s'exécutent telles quelles ; `grep -ri "pas encore décidé" PRODUCT.md aidd_docs/memory` ne renvoie plus le transport |
