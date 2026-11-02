---
objective: "En combat, le joueur termine le combat quand il le décide (après confirmation sur place) : il n'y a plus de tours, les créatures restent sur la carte avec leurs PV, et le journal dit « Le combat se termine »."
status: in-progress
---

# Plan: terminer le combat à tout moment

Source: aidd_docs/backlog/stories/terminer-le-combat-a-tout-moment.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Moteur : la résolution `EndCombat` et l'événement `CombatEnded` | [`phase-1.md`](./phase-1.md) | none |
| 2   | Serveur : `POST /api/combat/end` | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Client : « Terminer le combat » avec confirmation dans la barre | [`phase-3.md`](./phase-3.md) | 2 |
| 4   | Mettre à jour la mémoire projet, le design et la story | [`phase-4.md`](./phase-4.md) | 3 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | La fin du combat à la main est un choix du joueur-MJ, pas une règle du SRD ; aucune règle D&D n'est ajoutée. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Regression | Faible. Ajouts seulement : un événement dans `GameEvent` (sealed : le seul `switch` sur `GameEvent` est `SceneSession.highlight`, qui a un `default` ; ailleurs des `instanceof`, vérifié), une résolution, une méthode de `SceneSession`, une route, un bouton. Couvert par `CombatTest` et `CombatApiTest` (lus) ; `rejouerLeJournalDuCombatRedonneLEtatFinal` sert de modèle au rejeu avec fin. Côté client, `settleAction` met `combat` à `null` : tableau, cercle de vitesse et bande d'initiative suivent `combat` (lu dans `App.vue`) ; mais le `watch` du tour ne recharge rien quand le combat devient `null`, donc la feuille ouverte garderait un `attackRefusal` périmé (« pas son tour »). Client sans tests : parcours navigateur sur 8081/5174. `mvn -q test` non lancé pendant la planification : filet au début de la phase 1. | 1, 2, 3 |
| Architecture | Départ : le combat ne s'arrête que si la dernière créature le quitte (`LeftCombat.apply` → `GameState.endCombat`, sans entrée dédiée au journal) ou avec une nouvelle carte (hors journal). Cible : `EndCombat`, une `CombatStep` comme `EndTurn`, émet `CombatEnded` qui applique `GameState.endCombat` : la fin se rejoue. | 1 |
| Documentation | `architecture.md` : `EndCombat` parmi les résolutions du combat ; fin refusée pendant une décision. `codebase-map.md` : `EndCombat` dans `combat/`, route `combat/end`. `project-brief.md` : fonctionnalité. `DESIGN.md` : contrôle de combat (bouton et confirmation). Story en `status: done`. | 4 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| L'état ne change que via `Engine.emit` ; rejouer le journal redonne l'état (`architecture.md`) | La fin est l'événement `CombatEnded`, vérifié par le rejeu. |
| Refus d'état des règles → `ActionRefusedException` (409), jamais `IllegalStateException` brute (`architecture.md`) | « Aucun combat en cours » passe par `refusable`. |
| Une décision en attente bloque toute action (`DecisionPendingException`, 409) | Terminer le combat est refusé tant que « Qui joue ? » ou Bouclier attend. |
| Dés scriptés, noms de tests en français, pas de mock ; règle nouvelle = test de la situation de table (`testing.md`) | Tests moteur et API de la fin du combat. |
| Ne jamais tester sur 8080/5173 (`testing.md`) | Parcours navigateur sur 8081/5174. |
| Contrôle de combat dans la barre (`DESIGN.md`) et confirmation en deux temps du retrait en combat (`TokenBoard.vue`, absente de `DESIGN.md`) | La confirmation de fin reprend le même geste : demande, puis confirmer ou annuler ; Échap annule. La phase 4 décrit ce geste dans `DESIGN.md`. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Terminer le combat est refusé (409) tant qu'une décision attend ; le moteur générique n'apprend pas à abandonner une décision. | Choix de l'utilisateur : le dialogue modal bloque déjà tout ; le joueur répond, puis termine. |
| Le bouton « Terminer le combat » se confirme sur place, dans la barre (« Terminer le combat ? » Confirmer / Annuler), sans raccourci clavier. | Choix de l'utilisateur : la fin est irréversible (relancer retire l'initiative). |
| La fin vide seulement le combat (ordre, tour, vitesse dépensée) : PV, réaction dépensée et bonus de CA restent tels quels. | La story garde les créatures avec leurs PV ; effets à durée et réaction hors combat sont des pièges connus (`architecture.md`), hors de cette story. |
