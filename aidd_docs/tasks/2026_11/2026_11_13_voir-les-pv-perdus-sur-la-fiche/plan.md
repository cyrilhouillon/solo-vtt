---
objective: "Chaque coup reçu par un PJ posé, en combat ou non, baisse les PV actuels de sa fiche du même montant, une seule fois : gardé après la fin du combat et un redémarrage, jamais réémis par une reprise ou un rejeu, repris par la pose suivante ; un échec de suivi s'affiche au joueur sans défaire le coup."
status: implemented
---

# Plan: voir sur la fiche les PV perdus en combat

Source: aidd_docs/backlog/stories/voir-sur-la-fiche-les-pv-perdus-en-combat.md (mécanisme : aidd_docs/backlog/spikes/faire-suivre-a-la-fiche-ce-qui-arrive-au-pj-en-combat.md)

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | PV perdus dans l'histoire de la fiche et port `sustained` | [`phase-1.md`](./phase-1.md) | none |
| 2   | La session envoie les coups d'un PJ, hors rejeu | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Avertissement à l'écran de combat et parcours navigateur | [`phase-3.md`](./phase-3.md) | 2 |
| 4   | Mémoire et backlog | [`phase-4.md`](./phase-4.md) | 1, 2, 3 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | Le mécanisme vient du spike, essai jetable vert et contre-épreuve rouge (2026-11-13) |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| architecture | Aujourd'hui la fiche n'est lue qu'à la pose (`PlayerCharacters.take`, `CampaignCatalog.entry`) ; le combat ne lui écrit rien. Changement : `PlayerCharacters.sustained(campaignId, id, events)` (défaut `false`, « pas l'un des miens ») ; `SheetEvent.HitPointsLost(amount)`, un delta plié en PV ≥ 0, écrit `{ "type": "lost", "amount": n }` ; `SceneSession` garde les `DamageDealt` d'un PJ (entrée à `profile() != null`, id de fiche = `entry.id()`) émis pendant l'appel, puis, après `record` et seulement hors `replaying`, les envoie par fiche à un rappel que `CampaignSessions` tire de `CampaignCatalog.sustained` (sources des modules actifs, sans nommer de module). | phase 1, 2 |
| architecture | Concurrence : `SheetResource` écrit `dnd.json` sous son propre verrou (`synchronized`, `SheetResource.java:116-194`), `SheetCharacters` est un autre bean. Un coup écrit pendant une saisie perdrait l'une des deux écritures. Changement : `SheetCharacters` délègue l'écriture à une méthode `synchronized` de `SheetResource` (singleton Quarkus REST par défaut, annoté `@Singleton` pour le rendre explicite). Injection d'une ressource JAX-RS dans un bean : non vérifiée, à observer au démarrage sur 8081 (phase 3) ; repli : un verrou statique de `SheetStore`. | phase 1, 3 |
| regression | Moyen. `SheetHistory.replay` et `SheetStore.EventFile` gagnent un cas (`SheetHistoryTest`, `SheetResourceTest`, `SheetCharactersTest`) ; le constructeur de `SheetCharacters` change (`SheetCharactersTest:24`) ; `SceneSession.ActionOutcome` gagne `warnings` (construit à `SceneSession.java:336`, `:392`) ; `ActionResultDto` gagne `warnings` (un champ ajouté : seul `DecisionApiTest:119` lit un champ du DTO, sans casser) ; `CampaignSessions` gagne le catalogue (`CampaignSessionsTest` le construit à 2 arguments). En `@QuarkusTest`, `TestCharacters` (module `dnd`) et `SheetCharacters` sont deux sources : `TestCharacters.sustained` rend `true` pour ses deux ids, sinon chaque coup sur la Guerrière avertirait. La reprise (`ResumeGameTest`, `CampaignSessionsTest`, `CombatReplayTest`, `CombatsEnregistresTest`) passe déjà par `replaying` : le spike a montré la contre-épreuve. Pas de phase filet : ces tests couvrent déjà la reprise et le rejeu. | phase 1, 2 |
| runtime | Aucune dépendance hors dépôt. Parcours (poser un PJ, le frapper, ouvrir sa fiche ; redémarrer ; reposer) non observé : navigateur sur 8081 / 5174. | phase 3 |
| documentation | `architecture.md` (suivi de la fiche, port `sustained`, `HitPointsLost`, avertissement, pièges : coup perdu si arrêt entre les deux écritures, saisie périmée qui efface, PJ posé deux fois, combat repris avec la copie d'avant), `codebase-map.md` (`PlayerCharacters.sustained`), `project-brief.md` (les PV suivent la fiche), story `done`. | phase 4 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Les moteurs n'ont aucune dépendance runtime | `HitPointsLost` et le port restent dans `dnd-engine`, sans Jackson |
| Un module n'est nommé que par les assemblages | `vtt-server` ne parle qu'à `PlayerCharacters`, collecté par CDI |
| Le rejeu d'un combat ne relit ni n'écrit jamais la fiche (epic) | l'envoi vit après la garde `replaying` |
| Une fiche d'avant se relit ; un document abîmé rend 409 et n'est jamais écrasé (`SheetStore`) | `sustained` sur un `dnd.json` abîmé lève sans écrire |
| Code et messages en français, identifiants en anglais ; tests aux noms français ; dés scriptés | toutes les phases |
| Tests navigateur sur 8081 / 5174 uniquement, jamais 8080 / 5173 | phase 3 |
| Vérifications avant commit : `mvn -q compile`, `mvn -q test`, `npm run build` | chaque phase |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Tout coup reçu par un PJ posé baisse sa fiche, en combat comme hors combat | décision du joueur : la jauge de la carte et la fiche ne divergent jamais |
| Un échec de suivi (document abîmé, module inactif, fiche supprimée, écriture) laisse le coup et la partie, et la réponse de l'action porte un avertissement que l'écran de combat affiche dans son bandeau | décision du joueur |
| La fiche reçoit un delta (`HitPointsLost`), pas une valeur | une retouche entre deux coups se compose (spike) |
| L'envoi suit la garde de la partie | un arrêt entre les deux perd le coup pour la fiche, jamais ne le compte deux fois (spike) |
