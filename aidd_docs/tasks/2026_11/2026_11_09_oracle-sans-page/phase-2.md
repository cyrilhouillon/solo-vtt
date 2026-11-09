---
status: pending
---

# Instruction: Mythic : tirer sans page

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/
├── src/modules/notebookModule.ts     ✏️ retire hasOpenPage et writeToOpenPage
├── src/components/NotebookScreen.vue ✏️ retire leur implémentation
└── modules/
    └── mythic/MythicPanel.vue        ✏️ plus de refus sans page, writeResult
aidd_docs/memory/
├── architecture.md                   ✏️ panneau Mythic
└── project-brief.md                  ✏️ Oracle Mythic sans page
```

## Test Scope

| Scenario | Action | Observable outcome | Actor |
| -------- | ------ | ------------------ | ----- |
| Setup | Serveurs 8081/5174, campagne neuve, Mythic actif, tables de sens importées (mots inventés) | Panneau prêt | system |
| Happy path | Page ouverte : Demander, Tester la scène, Contrôle gardé, Tirer un sens | Chaque résultat s'écrit dans la page comme avant ; deux réponses rapprochées = un seul Ctrl+Z | browser |
| Happy path | Aucune page ouverte (tous onglets fermés) : les mêmes gestes | Chaque geste tire au serveur, ouvre le dialogue avec le résultat et le copie ; le dernier résultat du panneau se met à jour | browser |
| Edge case | Sans page, « Contrôle perdu » | Chaos monte de 1 dans le panneau ; « Fin de scène : contrôle perdu → chaos a → b » dans le dialogue et le presse-papiers | browser |
| Edge case | Sans page, événement aléatoire visant Personnages avec une entrée liée | Le dialogue montre le lien ; collé dans une page, il reste un lien vivant | browser |
| Teardown | Tuer 8081 puis 5174 | Ports libérés, 8080/5173 intacts | system |

## Tasks to do

### `1)` Mythic

> Mythic tire sans page.

1. Dans `MythicPanel.vue`, retirer les quatre refus `hasOpenPage` (`drawMeaning`, `testScene`, `endScene`, `ask`).
2. Remplacer `writeToOpenPage` et `write()` par `writeResult` ; supprimer la branche « Scène close…, mais non écrite ».
3. Mettre à jour le commentaire du panneau (`mythic/index.ts` ne parle pas de la page : inchangé).

### `2)` Contrat

> Retirer l'ancienne écriture, désormais sans appelant.

1. Supprimer `hasOpenPage` et `writeToOpenPage` de `NotebookContext` et de `moduleContext`.
2. Vérifier par recherche qu'aucun appel ni type dérivé ne reste.

### `3)` Mémoire

> La doc ne dit plus Mythic refusé sans page.

1. `architecture.md` : panneau Mythic (« refusée sans page avant tout appel » → dialogue et copie).
2. `project-brief.md` : Oracle Mythic (« sans page, la question est refusée »).

## Test acceptance criteria

| Task | Acceptance criteria |
| ---- | ------------------- |
| 1 | Sans page, Demander, Tester la scène, Contrôle gardé/perdu et Tirer un sens ouvrent le dialogue et copient ; le chaos suit la fin de scène |
| 2 | `npm run build` passe ; aucune occurrence de `hasOpenPage` ni `writeToOpenPage` dans `vtt-web/src` et `vtt-web/modules` |
| 3 | `architecture.md` et `project-brief.md` ne disent plus qu'un tirage Mythic est refusé sans page |
