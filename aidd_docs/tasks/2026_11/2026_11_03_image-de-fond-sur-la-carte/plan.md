---
objective: "Le joueur crée une carte à partir d'une image de battlemap dont la grille tombe sur celle de la carte, affiche ou masque la grille, et retrouve image et choix de grille après un redémarrage du serveur."
status: in-progress
---

# Plan: mettre une image de fond sur la carte

Source: aidd_docs/backlog/stories/mettre-une-image-de-fond-sur-la-carte.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Garder et servir les images de carte | [`phase-1.md`](./phase-1.md) | none |
| 2   | Carte avec image et grille affichable, gardées avec la partie | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Nouvelle carte depuis une image, fond et bascule de grille dans le navigateur | [`phase-3.md`](./phase-3.md) | 2 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://quarkus.io/guides/http-reference#quarkus-vertx-http_quarkus-http-limits-max-body-size | `quarkus.http.limits.max-body-size`, défaut `10240K` (vérifié dans `ServerLimitsConfig` de `quarkus-vertx-http` 3.40.1.1, `@WithDefault("10240K")`) : une battlemap haute définition dépasse vite 10 Mo, la limite est relevée. |
| https://developer.mozilla.org/en-US/docs/Web/API/HTMLImageElement/naturalWidth | Le navigateur donne la taille en pixels d'une PNG, JPEG ou WebP chargée ; le serveur n'a pas à décoder l'image (`ImageIO` ne lit pas le WebP sans plugin). |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Validé : limite de corps à 10 Mo par défaut dans Quarkus 3.40.1.1 ; relevée à 50 Mo. Non vérifié : qu'une image plus lourde reçoive un 413 de Vert.x sans corps JSON (le client traduit le 413 quel que soit le corps) ; rendu d'une très grande image (≥ 8000 px) dans Konva sur tablette. | 1, 3 |
| Regression | Moyenne. `SavedCommand.Create` gagne `imageId` : une sauvegarde existante, sans ce champ, doit se rejouer (non vérifié : Jackson devrait mettre `null` au champ absent d'un record, `ObjectMapper` sans option, `JsonFileGameStore.java:29`) ; verrouillé par un test dans `JsonFileGameStoreTest` et `ResumeGameTest`. `NewSceneRequest` et `SceneDto` gagnent des champs facultatifs : `SceneApiTest`, `ResumeGameTest` couvrent la création et la reprise. `SceneCanvas.drawGround` (`SceneCanvas.ts:240`) change : pose, aimant, Maj et clic sur le sol se revérifient dans le navigateur. Pas de phase filet : les tests serveur existants verrouillent déjà création, rejeu et format. | 2, 3 |
| Architecture | Départ : une carte = `Scene` (dimensions, unité, jetons), créée par la commande gardée `Create`. Cible : un port `ImageStore` (adaptateur fichier dans `persistence/`, mémoire en test) garde les images par empreinte SHA-256 ; la session ajoute à la carte l'image de fond (référence) et l'affichage de la grille, gardés par `Create` et une nouvelle commande `ShowGrid`. `solovtt.scene.Scene` ne change pas. Le client calcule les dimensions depuis la taille en pixels et la grille, comme il envoie déjà largeur et hauteur. | 1, 2, 3 |
| Documentation | `architecture.md` (images, grille affichable, limite de corps, piège de l'image disparue), `codebase-map.md` (`ImageResource`, `ImageStore`, `FileImageStore`, `/api/images`, `/api/scene/grid`), `project-brief.md` (fonctionnalité), `testing.md` (option `solovtt.images.path` du serveur de test), `DESIGN.md` (champ image du tiroir, bouton grille, grille sur image). Story : `status: done`. Plan : `status: implemented`. | 1, 2, 3 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Les moteurs n'ont aucune dépendance runtime (`architecture.md`, `package.md`) | Image et grille restent dans `vtt-server` et `vtt-web` ; `Scene` n'en sait rien. |
| La partie se garde par ses commandes, derrière `GameStore` ; une commande refusée n'y entre pas (`architecture.md`) | Image et grille se rejouent comme le reste : `Create` porte l'image, `ShowGrid` le choix de grille. |
| Une sauvegarde suppose le code qui l'a écrite (`architecture.md`, pièges) | Les champs ajoutés sont facultatifs : les parties déjà gardées se rejouent. |
| Le client mesure, le serveur tranche (`architecture.md`) | Le client calcule les dimensions ; le serveur valide qu'elles sont positives et que l'image existe. |
| Ne jamais tester sur 8080/5173 ni écrire la partie du joueur (`testing.md`) | Serveur de test avec `solovtt.save.path` et `solovtt.images.path` dans le scratchpad. |
| Tests serveur `@QuarkusTest` + RestAssured, noms en français (`testing.md`) | Nouveaux tests d'API d'image et de grille. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Les images vivent hors de la sauvegarde, dans un `ImageStore` adressé par empreinte SHA-256 (`~/.solo-vtt/images/<empreinte>.<ext>`) ; la partie ne garde que l'empreinte | Le JSON de la partie reste petit et réécrit à chaque action ; la même image n'est stockée qu'une fois, et les cartes enregistrées (story suivante) la référenceront. Contrepartie : une image n'est jamais supprimée. |
| Envoi en deux temps : `POST /api/images` (octets bruts, `Content-Type` image) rend l'empreinte, puis `POST /api/scene` la référence | Pas de multipart ni de base64 ; la création de carte reste en JSON. Contrepartie : une image envoyée puis abandonnée reste stockée. |
| Image et affichage de la grille portés par `SceneSession`, pas par `solovtt.scene.Scene` | Aucune règle n'en dépend : la scène générique reste des positions et des distances. |
| Une image absente au rejeu ne casse pas la partie : seule la création en direct refuse une image inconnue | Supprimer un fichier image ne doit pas mettre la partie de côté ; la carte revient sans fond. |
