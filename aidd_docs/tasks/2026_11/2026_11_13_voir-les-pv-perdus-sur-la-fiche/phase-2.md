---
status: done
---

# Instruction: la session envoie les coups d'un PJ, hors rejeu

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-server/src/main/java/solovtt/server/
├── SceneSession.java           ✏️ coups du PJ gardés pendant l'appel ; envoi après record, hors replaying ; ActionOutcome.warnings
├── CharacterSheetFollower.java ✅ interface : sustained(characterId, name, events), lève avec la raison
├── CampaignCatalog.java        ✏️ sustained(campaignId, id, events) : sources actives ; aucune ne le reconnaît → refus
├── CampaignSessions.java       ✏️ reçoit CampaignCatalog, passe le suivi à chaque session
└── Dtos.java                   ✏️ ActionResultDto.warnings
vtt-server/src/test/java/solovtt/server/
├── TestCharacters.java         ✏️ sustained : true pour guerriere et magicienne
├── CampaignSessionsTest.java   ✏️ constructeur
├── FicheSuitLeCombatTest.java  ✅
└── PlayerCharacterApiTest.java ✏️ de bout en bout avec une vraie fiche
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `SceneSession` avec un suivi qui enregistre ses appels ; Gobelin et PJ « aria » (12 PV) posés | aucun envoi | system |
| Happy path   | combat lancé, le Gobelin touche le PJ pour 5 | un envoi : `aria`, `[DamageDealt 5]` ; `warnings` vide | system |
| Happy path   | attaque hors combat qui touche le PJ | un envoi | system |
| Happy path   | Bouclier proposé (Magicienne de test), réponse, dégâts | l'envoi part avec la réponse (`answer`), pas avec l'attaque | system |
| Edge case    | nouvelle session sur le même `GameStore` (reprise), `SceneSession.replayed`, pas à pas de `CombatReplay` | aucun envoi | system |
| Edge case    | `resume` d'un combat en cours, puis un coup de plus | aucun envoi à la reprise, un seul pour le nouveau coup | system |
| Edge case    | « Nouvelle carte » après des coups | aucun envoi | system |
| Edge case    | coup raté, coup sur un monstre, attaque refusée (portée) | aucun envoi | system |
| Edge case    | le suivi lève « Fiches illisibles… » | l'attaque réussit et est gardée ; `warnings` : « Fiche d'Aria non mise à jour : … » | system |
| Edge case    | `CampaignCatalog.sustained` avec le module `dnd` inactif, ou aucune source qui reconnaît l'id | refus « fiche introuvable ou module inactif » | system |
| Happy path   | `@QuarkusTest` : fiche créée par `/dnd/sheets`, posée, frappée par `/attacks` | `GET /dnd/sheets/{id}` : PV baissés du montant du journal ; `warnings` vide | api |
| Edge case    | `@QuarkusTest` : coup sur la Guerrière de `TestCharacters` | `warnings` vide | api |
| Teardown     | chaque test API dans sa campagne (`Campagnes.creerUneNeuve()`) | aucune trace partagée | api |

## Tasks to do

### `1)` La session garde et envoie les coups d'un PJ

> Un coup vécu en direct atteint la fiche une seule fois ; un rejeu ne l'envoie jamais.

1. `CharacterSheetFollower` (fonctionnel) : `void sustained(String characterId, String name, List<GameEvent> events)`, lève avec la raison ; `NONE` ne fait rien.
2. `SceneSession` : constructeur complet avec le suivi (les autres passent `NONE`) ; l'abonnement au moteur garde, par id de fiche (`entries.get(targetId).id()` quand `profile() != null`), les `DamageDealt` émis pendant l'appel ; `recorded` et `create` les vident au départ.
3. `recordedAction` (attaque, lancement, fin de tour, fin du combat, déplacement, réponse) : après `record`, si `!replaying`, envoie chaque fiche et range chaque échec (`RuntimeException`, journalisé) en avertissement « Fiche de <nom> non mise à jour : <raison> » dans `ActionOutcome.warnings`.
4. `FicheSuitLeCombatTest` : cas `system` du Test Scope.

### `2)` Le cœur adresse les coups aux modules actifs

> Le suivi passe par le port, sans nommer de module.

1. `CampaignCatalog.sustained(campaignId, id, events)` : première source active qui rend `true` ; aucune → `IllegalStateException` « fiche introuvable ou module inactif ».
2. `@Inject CampaignSessions(Dice, CampaignStore, CampaignCatalog)` (seul constructeur injecté) : chaque session reçoit `(id, name, events) -> catalog.sustained(campaignId, id, events)` ; un constructeur de test à deux arguments garde le suivi `NONE`.
3. `Dtos.ActionResultDto.warnings` : liste, vide sans avertissement.
4. `TestCharacters.sustained` : `true` pour ses deux ids ; `PlayerCharacterApiTest` : cas `api`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Un coup sur un PJ, en combat ou non, part une seule fois avec la commande qui le porte ; reprise, `replayed`, `resume`, pas à pas et nouvelle carte n'envoient rien ; un suivi qui lève laisse le coup gardé et rend l'avertissement |
| 2 | De bout en bout, une vraie fiche perd les PV du journal ; un module inactif ou une fiche inconnue rend un refus ; les personnages de test n'avertissent pas |
