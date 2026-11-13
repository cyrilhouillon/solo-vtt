---
type: spike
status: resolved
parents:
  - aidd_docs/backlog/stories/voir-sur-la-fiche-les-pv-perdus-en-combat.md
  - aidd_docs/backlog/stories/creer-en-guide-un-magicien.md
---

# Spike : faire suivre à la fiche ce qui arrive au PJ en combat

## Question

La fiche est son propre agrégat à événements, distinct du combat, qui garde sa copie du PJ. Comment un coup du combat (PV perdus, emplacement dépensé) émet-il un événement sur la fiche ? Et comment le rejeu d'un combat, la reprise d'une partie au démarrage (rejeu de ses commandes), « Nouvelle carte » et la reprise d'un combat en cours évitent-ils de le réémettre ou de le perdre ?

## Decision

La forme des stories « voir sur la fiche les PV perdus en combat » et « créer en guidé un Magicien » (emplacements dépensés).

## Bounds

- Evidence needed : la garde actuelle par commandes et son rejeu (`SceneSession`, `SavedGame`, `CombatStore`, `CombatReplay`) ; le point où un événement du moteur (dégâts, emplacement dépensé) peut atteindre un autre agrégat sans entrer dans le journal du combat ; le stockage de la fiche dans le module D&D ; un essai jetable, hors dépôt, où un coup baisse les PV d'une fiche et où un rejeu ne les rebaisse pas.
- Stop when : un mécanisme esquissé, confirmé par l'essai, qui couvre le direct, la reprise, le rejeu et « Nouvelle carte » ; ou un cas impossible sans changer la garde par commandes, écart nommé.

## Investigation

| Attempt | Evidence | Result |
| ------- | -------- | ------ |
| Lecture de la garde (2026-11-13) | `SceneSession` (`recorded`, `record`, `replayNext`, `restore`, `resume`, `replayed`, `create`), `SavedGame`, `CampaignSessions`, `CampaignCatalog`, `PlayerCharacters`, `SheetCharacters`, `SheetStore`, `SheetHistory`, `GameEvent.DamageDealt` / `SpellSlotExpended` | Toute commande, en direct comme au rejeu, passe par `record` ; le rejeu (reprise au démarrage, `resume`, `replayed`, pas à pas de `CombatReplay`) y arrive avec `replaying = true` et n'écrit rien. « Nouvelle carte » ne rejoue rien. Une créature posée garde son entrée (`entries`), dont l'id est celui de la fiche pour un PJ (`profile() != null`). La fiche est un agrégat à événements dans `modules/dnd.json` ; `take` en tire la copie, PV actuels compris. |
| Essai jetable (worktree détaché du scratchpad, retiré ensuite ; `~/.m2` réinstallé depuis `main`) : `SceneSession` garde les événements émis pendant l'appel et, après la garde de la commande et hors rejeu, envoie ceux d'un PJ (`DamageDealt`, `SpellSlotExpended`) à un rappel par id de fiche ; `PlayerCharacters.sustained` (défaut vide) ; `SheetEvent.HitPointsLost(amount)` plié en PV ≥ 0, écrit `{ "type": "lost", "amount": n }` | `SpikeFicheTest` de `vtt-server` (session, fiche simulée) et de `dnd-server` (`SheetCharacters` réel) | Verts. Direct : un coup de 5 baisse la fiche de 12 à 7, un envoi. Reprise au démarrage, `replayed`, rejeu pas à pas : aucun envoi. « Nouvelle carte » : combat gardé à part, la nouvelle pose part de 7. `resume` : aucun envoi ; le coup suivant envoie son seul delta (7 → 1). Côté module : `HitPointsLost` gardé, relu par une autre instance, `take` rend les PV baissés, plancher à 0. Contre-épreuve : envoi placé avant la garde `replaying`, le test échoue à la reprise (`expected: <1> but was: <2>`). |

## Outcome

- Result :
  - **Mécanisme, sans changer la garde par commandes.** `SceneSession` garde les événements émis pendant l'appel ; après la garde de la commande (`record`) et jamais au rejeu (`replaying`, déjà levé par la reprise au démarrage, `resume`, `replayed` et le pas à pas de `CombatReplay`), elle envoie ceux d'un PJ (`DamageDealt` sur lui, `SpellSlotExpended`), groupés par id de fiche (l'id de son entrée, `profile() != null`). Le journal du combat ne change pas.
  - **Port.** `PlayerCharacters.sustained(campaignId, id, events)`, défaut vide ; `CampaignSessions` le passe à la session et l'adresse aux sources des modules actifs, sans nommer de module.
  - **Fiche.** Un événement de delta, `SheetEvent.HitPointsLost(amount)`, plié en PV ≥ 0 (écrit `lost`) ; un delta, pas une valeur, si bien qu'une retouche entre deux coups se compose. Les emplacements suivront le même chemin (événement de dépense).
  - **« Nouvelle carte »** ne rejoue rien : le combat reste à part, la pose suivante prend les PV de la fiche (`take`).
- Confidence : haute pour les PV (essai vert, contre-épreuve rouge sans la garde) ; moyenne pour les emplacements, même chemin mais non essayé (aucune fiche ne porte de sort).
- Remaining uncertainty :
  - arrêt entre la garde de la partie et l'écriture de `dnd.json` : le coup est perdu pour la fiche, jamais compté deux fois (ordre choisi) ;
  - fiche illisible (409) ou module désactivé : envoi journalisé, combat poursuivi ; prévenir le joueur ou non (story) ;
  - une saisie de fiche (`SheetEdited`, toutes les valeurs) partie d'un onglet périmé efface les PV perdus : dernier enregistrement gagnant, comme le Carnet ;
  - un combat repris garde la copie du PJ d'avant une retouche de la fiche ; seule une nouvelle pose relit la fiche ;
  - attaque hors combat : envoyée aussi dans l'essai ; à garder ou filtrer (story) ;
  - un même PJ posé deux fois : les deux copies baissent la même fiche.

## Follow-up

- Story « voir sur la fiche les PV perdus en combat » : débloquée ; à affiner sur deux choix, coups hors combat comptés ou non, et avertissement du joueur quand la fiche n'a pas pu suivre.
- Story « créer en guidé un Magicien » : la dépense d'emplacements passe par le même port et un événement de fiche de dépense.
- Plus tard, si utile : un spike sur la garde par événements plutôt que par commandes (une partie qui survit à un changement de règles), dont la question décisive est la reprise d'une décision en attente.
