---
status: pending
---

# Instruction: parcours navigateur du combat complet

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── aidd_docs/tasks/2026_11/2026_11_03_verifier-l-epic-combat/
    └── verification.md ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `mvn -q install -DskipTests`, serveur de test 8081 avec `solovtt.save.path` dans le scratchpad, Vite 5174 (`testing.md`) | page sur 5174, aucune carte | system |
| Happy path   | Dans l'interface seule : nouvelle carte, poser Guerrière, Magicienne, Gobelin, Squelette, Loup ; lancer le combat ; à chaque tour déplacer et attaquer depuis la feuille en ciblant sur la carte ; répondre à Bouclier ; finir les tours jusqu'à un adversaire à 0 PV ; terminer le combat | dés 3D puis verdict, PV des jetons à jour, journal qui retrace chaque action, ordre de jeu et créature active à jour | browser |
| Edge case    | Arrêter le serveur 8081 pendant la décision Bouclier, le relancer, recharger la page | même carte, positions, PV, ordre, tour et journal ; le dialogue Bouclier revient | browser |
| Edge case    | Arrêter et relancer après la fin du combat | créatures et PV restent, aucun combat | browser |
| Teardown     | Tuer les processus sur 8081 puis 5174 (`testing.md`) | 8080 et 5173 intacts | system |

## Tasks to do

### `1)` Lancer les serveurs de test

> Isoler le parcours de la partie de l'utilisateur.

1. Suivre `testing.md`, étapes 1 à 3, sauvegarde dans le scratchpad.

### `2)` Jouer le combat dans le navigateur

> Observer la première preuve de l'epic.

1. Jouer le scénario Happy path sans Swagger ni fichier édité.
2. Consigner dans `verification.md` chaque écart (bloquant, gênant, cosmétique) avec capture ou extrait de journal.
3. Juger si l'absence au journal de la pose, du retrait et du déplacement libre gêne la lecture du combat ; le consigner comme écart ou comme conforme.

### `3)` Vérifier la reprise

> Observer la seconde preuve de l'epic.

1. Redémarrer 8081 pendant Bouclier, puis après la fin du combat ; comparer avant et après.
2. Consigner le résultat dans `verification.md`.

### `4)` Arrêter les serveurs de test

> Rendre la machine telle qu'avant.

1. Tuer 8081 puis 5174, jamais 8080 ni 5173.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | 5174 répond et appelle 8081 ; `~/.solo-vtt/partie.json` inchangé. |
| 2 | `verification.md` dit, preuve à l'appui, si le combat 2 contre 3 se joue de bout en bout et si le journal retrace chaque action. |
| 3 | `verification.md` dit, preuve à l'appui, si la partie reprend à l'identique, décision comprise. |
| 4 | Plus rien n'écoute sur 8081 ni 5174 ; 8080 et 5173 inchangés. |
