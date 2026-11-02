---
objective: "Après arrêt puis relance du serveur, le joueur retrouve la carte, les positions, les PV, l'ordre de jeu, le tour en cours, le journal et la décision en attente tels qu'il les a laissés."
status: implemented
---

# Plan: reprendre une partie à l'identique

Source: aidd_docs/backlog/stories/reprendre-une-partie-a-l-identique.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Serveur : la session enregistre ses commandes et ses dés derrière un port, puis les rejoue | [`phase-1.md`](./phase-1.md) | none |
| 2   | Brancher la sauvegarde sur un fichier configurable, valider dans le navigateur, mettre à jour la mémoire | [`phase-2.md`](./phase-2.md) | 1 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | Aucune règle D&D en jeu ; la persistance réutilise Jackson, déjà présent dans `vtt-server` (`quarkus-rest-jackson`). |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Un fichier JSON écrit sur disque, hors du navigateur (PRD : « conservées durablement, en dehors du navigateur »). Écriture atomique (fichier temporaire puis déplacement). Non vérifié sous Windows tant que la phase 2 n'a pas tourné. | 1, 2 |
| Regression | Moyen : toutes les méthodes qui modifient `SceneSession` (`create`, `place`, `remove`, `move`, `attack`, `startCombat`, `endTurn`, `endCombat`, `answer`, lues) enregistrent désormais leur commande, et le moteur reçoit des dés enveloppés. Couvert par `SceneApiTest`, `AttackApiTest`, `DecisionApiTest`, `CombatApiTest`, `MoveApiTest`, `SceneWithoutMapTest` : ils verrouillent déjà le comportement, pas de phase filet dédiée ; `mvn -q test` en début de phase 1. Le profil de test n'a pas de fichier de sauvegarde : sinon `SceneWithoutMapTest` et l'état partagé des `@QuarkusTest` dépendraient d'une partie laissée par un run précédent. | 1, 2 |
| Architecture | Départ : une session en mémoire ; le journal du moteur se rejoue, mais les positions, les créatures posées, les compteurs d'instances et la décision en attente (lambda, non sérialisable) n'y sont pas. Cible : la session tient un journal de commandes (chaque appel réussi, avec ses arguments et les dés tirés pendant l'appel) ; relancer = rejouer ces commandes sur une session neuve avec ces dés. Moteur, règles et client inchangés : le client relit déjà scène, journal, décision et combat au chargement (`App.vue`, `onMounted`). | 1 |
| Risques connus, hors périmètre | (1) Une règle qui lève une exception après avoir émis des événements laisse la mémoire modifiée sans commande enregistrée : défaut existant (la pile de l'`Engine` n'est pas vidée), non corrigé ici ; la partie relancée est alors celle d'avant l'action. (2) En `quarkus:dev` sur 8080, un rechargement après modification d'une règle peut faire échouer le rejeu : le fichier de l'utilisateur est mis de côté horodaté, pas perdu. (3) Réécriture complète et rejeu intégral : coût linéaire par commande, négligeable à l'échelle d'un combat (non mesuré). | 1 |
| Documentation | `architecture.md` : décision (sauvegarde par commandes rejouées) et pièges (retirer « La persistance n'existe pas encore » et « une partie en attente de décision ne peut pas encore être sauvegardée » ; ajouter « une sauvegarde suppose le code qui l'a écrite »). `codebase-map.md` : fichier de sauvegarde, classes ajoutées. `project-brief.md` : fonctionnalité. `testing.md` : chemin de sauvegarde des serveurs de test. Story en `status: done`. | 2 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Moteurs sans dépendance runtime (`architecture.md`, `package.md`) | Jackson et l'accès disque restent dans l'adaptateur de `vtt-server`. |
| Nommer par intention, découper par responsabilité (`CLAUDE.md`) | Le port dit ce que veut la session (charger, sauver, mettre de côté), pas comment. |
| Le hasard est injecté (`Dice`) | Les dés sont notés à la source, rejoués par `Dice.scripted`. |
| Une décision en attente bloque toute action sauf « nouvelle carte » (`architecture.md`) | Le rejeu redonne la même décision (même id, mêmes options) ; « nouvelle carte » remet le journal de commandes à zéro. |
| Dés scriptés, noms de tests en français, pas de mock (`testing.md`) | Test de reprise sur un fichier temporaire, sans Quarkus. |
| Ne jamais tester sur 8080/5173 ni toucher la partie de l'utilisateur (`testing.md`) | Le serveur de test 8081 écrit dans un fichier temporaire, jamais dans le fichier par défaut. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Sauvegarder les commandes de la session et leurs dés, pas un instantané de l'état ; relancer = les rejouer. | Seule voie qui restitue la décision en attente (son handler est une lambda) sans sérialiser `GameState`, la `Scene` et le journal ; le moteur est déterministe à dés égaux (maps ordonnées dans `GameState`, `Combat`, `Scene` ; ids par compteur, vérifié). Coût : une sauvegarde suppose le même code de règles ; un rejeu qui échoue met le fichier de côté et démarre sans carte. |
| La persistance est un port, `GameStore` (`load`, `save`, `setAside`), que `SceneSession` seule utilise ; l'adaptateur `persistence.JsonFileGameStore` porte Jackson, le chemin et le disque ; `SavedGame` n'a aucune annotation (mixins dans l'adaptateur). Identifiants en anglais, comme le veut `project-brief.md`. | Demande de l'utilisateur (séparation des responsabilités, esprit hexagonal) : changer de stockage = un autre adaptateur, sans toucher la session ; la session se teste avec un faux en mémoire, sans disque. `setAside` est au port car c'est la session qui sait qu'un rejeu échoue. |
| Une seule partie, réécrite après chaque commande réussie ; une commande refusée n'est pas enregistrée. | La story parle d'une partie ; pas de bouton « sauvegarder » à oublier. |
| Fichier par défaut `~/.solo-vtt/partie.json`, surchargeable par `solovtt.save.path` ; aucun en profil de test. | Hors du dépôt et du navigateur ; les tests et le serveur 8081 n'écrasent jamais la partie de l'utilisateur. |
| Une écriture impossible rend une erreur (500) au client ; l'état en mémoire garde l'action. | Le joueur sait que sa partie n'est plus sauvée, sans perdre ce qu'il vient de jouer. |
