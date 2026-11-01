---
objective: "Depuis le navigateur, le joueur crée une carte vide en mètres ou en pieds, y place des personnages et des monstres du catalogue SRD, lit la distance entre deux créatures et consulte la feuille de chacune."
status: in-progress
---

# Plan: préparer une scène de combat

Source: aidd_docs/backlog/stories/preparer-une-scene-de-combat.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Scène générique et catalogue SRD | [`phase-1.md`](./phase-1.md) | none |
| 2   | Serveur Quarkus de la scène | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Client Vue : carte, catalogue, feuille | [`phase-3.md`](./phase-3.md) | 2 |
| 4   | Mettre à jour la mémoire projet | [`phase-4.md`](./phase-4.md) | 3 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://quarkus.io/guides/rest-json | REST + JSON = extension `io.quarkus:quarkus-rest-jackson` ; guide en Quarkus 3.40 ; JDK 17+ (Java 21 du projet compatible) |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | nouveau : Quarkus (BOM `io.quarkus.platform:quarkus-bom`, version exacte non vérifiée, à figer en phase 2) dans `vtt-server` seul ; Node 22.19 / npm 10.9 présents (observé) pour `vtt-web` ; Vue 3, Vite, TypeScript, Konva non installés (non vérifié) | 2, 3 |
| Regression | faible : seul `Creature` change (ajout de `speed`, défaut 9 m, compatible avec le builder actuel). Filet : les 21 tests existants (14 dans `AttackRulesTest`, 7 dans `EngineTest`, `grep -c @Test` observé), inchangés ; pas de phase de filet dédiée | 1 |
| Architecture | départ : deux librairies sans UI ni serveur, l'état D&D n'a ni carte ni positions (`GameState` = map de créatures). Cible : `vtt-engine` gagne la scène générique (`solovtt.scene` : carte, unité, jetons, distance) ; `dnd-engine` gagne le catalogue SRD ; nouveau module `vtt-server` (Quarkus, REST JSON) qui compose `Scene` + `GameState` en une session en mémoire ; nouveau client `vtt-web` (Vue 3 + TS + Konva) hors réacteur Maven | 1, 2, 3 |
| Documentation | `architecture.md`, `codebase-map.md`, `package.md`, `coding-assertions.md`, `testing.md`, `project-brief.md` ne décrivent ni serveur, ni client, ni scène ; `PRODUCT.md` liste le transport comme indécis | 4 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Aucune dépendance runtime dans `vtt-engine` et `dnd-engine` (`package.md`) | Quarkus et Jackson restent confinés à `vtt-server` |
| Le cœur ignore le système (`PRODUCT.md`, principe 3) | la scène et la distance vivent dans `vtt-engine`, sans type D&D |
| La carte ne connaît que des distances réelles (PRD) | positions et distances exprimées dans l'unité de la carte |
| Records, sealed interfaces, `switch` à motifs (`architecture.md`) | modèle de la scène |
| Tests en JUnit, noms en français décrivant la règle, pas de mock (`testing.md`) | tests de la scène, du catalogue et du serveur |
| Code et messages en français, identifiants en anglais (`project-brief.md`) | tout fichier créé, interface comprise |
| Contenu D&D tiré du SRD 5.2 (CC-BY-4.0) avec attribution | valeurs du catalogue |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Serveur Quarkus (`quarkus-rest-jackson`), transport HTTP REST JSON | choix de l'utilisateur ; REST suffit tant que le serveur ne pousse rien (WebSocket ajoutable plus tard) |
| Carte rendue avec Konva dans un module TS isolé de Vue | choix de l'utilisateur ; `PRODUCT.md` impose un rendu canvas indépendant du framework |
| La scène (carte, jetons, distance) est générique, dans `vtt-engine` | les règles D&D devront lire les distances (story « attaquer », portée) sans dépendre du serveur ; la carte doit survivre au changement de système |
| Distance euclidienne de centre à centre, dans l'unité de la carte, sans règle de grille | le PRD veut des distances réelles et un déplacement libre ; l'usage D&D (portée) relève du système |
| La préparation construit l'état initial hors journal (`Scene.place`, `GameState.add`) ; une seule session en mémoire | le journal commence avec le combat, comme dans la démo ; persistance (story 7) et déplacements journalisés (story 5) hors périmètre |
