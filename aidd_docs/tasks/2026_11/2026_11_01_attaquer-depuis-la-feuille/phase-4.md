---
status: done
---

# Instruction: mettre à jour la mémoire projet

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── aidd_docs/memory/
│   ├── architecture.md     ✏️ portées dans dnd-engine (distance en m fournie par l'appelant, euclidienne stricte) ; Engine et journal par carte dans SceneSession ; piège : Bouclier absent du serveur
│   ├── codebase-map.md     ✏️ ressources /api/attacks et /api/journal ; JournalPanel ; RangeRules, Attack
│   ├── project-brief.md    ✏️ fonctionnalité : attaque depuis la feuille, ciblage, journal ; ligne SRD 5.2 : référence officielle https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf, à consulter pour toute règle ou fiche (pas de wiki tiers)
│   └── testing.md          ✏️ dés scriptés côté serveur (ScriptedDice)
├── aidd_docs/backlog/stories/attaquer-depuis-la-feuille-en-ciblant-sur-la-carte.md  ✏️ status: done
└── dnd-engine/README.md    ✏️ ligne state/ : Attack ; portées
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | lire les fichiers de mémoire | état connu | cli |
| Happy path   | relire chaque fichier modifié | chaque fait cité existe dans le code (classe, ressource, port) | cli |
| Edge case    | un fait de la mémoire est devenu faux (« pas de journal ») | corrigé ou retiré | cli |

## Tasks to do

### `1)` Mémoire

> La mémoire décrit l'état réel après la story.

1. Mettre à jour les fichiers de l'arbre, sans répéter le code.
2. Passer la story à `done`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Chaque classe, ressource et commande citée existe (`grep`). La story est en `status: done`. |
