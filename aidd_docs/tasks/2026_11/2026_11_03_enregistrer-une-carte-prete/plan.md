---
objective: "Le joueur enregistre la carte en cours sous un nom (image, échelle, grille, créatures et positions), la retrouve dans une liste après un redémarrage du serveur, et la recharge comme une nouvelle carte, avec des créatures neuves aux positions enregistrées."
status: implemented
---

# Plan: enregistrer puis charger une carte prête

Source: aidd_docs/backlog/stories/enregistrer-puis-charger-une-carte-prete.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Garder les cartes enregistrées | [`phase-1.md`](./phase-1.md) | none |
| 2   | Enregistrer et charger une carte par l'API | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Tiroir « Cartes enregistrées » dans le navigateur | [`phase-3.md`](./phase-3.md) | 2 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | none |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Nouveau fichier `~/.solo-vtt/cartes.json` (propriété `solovtt.maps.path`, vide en profil de test : mémoire), écrit comme `JsonFileGameStore` (fichier voisin puis déplacement atomique, `JsonFileGameStore.java:57`). Observé : Jackson relit déjà des records sans configuration (`JsonFileGameStoreTest`). | 1 |
| Regression | Faible. Aucune commande gardée nouvelle : charger une carte rejoue `Create`, `ShowGrid` et `Place` existants (`SceneSession.java:153`, `:325`, `:333`). Seul `Create` gagne un champ facultatif `mapName` (`SavedGame.java:24`) : une partie déjà gardée, sans ce champ, se relit avec `null`, comme `imageId` avant lui (observé : `JsonFileGameStoreTest.java:91`). `SceneSession` gagne deux méthodes sans toucher les autres ; `App.vue` gagne un tiroir : `NewMapForm` passe de `v-else` à `v-else-if="drawer === 'map'"` (`App.vue:982`). Observé : `attempt` (`App.vue:260`) remplace tout 404/409 par le message de `resync()` ; les appels des cartes enregistrées n'y passent donc pas, pour garder « carte inconnue » ou « entrée absente du catalogue ». Couvert par `ResumeGameTest`, `SceneApiTest` côté serveur, parcours navigateur côté client ; pas de phase filet. | 2, 3 |
| Architecture | Départ : une carte naît seulement de « Nouvelle carte » puis de poses une à une ; seule la partie en cours est gardée (`GameStore`). Cible : un port `MapStore` (adaptateur `persistence.JsonFileMapStore`, mémoire en test) garde des `SavedMap` par nom ; `SceneSession.snapshot` en tire une de la carte en cours, `SceneSession.load` la rejoue en commandes ordinaires. | 1, 2 |
| Documentation | `architecture.md` (cartes enregistrées, port `MapStore`, chargement = commandes ordinaires, piège de l'image absente), `codebase-map.md` (`MapResource`, `MapStore`, `JsonFileMapStore`, `/api/maps`, `/api/maps/load`, `SavedMapsPanel`), `project-brief.md` (fonctionnalité), `testing.md` (option `solovtt.maps.path` du serveur de test), `DESIGN.md` (tiroir Cartes enregistrées, icône du rail). Story : `status: done`. Plan : `status: implemented`. | 1, 2, 3 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Les moteurs n'ont aucune dépendance runtime (`architecture.md`) | Tout reste dans `vtt-server` et `vtt-web` ; `Scene` ne change pas. |
| La partie se garde par ses commandes ; « nouvelle carte » la remet à zéro (`architecture.md`) | Charger une carte = `create` puis `showGrid` puis `place` : la partie repart de zéro et se rejoue sans format nouveau. |
| Port et adaptateur dans `persistence/`, profil de test sans fichier (`architecture.md`) | `MapStore` suit `GameStore` et `ImageStore`. |
| Ne jamais tester sur 8080/5173 ni écrire les fichiers du joueur (`testing.md`) | Le serveur de test reçoit aussi `-Dsolovtt.maps.path=<scratchpad>/cartes.json`. |
| Tests `@QuarkusTest` + RestAssured, noms en français (`testing.md`) | Nouveaux tests d'API et d'adaptateur. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Une carte enregistrée est un modèle (dimensions, unité, image, grille, entrées du catalogue et positions), pas une partie : PV, effets, combat et journal n'y entrent pas | Les créatures chargées doivent être neuves, et recharger doit redonner la même scène de départ. |
| La partie contient une copie de la carte de départ (ses commandes), jamais une référence ; le nom de la carte chargée n'y est gardé qu'à titre d'information (`Create.mapName`), sans effet au rejeu | Modifier ou supprimer une carte enregistrée ne touche aucune partie ; le nom s'affiche dans la barre. |
| Charger rejoue des commandes existantes (`Create`, `ShowGrid`, `Place`) au lieu d'ajouter une commande `LoadMap` | La partie ne dépend pas du fichier des cartes : modifier ou perdre `cartes.json` ne casse jamais la reprise. Contrepartie : une sauvegarde de partie fait N + 2 écritures au chargement. |
| Toutes les cartes dans un seul fichier `cartes.json`, indexées par nom ; un nom déjà pris est remplacé (le client prévient) | Peu de cartes, de petite taille : un fichier, une écriture atomique, aucun nom de fichier à assainir sous Windows. |
