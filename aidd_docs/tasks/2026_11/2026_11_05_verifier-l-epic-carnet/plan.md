---
objective: "Les cinq preuves de succès de l'epic « tenir le carnet de sa campagne » sont observées (session liée puis retour du combat, renommage suivi et liens entrants, recherche, corbeille liens compris, reprise après redémarrage), puis l'epic est close ou ses écarts deviennent des items du backlog."
status: implemented
---

# Plan: vérifier l'epic « tenir le carnet de sa campagne » de bout en bout

Source: aidd_docs/backlog/epics/tenir-le-carnet-de-sa-campagne.md (Success Evidence) ; choix de l'utilisateur : vérifier l'epic avant de la clore, toutes ses stories étant `done`

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Scénario de l'epic en test du Carnet | [`phase-1.md`](./phase-1.md) | none |
| 2   | Parcours navigateur du Carnet | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Clore l'epic ou reporter ses écarts | [`phase-3.md`](./phase-3.md) | 2 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | Vérification seulement : aucune bibliothèque ajoutée ni changée. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Regression | Aucune : seul un test est ajouté (`vtt-server/src/test/java/solovtt/server/CarnetDeBoutEnBoutTest.java`), aucun code de production modifié. Les briques sont couvertes une à une (`NotebookStoreTest`, `JsonFileNotebookStoreTest`, `PageLinksTest`, `PageSearchTest`, `PageTreeTest`, `NotebookApiTest`), mais aucun test n'enchaîne le scénario de l'epic ni ne relit tout par un nouveau `FileCampaignStore`. `mvn -q test` lancé pendant la planification : 314 tests, 0 échec (exit 0) ; la phase 1 le relance comme filet. | 1 |
| Exigence « le lien suit le renommage » | Côté serveur, prouvée par l'absence de titre dans le nœud `pageLink` (`architecture.md`) : le contenu des sessions ne change pas au renommage, `PageLinks.citing` les rend toujours. L'affichage du nouveau titre n'est observable qu'au navigateur. | 1, 2 |
| Runtime | Non vérifié : arrivée sur la dernière page, onglets au retour du combat, liens barrés puis revivants, adresses et reprise dans l'interface ne sont observables qu'en phase 2, sur 8081/5174. | 2 |
| Documentation | Epic : `status: done` si les cinq preuves tiennent ; sinon un item par écart dans `aidd_docs/backlog/`, l'epic reste `ready`. Plan : `status: implemented`. | 3 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Noms de tests en français, pas de mock (`testing.md`) | Test du scénario complet sur le vrai adaptateur fichier (`@TempDir`). |
| Ne jamais tester sur 8080/5173 ; campagnes et images dans le scratchpad (`testing.md`) | Parcours navigateur et redémarrage du serveur de test sur 8081/5174, sans toucher `~/.solo-vtt/`. |
| Le joueur ne passe que par l'interface (epic) | Ni Swagger ni fichier édité à la main ; l'API n'est lue qu'en `GET` pour comparer avant et après redémarrage. |
| Prouver `works` par des preuves (`CLAUDE.md`) | Chaque preuve de l'epic est consignée avec capture ou réponse `GET` dans `verification.md`. |
