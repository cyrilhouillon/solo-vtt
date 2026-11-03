---
objective: "Sur une carte avec image, le joueur décale la grille et ajuste la taille d'une case au pixel près, en voyant la grille bouger ; à la validation, dimensions, distances et calage (Maj) suivent la nouvelle grille, les créatures restent au même endroit de l'image, et le calage est gardé avec la partie et les cartes enregistrées."
status: implemented
---

# Plan: réaligner la grille sur l'image

Source: aidd_docs/backlog/stories/realigner-la-grille-sur-l-image.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Caler la grille côté serveur | [`phase-1.md`](./phase-1.md) | none |
| 2   | Garder le calage dans les cartes enregistrées | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Caler la grille dans le navigateur | [`phase-3.md`](./phase-3.md) | 1, 2 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | none |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Aucun. Pas de nouveau fichier ni de nouvelle dépendance : le calage entre dans `partie.json` (commande `AlignGrid`) et `cartes.json` (deux champs de `SavedMap`). Non vérifié : un ancien `cartes.json` sans ces champs doit se relire avec un décalage 0 (Jackson par défaut, `JsonFileMapStore.java:26`, sans `FAIL_ON_NULL_FOR_PRIMITIVES`) ; un test le fixe en phase 2. | 1, 2 |
| Regression | Moyen côté client, faible côté serveur. Serveur : une commande gardée de plus (`SavedGame.java:19`) et une méthode de `SceneSession` ; `create`, `place`, `move` inchangés ; couverts par `ResumeGameTest`, `SceneApiTest`, `MapApiTest`, `JsonFileMapStoreTest`. Client : `snapToCell` (`grid.ts:12`), le curseur clavier et l'annonce de case (`MapView.vue:102`, `:148`) et le dessin de la grille (`SceneCanvas.ts:281-294`) prennent un décalage ; à décalage 0, comportement identique. `history` (`App.vue:190`) garde des positions d'avant le calage pour Ctrl+Z : vidé à la validation. Pas de tests automatisés côté web (`testing.md`) : parcours navigateur. Pas de phase filet : les tests serveur existants couvrent déjà création, poses et reprise. | 1, 2, 3 |
| Architecture | Départ : la carte tire ses dimensions de l'image une fois pour toutes (`NewMapForm.vue:66`, `backdrop.ts`), la grille part du coin de l'image, `Scene` a des dimensions fixes. Cible : la carte reste l'aire de l'image (origine au coin de l'image) ; le calage de `SceneSession` porte un décalage de grille en fraction de case (`gridOffsetX`, `gridOffsetY` dans [0, 1)), affichage et calage client seulement ; changer la taille de case remplace la `Scene` par `Scene.rescaled(facteur)` (générique, `vtt-engine`), qui multiplie dimensions et positions. | 1, 3 |
| Documentation | `architecture.md` (calage : commande `AlignGrid`, refus en combat, `Scene.rescaled`, décalage en fraction de case ; piège : jetons rapprochés après agrandissement de case), `codebase-map.md` (`scene/alignment`, `map/alignment.ts`, `GridAlignPanel`), `project-brief.md` (fonctionnalité), `DESIGN.md` (tiroir Caler la grille, icône du rail). Story : `status: done`. Plan : `status: implemented`. | 1, 3 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Les moteurs n'ont aucune dépendance runtime ; `Scene` est générique, sans grille (`architecture.md`) | `Scene.rescaled` ne connaît ni case ni image ; le décalage de grille vit dans `SceneSession`, comme l'image et l'affichage de la grille. |
| La partie se garde par ses commandes ; une commande refusée n'y entre pas (`architecture.md`) | Le calage est une commande `AlignGrid` rejouée ; refusée (combat, décision, sans image), elle n'est pas gardée. |
| Un refus d'état devient `ActionRefusedException` (409) ; une décision en attente `DecisionPendingException` (409) (`architecture.md`) | Calage en combat ou pendant une décision. |
| Le client mesure, le serveur tranche (`architecture.md`) | Le client calcule l'aperçu ; le serveur applique le facteur et refuse ce qui sort des règles. |
| Ne jamais tester sur 8080/5173 ni écrire les fichiers du joueur (`testing.md`) | Parcours sur 8081/5174 avec les chemins du scratchpad. |
| Tests `@QuarkusTest` + RestAssured, noms en français ; toute règle arrive avec son test (`testing.md`) | Tests d'API, de reprise et de `Scene.rescaled`. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| La carte reste l'aire de l'image, origine au coin de l'image ; seule la grille se décale | Les positions ne bougent pas avec le décalage, et l'image reste étirée sur toute la carte : rien à recalculer pour un simple décalage. Contrepartie : des cases partielles sur les quatre bords. |
| Le décalage est gardé en fraction de case, normalisé dans [0, 1) | Le serveur n'a pas à connaître la taille d'une case (1,5 m / 5 ft est une convention client) ; la fraction ne change pas quand la taille de case change. |
| Changer la taille de case met la carte à l'échelle (dimensions et positions multipliées par le même facteur, `largeur nouvelle / largeur ancienne`) au lieu de garder les dimensions | Une case vaut toujours 1,5 m ou 5 ft : les créatures restent au même endroit de l'image et les distances suivent la nouvelle grille. Contrepartie : deux créatures au contact peuvent se chevaucher après un agrandissement de case. |
| Calage refusé en combat, pendant une décision et sans image | Pas de vitesse dépensée à remettre à l'échelle ; la story ne vise que les cartes avec image. |
