---
status: pending
---

# Instruction: Client : « Terminer le combat » avec confirmation dans la barre

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/src/
├── api.ts                         ✏️
├── App.vue                        ✏️
└── components/CombatControl.vue   ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081/5174 (`testing.md`) ; carte, trois créatures, combat lancé, une attaque qui blesse | Barre « Round 1 · Tour de X », « Fin du tour », « Terminer le combat » | browser |
| Happy path   | Clic « Terminer le combat », puis « Confirmer » | Barre « Lancer le combat » ; journal « Le combat se termine » ; tableau sans colonne Init. ni bande ; jetons en place, PV de la cible inchangés ; feuille ouverte sans refus « pas son tour » ; plus de cercle de vitesse | browser |
| Edge case    | Clic « Terminer le combat », puis « Annuler » ou Échap | Combat intact ; le focus revient au bouton « Terminer le combat » | browser |
| Edge case    | Confirmation ouverte, puis « Fin du tour » ou F | Le tour passe ; la confirmation se ferme | browser |
| Edge case    | « Qui joue ? » en attente | Le dialogue modal bloque le bouton ; aucune requête | browser |
| Edge case    | Double clic sur « Confirmer » | Une seule requête (`combatBusy`) | browser |
| Teardown     | Arrêter 8081 puis 5174 | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌──────────────────────────────────────────────────────────────────────────┐
│ (1) Barre : Solo VTT · Carte … · 3 créatures                              │
│     (2) Round 2 · Tour de Loup  [Fin du tour]  (3) [Terminer le combat] (☾)│
├──────────────────────────────────────────────────────────────────────────┤
│     (4) confirmation, à la place de (3) :                                 │
│         Terminer le combat ?  [Confirmer]  [Annuler]                      │
└──────────────────────────────────────────────────────────────────────────┘
```

1. Barre de l'application, inchangée.
2. Contrôle de combat existant : round, créature qui joue, fin du tour.
3. Bouton ghost « Terminer le combat », en combat seulement ; après la fin, « Lancer le combat » le remplace avec (2).
4. Confirmation sur place, focus sur « Confirmer » ; Annuler ou Échap rend (3).

## Tasks to do

### `1)` Appel API

> Le client sait terminer le combat.

1. Ajouter `endCombat: () => request<ActionResult>('/api/combat/end', { method: 'POST' })` dans `api.ts`.

### `2)` Bouton et confirmation

> Un geste en deux temps, comme le retrait en combat dans `TokenBoard`.

1. Dans `CombatControl`, ajouter l'émission `endCombat` et un état `confirming` : en combat, un bouton ghost « Terminer le combat » (désactivé si `blocked`) ouvre « Terminer le combat ? » avec « Confirmer » (focus) et « Annuler ».
2. Annuler ou Échap ferme la confirmation et rend le focus au bouton ; toute fin de combat ou changement de tour la ferme.
3. Exposer `cancelEndCombat()` (vrai s'il y avait une confirmation) et l'appeler en tête du traitement d'Échap dans `App.vue`, avant `board.cancelRemoval()`.

### `3)` Fin du combat dans l'application

> Après la fin, rien ne reste du combat à l'écran.

1. Brancher `@end-combat="combatAction(api.endCombat)"` dans `App.vue`.
2. Quand `combat` passe à `null` après une action, recharger la feuille ouverte (`showSheet`) pour que `attackRefusal` suive ; vérifier que tableau, cercle de vitesse et anneau « à son tour » disparaissent.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe ; « Confirmer » envoie `POST /api/combat/end` (onglet réseau). |
| 2 | Un seul clic ne termine pas le combat ; Annuler et Échap le laissent intact, focus sur le bouton. |
| 3 | Après « Confirmer » : « Lancer le combat », journal « Le combat se termine », créatures et PV en place, feuille sans refus de tour. |
