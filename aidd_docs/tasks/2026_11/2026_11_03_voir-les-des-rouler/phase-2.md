---
status: pending
---

# Instruction: Client : les dés roulent sur la piste puis le verdict se pose

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/src/
├── types.ts                      ✏️
├── App.vue                       ✏️
└── components/DiceTray.vue       ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081/5174 (`testing.md`), carte, Guerrière et Squelette au contact | piste vide | browser |
| Happy path   | Attaque à l'Épée longue | un d20 et le d10 roulent sur la piste, s'arrêtent sur les valeurs des entrées du journal, puis le verdict se pose ; la hauteur de la piste et de la carte ne bouge pas | browser |
| Edge case    | Attaque en portée longue (désavantage) | deux d20 ; le gardé plein, l'autre atténué et barré | browser |
| Edge case    | Raté | seul le d20 roule ; aucun dé de dégâts | browser |
| Edge case    | Bouclier posé pendant l'attaque | les dés roulent quand l'attaque est tranchée (après la réponse), avec les d20 tirés avant la question | browser |
| Edge case    | Rechargement de la page | la dernière attaque s'affiche avec ses dés immobiles, sans roulement | browser |
| Edge case    | Émulation `prefers-reduced-motion: reduce` | dés et verdict posés d'emblée, aucun roulement | browser |
| Edge case    | Lecteur d'écran | l'annonce `aria-live` dit l'issue une seule fois ; la piste reste `aria-hidden` | browser |
| Teardown     | Arrêter 8081 puis 5174 | ports libres, 8080/5173 intacts | system |

## Wireframe

```txt
┌─ piste à dés (hauteur fixe, 3 lignes) ──────────────────────────────┐
│ (1) Verdict                (2) Jet d'attaque ╎ (3) Dégâts  (4) Cible │
│     Calcul                     [15] [4̶]      ╎  [6]       nom jauge │
│     Annonce de l'action        d20  d20       ╎  d10                 │
└──────────────────────────────────────────────────────────────────────┘
```

1. Verdict, calcul, annonce : inchangés.
2. Jet d'attaque : les d20 (l'écarté atténué et barré) puis les dés bonus.
3. Dégâts : les dés de dégâts, après un filet ; absents sur un raté.
4. Cible : nom et jauge, inchangés.

## Tasks to do

### `1)` Types

> Le client connaît le fait `dice`.

1. Ajouter `RolledDice` (`sides`, `values`, `kept: number | null`) et `dice: RolledDice | null` à `JournalEntry`.

### `2)` Résultat de la piste

> La piste reçoit les dés de l'attaque, rangés par moment.

1. Dans `TrayResult`, ajouter `attackDice` et `damageDice` (les entrées `dice` de l'action, avant et après l'entrée `OUTCOME`) et `live: boolean`.
2. `trayFromJournal` les remplit ; `live` vaut vrai seulement depuis `settleAction`, faux au chargement et à la reprise.

### `3)` Dés sur la piste

> Les dés roulent puis s'arrêtent sur les valeurs du serveur.

1. Rendre un dé par valeur (faces en légende, valeur au centre), jet d'attaque puis filet puis dégâts, dans la hauteur fixe actuelle ; le d20 écarté est atténué et barré (forme, pas seulement teinte).
2. Si `live` : faces qui défilent ~600 ms (affichage seulement, jamais montré comme résultat), puis arrêt sur la valeur ; le verdict se pose après l'arrêt. Sinon, et sous `prefers-reduced-motion`, tout est posé d'emblée.
3. Émettre `landed` quand le verdict est posé (à l'arrêt des dés, ou d'emblée) ; une nouvelle attaque relance l'animation (clé sur `action`).
4. À une largeur où les dés ne tiennent pas, ils passent avant la cible et se coupent plutôt que de pousser la piste (The Still Table Rule).

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` sans erreur de type. |
| 2 | Après une attaque, la piste reçoit les dés de l'action ; au rechargement, `live` est faux. |
| 3 | Les valeurs à l'arrêt égalent celles des entrées `dice` (vérifiées dans la réponse réseau) ; hauteur de la piste identique avant et après ; aucun roulement sous `prefers-reduced-motion` ni au rechargement. |
