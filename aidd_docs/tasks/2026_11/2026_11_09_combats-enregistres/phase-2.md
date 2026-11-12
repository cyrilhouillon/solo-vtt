---
status: done
---

# Instruction: Serveur : rejeu pas à pas d'un combat terminé

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-server/src/main/java/solovtt/server/
├── CombatReplay.java          ✅ découpe un combat en pas et rend l'état après chacun
├── SceneSession.java          ✏️ rejeu commande par commande, observable
├── Dtos.java                  ✏️ ReplayDto, ReplayStepDto
└── CombatReplayRoute (dans CombatsResource.java de la phase 1) : GET /{combatId}/replay
vtt-server/src/test/java/solovtt/server/
└── CombatReplayTest.java      ✅
```

## Test Scope

| Scenario | Action | Observable outcome | Actor |
| -------- | ------ | ------------------ | ----- |
| Setup | Combat scripté : lancement, déplacement de l'active, repositionnement libre d'une autre, attaque avec réaction Bouclier (décision + réponse), fin de tour, fin du combat | Combat terminé gardé | api |
| Happy path | `GET …/combats/{id}/replay` | Pas 0 = état au lancement (avant l'initiative) ; un pas par action : lancement, déplacement, attaque (avec sa réponse), fin de tour, fin ; chaque pas porte ses entrées de journal, sa cible et ses PV, la scène, le combat et les feuilles | api |
| Edge case | Repositionnement libre | Pas de pas à lui : appliqué au pas suivant | api |
| Edge case | Attaque suspendue par une décision | Attaque et réponse forment un seul pas | api |
| Edge case | Combat en cours | 409 « combat en cours » | api |
| Edge case | Combat dont le rejeu échoue | 409, le Carnet n'est pas touché | api |
| Teardown | Campagne en mémoire | Rien sur disque | system |

## Tasks to do

### `1)` Découper en pas

> Un pas est une action ; le repositionnement libre et l'affichage n'en sont pas.

1. `SceneSession` : rejouer une `SavedGame` en appelant un observateur après chaque commande (nombre d'entrées du journal ajoutées, décision en attente).
2. `CombatReplay.of(SavedGame, combatId)` : rejoue jusqu'au `StartCombat` de ce combat (pas 0), puis ferme un pas après chaque commande qui ajoute au journal et ne laisse aucune décision en attente ; une commande sans entrée (repositionnement libre, grille) rejoint le pas suivant.
3. Pour chaque pas : entrées ajoutées, cible et ses PV, `SceneDto`, `CombatDto` (ou `null`), feuilles de toutes les créatures (`CreatureSheetDto`).

### `2)` Servir le rejeu

> Une seule requête rend tout le combat.

1. `GET /combats/{combatId}/replay` → `ReplayDto { id, steps: ReplayStepDto[] }` ; 409 si le combat n'est pas terminé ou illisible, 404 inconnu.

## Test acceptance criteria

| Task | Acceptance criteria |
| ---- | ------------------- |
| 1 | Le combat scripté donne exactement les pas attendus, le repositionnement libre dans le pas suivant, l'attaque et sa réponse dans un seul pas ; l'état du dernier pas égale celui de `GET …/combats/{id}` |
| 2 | La route rend le rejeu d'un combat terminé, 409 pour un combat en cours, 404 pour un inconnu |
