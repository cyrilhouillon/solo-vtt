---
objective: "Quand une règle demande un choix (réaction Bouclier de la Magicienne), la partie s'arrête : la question et ses options s'affichent dans un dialogue modal, aucune autre action n'est possible tant que le joueur n'a pas répondu, puis l'action reprend et son résultat tient compte du choix."
status: implemented
---

# Plan: répondre à une décision du moteur

Source: aidd_docs/backlog/stories/repondre-a-une-decision-du-moteur.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Serveur : décision en attente, réponse et blocage | [`phase-1.md`](./phase-1.md) | none |
| 2   | Client : dialogue modal de décision | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Mettre à jour la mémoire projet | [`phase-3.md`](./phase-3.md) | 2 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/dialog | `showModal()` rend le reste de la page inerte et piège le focus ; Échap déclenche l'événement `cancel`, que `preventDefault()` empêche de fermer le dialogue. Non garanti : Chromium (close watchers) peut fermer au second Échap sans activation utilisateur ; le dialogue se rouvre donc sur `close` tant que la décision attend. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Regression | Moyenne. `SceneSession.rules()` est remplacé par `StandardRules.create()` : `ShieldReaction` revient, donc une attaque touchée contre la Magicienne, que Bouclier ferait rater, s'arrête désormais. `AttackApiTest.laMagicienneToucheeNePoseAucuneDecision` (d20 15 → 19 contre CA 12, Bouclier inutile à 17) reste vrai : `ShieldReaction.appliesTo` exige `total < CA + 5`. `AttackResultDto` gagne un champ `decision` ; le client lit déjà `entries`, `targetId`, `targetHitPoints`. Filet : les tests d'`AttackApiTest`, `SceneApiTest`, `SceneWithoutMapTest` et les 4 tests Bouclier d'`AttackRulesTest` (lus). Pas de phase de filet dédiée : le moteur ne change pas. | 1, 2 |
| Architecture | Départ : le serveur n'enregistre pas `ShieldReaction` (`SceneSession.rules`, liste recopiée de `StandardRules`) et ignore le `StepResult` de `engine.submit`. Cible : la session enregistre `StandardRules.create()`, garde la décision en attente et la cible de l'action suspendue, refuse attaque, pose et retrait tant qu'elle attend (409), et reprend l'action par `engine.answer`. Le moteur générique ne change pas. | 1 |
| Documentation | `architecture.md` : retirer les pièges « le serveur n'enregistre pas `ShieldReaction` » et « une nouvelle règle doit y être ajoutée aussi » ; ajouter la décision en session et le blocage. `codebase-map.md` : ressource `decisions`, composant `DecisionDialog`. `project-brief.md` : fonctionnalité. Story en `status: done`. | 3 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| L'état ne change que via `Engine.emit` ; une seule décision en attente (`architecture.md`) | La reprise passe par `engine.answer` ; le serveur ne simule rien. |
| Le handler d'une décision capture le moteur (`architecture.md`) | « Nouvelle carte » jette le moteur et sa décision ; rien d'autre ne doit toucher l'état pendant l'attente. |
| Dés toujours scriptés, noms de tests en français, pas de mock (`testing.md`) | Tests d'API de la décision |
| Ne jamais tester sur 8080/5173 (`testing.md`) | Le parcours navigateur se fait sur 8081/5174. |
| Univers visuel et tokens de `DESIGN.md` | Le dialogue reprend boutons et couleurs existants. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Le serveur enregistre `StandardRules.create()` tel quel ; la copie de `SceneSession.rules()` disparaît. | Plus de double liste à maintenir (finding de la revue de l'attaque) ; toutes les décisions arrivent jusqu'au client. |
| Pendant une décision, le serveur refuse attaque, pose et retrait (409 avec la raison) ; « nouvelle carte » reste possible et remet la session à zéro. | Choix de l'utilisateur : retirer la cible ferait échouer la reprise ; une nouvelle carte ne corrompt rien. |
| La décision s'affiche dans un `<dialog>` modal, sans fermeture par Échap ni bouton d'annulation. | Choix de l'utilisateur : le modal bloque nativement toute autre action. Contrepartie : la carte est masquée pendant le choix, et le rail « Nouvelle carte » est inaccessible depuis cet onglet. |
| La décision en attente se relit (`GET /api/decisions/pending`) au chargement et après une resynchronisation. | Un rechargement de page ne doit pas perdre la question ; le serveur la garde en mémoire. |
| Le joueur répond à toutes les décisions, quel que soit `chooserId`. | Jeu solo : le joueur contrôle personnages et adversaires (epic). |
