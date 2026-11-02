---
objective: "Quand une attaque part, ses dés (d20 gardé distingué, dés bonus, dés de dégâts) roulent sur la piste puis s'arrêtent sur les valeurs du serveur, la cible porte brièvement « −11 » ou « Raté » sur la carte, et chaque jeton montre ses PV en permanence."
status: in-progress
---

# Plan: voir les dés rouler et le résultat sur la carte

Source: aidd_docs/backlog/stories/voir-les-des-rouler-et-le-resultat-sur-la-carte.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Serveur : les dés tirés en faits structurés dans le journal | [`phase-1.md`](./phase-1.md) | none |
| 2   | Client : les dés roulent sur la piste puis le verdict se pose | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Carte : jauge de PV sur chaque jeton et issue sur la cible | [`phase-3.md`](./phase-3.md) | 2 |
| 4   | Mettre à jour le design, la mémoire projet et la story | [`phase-4.md`](./phase-4.md) | 3 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | Affichage seulement : aucune règle D&D ajoutée ni changée. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Regression | Faible. Serveur : `DiceRolled` (`GameEvent.java:67`) gagne `sides` à la place de `notation` ; seuls appelants `CoreAttackRules.java:96` et `:146` (grep), `describe()` garde le même texte, donc les tests qui lisent « d20 : 15 » (`AttackApiTest`) restent valides. `JournalEntry` gagne un champ `dice` ; un seul constructeur (`SceneSession.java:144`). Sauvegarde : elle rejoue les commandes, le journal se reconstruit, aucun format de fichier ne change. Client : la piste (`DiceTray.vue`) garde sa hauteur fixe ; au rechargement (`trayFromJournal()` sans cible, `App.vue:232`, `:265`) rien ne doit rouler. Client sans tests : parcours navigateur sur 8081/5174. `mvn -q test` non lancé pendant la planification : filet au début de la phase 1. | 1, 2, 3 |
| Architecture | Départ : le journal porte les faits de l'issue (`roll`) et des dégâts (`damage`) ; les dés ne sont que du texte (`D20Rolled.describe`, `DiceRolled.describe`). Cible : chaque entrée de dés porte `dice` (faces, valeurs, valeur gardée pour le d20) ; le client range les dés avant l'issue en jet d'attaque, après en dégâts, sans lire le texte. | 1, 2 |
| Documentation | `DESIGN.md` : piste (dés, nouveau mouvement, « seul mouvement » à réécrire), carte (jauge des jetons, bulle d'issue). `architecture.md` : fait `dice` des entrées. `project-brief.md` : fonctionnalité. Story : critères amendés (dés sur la piste, pas de surimpression centrale), `status: done`. Plan : `status: implemented`. | 4 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| L'état ne change que via `Engine.emit` ; les jets de dés sont des événements (`architecture.md`) | Les valeurs affichées viennent de `D20Rolled` et `DiceRolled`, jamais d'un tirage client. |
| La piste à dés se compose des faits des entrées, sans lire le texte (`architecture.md`) | Les dés arrivent en fait `dice`, comme `roll` et `damage`. |
| Dés scriptés, noms de tests en français, RestAssured lit les nombres en `float` (`testing.md`) | Tests API des faits `dice`. |
| Ne jamais tester sur 8080/5173 (`testing.md`) | Parcours navigateur sur 8081/5174, sauvegarde dans le scratchpad. |
| The Still Table Rule et un seul mouvement (`DESIGN.md`) | Les dés tiennent dans la hauteur fixe de la piste ; le roulement devient le premier temps du mouvement, réécrit en phase 4. |
| `prefers-reduced-motion` respecté (`DESIGN.md`, story) | Sans roulement : valeurs et verdict posés d'emblée, bulle sans glissement. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Les dés roulent sur la piste à dés, pas en surimpression au milieu de la carte ; sur la carte, seule la bulle d'issue sur la cible. La story est amendée. | Choix de l'utilisateur : cohérent avec `DESIGN.md` (« le moteur lance sur la piste »), la carte n'est jamais masquée. |
| `DiceRolled` porte `sides` au lieu de `notation` (dérivée). | Le serveur n'a pas à relire une notation texte pour donner les faces. |
