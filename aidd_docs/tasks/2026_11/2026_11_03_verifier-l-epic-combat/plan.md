---
objective: "Les deux preuves de succès de l'epic sont observées (combat de 2 personnages contre 3 adversaires joué de bout en bout dans le navigateur, journal complet, reprise à l'identique), puis l'epic est close ou ses écarts deviennent des items du backlog."
status: implemented
---

# Plan: vérifier l'epic « jouer un combat D&D 2024 en local » de bout en bout

Source: aidd_docs/backlog/epics/jouer-un-combat-dnd-2024-en-local.md (Success Evidence) ; choix de l'utilisateur : vérifier l'epic avant de la clore, toutes ses stories étant `done`

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Scénario de l'epic en test de session | [`phase-1.md`](./phase-1.md) | none |
| 2   | Parcours navigateur du combat complet | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Clore l'epic ou reporter ses écarts | [`phase-3.md`](./phase-3.md) | 2 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | Vérification seulement : aucune règle D&D ajoutée ni changée. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Regression | Aucune : seul un test est ajouté (`vtt-server/src/test/java/solovtt/server/CombatDeBoutEnBoutTest.java`), aucun code de production modifié. `ResumeGameTest` couvre la reprise pendant Bouclier, mais ni un combat à 5 créatures jusqu'à `endCombat`, ni une créature à 0 PV. Le catalogue (`SrdCatalog.java:29-58`) a bien 2 personnages (Guerrière, Magicienne) et 3 adversaires (Gobelin, Squelette, Loup). `mvn -q test` lancé pendant la planification : 161 tests, 0 échec (exit 0) ; la phase 1 le relance comme filet. | 1 |
| Exigence « le journal retrace chaque action » | Interprétée comme chaque action du moteur : pose, retrait et déplacement libre sont hors journal par conception (`architecture.md`, `SceneSession.move` l. 217). Phase 1 le verrouille, phase 2 juge si c'est un écart. | 1, 2 |
| Runtime | Non vérifié : le parcours navigateur (dés 3D, dialogue de décision, reprise après redémarrage du serveur) n'est observable qu'en phase 2, sur 8081/5174. | 2 |
| Documentation | Epic : `status: done` si les deux preuves tiennent ; sinon un item par écart dans `aidd_docs/backlog/`, l'epic reste `ready`. Plan : `status: implemented`. | 3 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Dés scriptés, noms de tests en français (`testing.md`) | Test de session du scénario complet. |
| Rejouer redonne les mêmes `signature()` (`testing.md`) | La reprise se vérifie par signatures de créatures et de combat, comme `ResumeGameTest`. |
| Ne jamais tester sur 8080/5173 ; sauvegarde dans le scratchpad (`testing.md`) | Parcours navigateur et redémarrage du serveur de test sur 8081/5174. |
| Le joueur ne touche ni au code ni à l'état (epic) | Le parcours ne passe que par l'interface : ni Swagger, ni fichier de partie modifié à la main. |
| Prouver `works` par des preuves (`CLAUDE.md`) | Chaque preuve de l'epic est consignée avec capture ou extrait de journal dans `verification.md`. |
