---
objective: "Chaque combat d'une campagne est une entrée du Carnet, nommée, déplaçable, liable et supprimable, qu'on reprend tant qu'il est en cours et qu'on rejoue pas à pas en lecture seule une fois terminé."
status: implemented
---

# Plan: Combats enregistrés et rejouables

Source: brief « Combats enregistrés et rejouables » approuvé dans la conversation du 2026-11-09 (Clair, Supposé, Ouvert). Hors périmètre : rejouer un combat après un changement de règles (story à part).

## Phases

| #   | Phase | File | Depends on |
| --- | ----- | ---- | ---------- |
| 1   | Serveur : combat enregistré, entrée du Carnet, reprise | [`phase-1.md`](./phase-1.md) | none |
| 2   | Serveur : rejeu pas à pas d'un combat terminé | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Client : l'entrée combat dans le Carnet | [`phase-3.md`](./phase-3.md) | 1 |
| 4   | Client : écran de combat, reprise et rejeu | [`phase-4.md`](./phase-4.md) | 2, 3 |

## External sources

| URL | What it settled |
| --- | --------------- |
| none | Tout repose sur le code du dépôt (Jackson lit un champ absent d'un record en `null`, déjà employé pour `Create.imageId` / `mapName`) |

## Impact

| Category | Verdict and evidence | Handled in |
| -------- | -------------------- | ---------- |
| Regression | Medium : `SceneSession` (enregistrement, rejeu) et `SavedGame.StartCombat` changent ; couverts par `ResumeGameTest`, `CombatApiTest`, `CombatDeBoutEnBoutTest`, `JsonFileGameStoreTest`, `NotebookStoreTest`, `JsonFileNotebookStoreTest`. Une `partie.json` d'avant (`startCombat` sans `combatId`) doit se relire : test ajouté. Pas de phase filet : les tests existants verrouillent déjà la reprise | 1, 2 |
| Regression (client) | Medium : `vtt-web` sans test automatisé ; `GameScreen` (1598 lignes) gagne un mode lecture seule, `NotebookScreen`, `PageBranch`, `tree.ts` un type d'entrée. Vérification au navigateur sur 5174/8081 | 3, 4 |
| Architecture | Aujourd'hui une campagne a une seule partie (`partie.json`), le combat y vit. Après : chaque combat a son fichier (`combats/<id>.json`, une `SavedGame` de la création de la carte à la fin du combat), référencé par son entrée du Carnet (`Page.combatId`) ; la partie courante peut être remplacée par un combat en cours (reprise). Le Carnet gagne un type d'entrée (une `Page` qui porte `combatId`) et une section du cœur, `combats` | 1, 3 |
| Documentation | `architecture.md` (combats, rejeu, section du cœur, reprise), `codebase-map.md` (nouvelles classes, routes, composants), `project-brief.md` (fonctionnalité), `DESIGN.md` si la barre de rejeu ajoute un motif | 4 |

## Rules

| Rule | Why it applies |
| ---- | -------------- |
| L'état ne change que par `Engine.emit` ; la partie se garde par ses commandes (`architecture.md`) | Le combat se garde et se rejoue par les mêmes commandes et dés |
| Une sauvegarde suppose le code de règles qui l'a écrite (`architecture.md`, Pièges) | Un combat dont le rejeu échoue se montre illisible (409), sans casser le Carnet |
| `IllegalArgumentException` → 400, ressource inconnue → `NotFoundException` 404, refus d'état → `ActionRefusedException` 409 | Routes de combat et refus d'écrire le contenu d'une entrée combat |
| Dés toujours scriptés en test (`testing.md`) | Tests de rejeu et de reprise |
| Tests navigateur sur 8081/5174 uniquement (`testing.md`) | Phases 3 et 4 |
| Code et messages en français, identifiants en anglais (`project-brief.md`) | Nouveaux textes et noms |
| Le cœur ne nomme aucun module (`architecture.md`) | La section `combats` est du cœur : `visiblePages` ne doit pas la cacher comme un module inactif |

## Decisions

| Decision | Why |
| -------- | --- |
| Le combat est un agrégat (`combats/<combatId>.json`) qui ignore le Carnet ; son entrée est une `Page` qui le référence par `combatId` (non `null` : entrée combat), avec son propre id, sans contenu | Arbre, déplacement, renommage, corbeille, onglets, recherche par titre, `[[` et pages citantes marchent sans rien dupliquer ; l'agrégat reste indépendant |
| Le nom du combat vit dans le titre de son entrée, pas dans l'agrégat | Une seule source : renommer l'entrée suffit |
| Le combat se garde comme une `SavedGame` complète (de `Create` à `EndCombat`), écrite à chaque commande pendant le combat | Le rejeu et la reprise réutilisent le rejeu existant de `SceneSession` ; aucun nouvel état sérialisé |
| `SavedCommand.StartCombat` porte `combatId` | Le rejeu de `partie.json` retrouve le combat en cours sans recréer d'entrée |
| Le rejeu se calcule au serveur en une passe et se rend en entier (`GET …/combats/{id}/replay`, une liste de pas) | Avant, arrière, début, fin et lecture se font côté client sans latence ; DTO de scène, combat et feuilles réutilisés |
| Reprendre un combat en cours remplace la partie courante sans confirmation | Brief : « un clic le reprend ». Tradeoff : une carte en préparation non enregistrée est perdue, comme avec « Nouvelle carte » |
| Vider la corbeille n'efface pas le fichier d'un combat | Comme les images : rien de détruit ; un fichier orphelin est sans effet |
