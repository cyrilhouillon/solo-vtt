---
objective: "Depuis le panneau Mythic, le joueur teste sa scène attendue au chaos de la campagne (attendue, modifiée avec son ajustement de scène, ou interrompue avec un événement aléatoire) puis la clôt en disant s'il a gardé le contrôle (chaos −1 ou +1, entre 1 et 9) ; chaque résultat s'écrit dans la page ouverte."
status: implemented
---

# Plan: ouvrir et clore une scène

Source: aidd_docs/backlog/stories/ouvrir-et-clore-une-scene.md (epic `aidd_docs/backlog/epics/jouer-sa-campagne-avec-mythic-gme-depuis-le-carnet.md`, order 5, dernière story `ready` de l'epic ; s'appuie sur l'oracle, les tables de sens et le tirage dans les listes, tous `done`).

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Le moteur Mythic teste une scène attendue et ajuste le chaos en fin de scène | [`phase-1.md`](./phase-1.md) | none |
| 2   | Le serveur Mythic teste et clôt une scène | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Le panneau ouvre et clôt une scène dans la page | [`phase-3.md`](./phase-3.md) | 2 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://wordmillgames.itch.io/mythic-game-master-emulator-second-edition (copie locale de l'utilisateur, `documents/`, hors dépôt) | p. 67 : tester la scène attendue = 1d10 ; au-dessus du chaos, attendue ; égal ou dessous, impair (1, 3, 5, 7, 9) modifiée, pair (2, 4, 6, 8) interrompue. p. 70-72 : table d'ajustement de scène (1d10) : 1 Retirer un personnage, 2 Ajouter un personnage, 3 Réduire ou retirer une activité, 4 Intensifier une activité, 5 Retirer un objet, 6 Ajouter un objet, 7-10 Deux ajustements (relancer jusqu'à deux résultats ; un nouveau 7-10 est ignoré et relancé ; deux résultats contraires : ignorer le second, garder le premier) ; un résultat impossible dans le contexte se relance, au jugement du joueur. p. 72 : scène interrompue = un événement aléatoire (focus, sens, liste), interprété en contexte. p. 114 : fin de scène, contrôle gardé : chaos −1, perdu : +1, jamais hors de 1 à 9 (le modificateur est ignoré). |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Aucune dépendance nouvelle ; deux routes de plus sous `/api/campaigns/{id}/mythic`, découvertes par Quarkus comme `MythicResource` (même classe). Non vérifié sur le serveur assemblé avant la phase 3 (parcours 8081 / 5174). | 2, 3 |
| Regression | Faible. Le tirage d'un événement (focus, sens, liste) sort de `FateQuestion.ask` (l. 29-36) vers `RandomEvent`, à ordre de dés identique : verrouillé par `FateQuestionTest` (14 tests). La lecture paresseuse des listes de `MythicResource.ask` (l. 156-160) devient une méthode partagée : verrouillée par `MythicResourceTest` (tirage dans Personnages, Fils, lien, liste perdue). `result.ts` : les paragraphes de l'événement sortent d'`answerBlocks` (l. 56-61) pour servir aussi à la scène interrompue ; `MythicPanel.vue` : `last` gagne deux formes. Pas de phase filet : les tests existants verrouillent l'existant, et le client se revérifie au navigateur en phase 3. | 1, 2, 3 |
| Architecture | Départ : le moteur Mythic répond à une question (`FateQuestion`), dont un double tire un `RandomEvent` ; le chaos ne change qu'au réglage du joueur (`PUT chaos`). Cible : `RandomEvent.roll` tire un événement pour qui en a besoin (question, scène interrompue) ; `SceneCheck.test` teste une scène attendue ; `ChaosFactor.afterScene(inControl)` porte la règle de fin de scène. Le serveur tire et garde le chaos, le client écrit, comme pour l'oracle. Aucun état de scène n'est gardé. Le cœur ne nomme toujours pas Mythic (`grep -ril mythic vtt-server/src vtt-web/src` vide aujourd'hui). | 1, 2 |
| Documentation | `architecture.md` (routes `scenes`, `scenes/end`, ordre des dés, écrit dans la page), `codebase-map.md` (`SceneCheck`, `SceneAdjustment`), `project-brief.md` (ouvrir et clore une scène), `DESIGN.md` (section Scène du panneau). Story `status: done` ; plan `status: implemented`. L'epic reste `ready` : sa vérification d'ensemble est une tâche à part, comme pour les epics Combat et Carnet. | 1, 2, 3 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Le livre de Mythic est commercial : jamais dans le dépôt ; les tests n'emploient que des mots inventés (`architecture.md`) | Les libellés français des ajustements sont écrits par nous ; aucune citation du livre. |
| Seuls les assemblages nomment un module (`architecture.md`) | Tout vit dans `mythic-engine`, `mythic-server`, `vtt-web/modules/mythic/` ; `grep -ri mythic vtt-server/src vtt-web/src` reste vide. |
| Moteurs sans dépendance, hasard injecté (`Dice`), dés scriptés en test (`testing.md`) | Le test de scène et l'ajustement vivent dans `mythic-engine`, en Java pur. |
| Le serveur tire, le client écrit dans la page en nœuds standard ; refus sans page avant tout appel (`architecture.md`, oracle et sens) | Même chemin pour la scène et sa clôture : sans page, rien n'est tiré et le chaos ne bouge pas. |
| Serveurs de test 8081 / 5174, chemins dans le scratchpad (`testing.md`) | Parcours navigateur de la phase 3. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Aucun état de scène gardé (ni numéro, ni « scène ouverte ») : tester une scène et la clore sont deux gestes indépendants du panneau ; la page garde la trace | La story ne demande que l'écrit dans la page ; le livre ne numérote pas les scènes. Un état ouvert / clos ajouterait persistance et cas d'erreur sans valeur pour l'acceptation. |
| Clore = `POST …/mythic/scenes/end { inControl }` : le serveur applique `ChaosFactor.afterScene`, garde le chaos et rend avant / après ; pas de `PUT chaos` depuis le client | La règle −1 / +1 est de Mythic, donc dans le moteur, testée ; le client n'ajoute aucune règle. |
| Deux ajustements : relancer 7-10 ; un doublon (deux fois le même) est gardé ; un second résultat contraire du premier (ajouter / retirer le même genre) est ignoré et l'ajustement reste unique, comme le dit le livre (« ignorer le second, garder le premier ») | Lecture littérale du livre, déterministe et testable ; tous les jets sont rendus pour l'écrit. |
| Un ajustement impossible dans le contexte n'est pas relancé par l'application | Jugement du joueur (p. 70), comme « Choisir » pour une ligne vide. |
| Scène interrompue : même événement que celui d'une question (focus, sens si tables, tirage dans la liste visée), par `RandomEvent.roll` | p. 72 : « comme un événement aléatoire » ; une seule implémentation. |
| La scène attendue est facultative (≤ 500 caractères), comme la question | Même contrat que `QuestionRequest`. |
